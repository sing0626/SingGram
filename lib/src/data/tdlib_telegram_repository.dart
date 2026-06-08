import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:path_provider/path_provider.dart';
import 'package:tdlib/td_api.dart' as td_api;
import 'package:tdlib/tdlib.dart';

import '../models/telegram_models.dart';
import 'telegram_repository.dart';

class TdlibTelegramRepository extends TelegramRepository {
  TdlibTelegramRepository({TdlibClient? tdlib, TdlibDirectoryProvider? dirs})
    : _tdlib = tdlib ?? const NativeTdlibClient(),
      _dirs = dirs ?? const AppTdlibDirectoryProvider();

  final TdlibClient _tdlib;
  final TdlibDirectoryProvider _dirs;
  final Map<String, Completer<td_api.TdObject>> _pending = {};
  final Map<String, List<ChatMessage>> _messages = {};
  final List<ChatDialog> _dialogs = [];

  AuthState _authState = const AuthState.signedOut();
  LoginCredentials? _credentials;
  Timer? _receiveTimer;
  int _clientId = 0;
  bool _initialized = false;
  bool _disposed = false;
  bool _readyLoaded = false;

  @override
  AuthState get authState => _authState;

  @override
  List<ChatDialog> get dialogs => List.unmodifiable(_dialogs);

  @override
  List<ChatMessage> messagesFor(String chatId) {
    return List.unmodifiable(_messages[chatId] ?? const []);
  }

  @override
  Future<void> startLogin(LoginCredentials credentials) async {
    if (credentials.apiHash.trim().isEmpty ||
        credentials.phoneNumber.trim().isEmpty) {
      _setAuthState(const AuthState.error('API hash and phone are required.'));
      return;
    }

    _credentials = credentials;
    _readyLoaded = false;
    _setAuthState(const AuthState.connecting());

    try {
      await _ensureClient();
      _send(const td_api.GetAuthorizationState(), 'auth-state');
    } on Object catch (error) {
      _setAuthState(AuthState.error('TDLib failed to start: $error'));
    }
  }

  @override
  Future<void> submitCode(String code) async {
    final trimmed = code.trim();
    if (trimmed.isEmpty) {
      _setAuthState(const AuthState.error('Login code is empty.'));
      return;
    }

    try {
      _setAuthState(const AuthState.connecting());
      _send(td_api.CheckAuthenticationCode(code: trimmed), 'auth-code');
    } on Object catch (error) {
      _setAuthState(AuthState.error('Could not submit code: $error'));
    }
  }

  @override
  Future<void> submitPassword(String password) async {
    if (password.isEmpty) {
      _setAuthState(const AuthState.error('Password is empty.'));
      return;
    }

    try {
      _setAuthState(const AuthState.connecting());
      _send(td_api.CheckAuthenticationPassword(password: password), 'auth-2fa');
    } on Object catch (error) {
      _setAuthState(AuthState.error('Could not submit password: $error'));
    }
  }

  @override
  Future<void> loadMessages(String chatId) async {
    final id = int.tryParse(chatId);
    if (id == null || _clientId == 0) return;

    try {
      final response = await _request(
        td_api.GetChatHistory(
          chatId: id,
          fromMessageId: 0,
          offset: 0,
          limit: 50,
          onlyLocal: false,
        ),
        timeout: const Duration(seconds: 20),
      );
      if (response is td_api.Messages) {
        _messages[chatId] = response.messages
            .map(_mapMessage)
            .whereType<ChatMessage>()
            .toList()
            .reversed
            .toList();
        notifyListeners();
      } else if (response is td_api.TdError) {
        _setAuthState(AuthState.error(response.message));
      }
    } on Object catch (error) {
      _setAuthState(AuthState.error('Could not load messages: $error'));
    }
  }

  @override
  Future<void> sendText({required String chatId, required String text}) async {
    final id = int.tryParse(chatId);
    final trimmed = text.trim();
    if (id == null || trimmed.isEmpty || _clientId == 0) return;

    final request = td_api.SendMessage(
      chatId: id,
      messageThreadId: 0,
      inputMessageContent: td_api.InputMessageText(
        text: td_api.FormattedText(text: trimmed, entities: const []),
        disableWebPagePreview: false,
        clearDraft: true,
      ),
    );

    try {
      final response = await _request(
        request,
        timeout: const Duration(seconds: 20),
      );
      if (response is td_api.Message) {
        _upsertMessage(_mapMessage(response));
      } else if (response is td_api.TdError) {
        _setAuthState(AuthState.error(response.message));
      }
    } on Object catch (error) {
      _setAuthState(AuthState.error('Could not send message: $error'));
    }
  }

  @override
  Future<void> logout() async {
    if (_clientId != 0) {
      _send(const td_api.LogOut(), 'logout');
    }
    _credentials = null;
    _readyLoaded = false;
    _dialogs.clear();
    _messages.clear();
    _setAuthState(const AuthState.signedOut());
  }

  @override
  void dispose() {
    _disposed = true;
    _receiveTimer?.cancel();
    for (final completer in _pending.values) {
      if (!completer.isCompleted) {
        completer.completeError(StateError('TDLib repository disposed.'));
      }
    }
    _pending.clear();
    if (_clientId != 0) {
      _send(const td_api.Close(), 'close');
      _clientId = 0;
    }
    super.dispose();
  }

  Future<void> _ensureClient() async {
    if (!_initialized) {
      final libPath = Platform.isAndroid ? 'libtdjson.so' : null;
      await _tdlib.initialize(libPath);
      _initialized = true;
    }

    if (_clientId == 0) {
      _clientId = _tdlib.create();
      if (_clientId == 0) {
        throw StateError('TDLib returned an empty client id.');
      }
      _startReceiveLoop();
    }
  }

  void _startReceiveLoop() {
    _receiveTimer?.cancel();
    _receiveTimer = Timer.periodic(const Duration(milliseconds: 80), (_) {
      _drainUpdates();
    });
  }

  void _drainUpdates() {
    if (_disposed || _clientId == 0) return;

    for (var i = 0; i < 32; i += 1) {
      final object = _tdlib.receive(0);
      if (object == null) return;
      _handleObject(object);
    }
  }

  void _send(td_api.TdFunction function, [Object? extra]) {
    if (_clientId == 0) return;
    _tdlib.send(_clientId, function, extra);
  }

  Future<td_api.TdObject> _request(
    td_api.TdFunction function, {
    Duration timeout = const Duration(seconds: 12),
  }) async {
    if (_clientId == 0) {
      throw StateError('TDLib client has not started.');
    }

    final extra =
        'req-${DateTime.now().microsecondsSinceEpoch}-${Random().nextInt(1 << 32)}';
    final completer = Completer<td_api.TdObject>();
    _pending[extra] = completer;
    _send(function, extra);
    return completer.future.timeout(
      timeout,
      onTimeout: () {
        _pending.remove(extra);
        throw TimeoutException(function.getConstructor(), timeout);
      },
    );
  }

  void _handleObject(td_api.TdObject object) {
    final extra = object.extra;
    if (extra != null) {
      final completer = _pending.remove(extra.toString());
      if (completer != null && !completer.isCompleted) {
        completer.complete(object);
      }
    }

    if (object is td_api.TdError) {
      _setAuthState(AuthState.error(object.message));
      return;
    }

    if (object is td_api.AuthorizationState) {
      _handleAuthorizationState(object);
      return;
    }

    if (object is td_api.UpdateAuthorizationState) {
      _handleAuthorizationState(object.authorizationState);
      return;
    }

    if (object is td_api.UpdateNewChat) {
      _upsertDialog(_mapChat(object.chat));
      notifyListeners();
      return;
    }

    if (object is td_api.UpdateChatLastMessage) {
      _refreshDialogLastMessage(object.chatId, object.lastMessage);
      notifyListeners();
      return;
    }

    if (object is td_api.UpdateNewMessage) {
      _upsertMessage(_mapMessage(object.message));
      return;
    }

    if (object is td_api.UpdateMessageSendSucceeded) {
      _replaceMessage(
        object.message.chatId.toString(),
        object.oldMessageId.toString(),
        _mapMessage(object.message),
      );
      return;
    }

    if (object is td_api.UpdateMessageSendFailed) {
      _upsertMessage(_mapMessage(object.message));
      _setAuthState(AuthState.error(object.errorMessage));
    }
  }

  Future<void> _handleAuthorizationState(
    td_api.AuthorizationState state,
  ) async {
    final credentials = _credentials;

    if (state is td_api.AuthorizationStateWaitTdlibParameters) {
      if (credentials == null) {
        _setAuthState(const AuthState.signedOut());
        return;
      }

      try {
        final directories = await _dirs.directories();
        _send(
          td_api.SetTdlibParameters(
            useTestDc: false,
            databaseDirectory: directories.databaseDirectory,
            filesDirectory: directories.filesDirectory,
            databaseEncryptionKey: '',
            useFileDatabase: true,
            useChatInfoDatabase: true,
            useMessageDatabase: true,
            useSecretChats: false,
            apiId: credentials.apiId,
            apiHash: credentials.apiHash,
            systemLanguageCode: 'en',
            deviceModel: 'Android',
            systemVersion: Platform.operatingSystemVersion,
            applicationVersion: 'TG Third 1.0.0',
            enableStorageOptimizer: true,
            ignoreFileNames: false,
          ),
          'set-tdlib-parameters',
        );
      } on Object catch (error) {
        _setAuthState(AuthState.error('TDLib parameters failed: $error'));
      }
      return;
    }

    if (state is td_api.AuthorizationStateWaitPhoneNumber) {
      if (credentials == null) {
        _setAuthState(const AuthState.signedOut());
        return;
      }

      _setAuthState(const AuthState.connecting());
      _send(
        td_api.SetAuthenticationPhoneNumber(
          phoneNumber: credentials.phoneNumber,
        ),
        'set-phone-number',
      );
      return;
    }

    if (state is td_api.AuthorizationStateWaitCode) {
      _setAuthState(AuthState.waitingCode(credentials?.phoneNumber ?? ''));
      return;
    }

    if (state is td_api.AuthorizationStateWaitPassword) {
      _setAuthState(AuthState.waitingPassword(credentials?.phoneNumber ?? ''));
      return;
    }

    if (state is td_api.AuthorizationStateWaitEmailAddress ||
        state is td_api.AuthorizationStateWaitEmailCode) {
      _setAuthState(
        const AuthState.error(
          'Telegram requested email verification. This build supports phone code and 2FA password first.',
        ),
      );
      return;
    }

    if (state is td_api.AuthorizationStateWaitRegistration) {
      _setAuthState(
        const AuthState.error(
          'This phone number is not registered on Telegram yet.',
        ),
      );
      return;
    }

    if (state is td_api.AuthorizationStateWaitOtherDeviceConfirmation) {
      _setAuthState(
        AuthState.error(
          'Confirm login from another Telegram device: ${state.link}',
        ),
      );
      return;
    }

    if (state is td_api.AuthorizationStateReady) {
      if (!_readyLoaded) {
        _readyLoaded = true;
        await _loadReadyState();
      }
      return;
    }

    if (state is td_api.AuthorizationStateClosed) {
      _clientId = 0;
      _setAuthState(const AuthState.signedOut());
    }
  }

  Future<void> _loadReadyState() async {
    try {
      final me = await _request(const td_api.GetMe());
      if (me is td_api.User) {
        _setAuthState(AuthState.ready(_mapUser(me)));
      } else if (me is td_api.TdError) {
        _setAuthState(AuthState.error(me.message));
        return;
      } else {
        _setAuthState(
          const AuthState.ready(
            TelegramUser(id: 'me', displayName: 'Telegram user'),
          ),
        );
      }

      await _loadDialogs();
    } on Object catch (error) {
      _setAuthState(AuthState.error('Could not finish login: $error'));
    }
  }

  Future<void> _loadDialogs() async {
    final response = await _request(
      const td_api.GetChats(limit: 30),
      timeout: const Duration(seconds: 20),
    );
    if (response is td_api.Chats) {
      final loadedDialogs = <ChatDialog>[];
      for (final chatId in response.chatIds) {
        final chat = await _request(
          td_api.GetChat(chatId: chatId),
          timeout: const Duration(seconds: 12),
        );
        if (chat is td_api.Chat) {
          loadedDialogs.add(_mapChat(chat));
        }
      }
      _dialogs
        ..clear()
        ..addAll(loadedDialogs);
      notifyListeners();
    }
  }

  TelegramUser _mapUser(td_api.User user) {
    final name = [
      user.firstName,
      user.lastName,
    ].where((part) => part.trim().isNotEmpty).join(' ').trim();
    final username = user.usernames?.activeUsernames.firstOrNull;

    return TelegramUser(
      id: user.id.toString(),
      displayName: name.isEmpty ? username ?? user.id.toString() : name,
      username: username,
    );
  }

  ChatDialog _mapChat(td_api.Chat chat) {
    return ChatDialog(
      id: chat.id.toString(),
      title: chat.title,
      kind: _mapChatKind(chat.type),
      unreadCount: chat.unreadCount,
      lastMessage: _messagePreview(chat.lastMessage),
      lastMessageAt: _messageDate(chat.lastMessage),
    );
  }

  DialogKind _mapChatKind(td_api.ChatType type) {
    if (type is td_api.ChatTypePrivate) return DialogKind.user;
    if (type is td_api.ChatTypeBasicGroup) return DialogKind.group;
    if (type is td_api.ChatTypeSupergroup) {
      return type.isChannel ? DialogKind.channel : DialogKind.group;
    }
    return DialogKind.unknown;
  }

  ChatMessage? _mapMessage(td_api.Message message) {
    final text = _messagePreview(message);
    if (text == null) return null;

    return ChatMessage(
      id: message.id.toString(),
      chatId: message.chatId.toString(),
      text: text,
      authorName: message.isOutgoing ? null : 'Sender',
      sentAt: DateTime.fromMillisecondsSinceEpoch(message.date * 1000),
      outgoing: message.isOutgoing,
    );
  }

  String? _messagePreview(td_api.Message? message) {
    if (message == null) return null;

    final content = message.content;
    if (content is td_api.MessageText) {
      return content.text.text;
    }

    return switch (content.getConstructor()) {
      'messagePhoto' => 'Photo',
      'messageVideo' => 'Video',
      'messageAnimation' => 'Animation',
      'messageSticker' => 'Sticker',
      'messageVoiceNote' => 'Voice note',
      'messageAudio' => 'Audio',
      'messageDocument' => 'Document',
      'messageCall' => 'Call',
      'messageContact' => 'Contact',
      'messageLocation' => 'Location',
      _ => content.getConstructor().replaceFirst('message', ''),
    };
  }

  DateTime? _messageDate(td_api.Message? message) {
    if (message == null || message.date <= 0) return null;
    return DateTime.fromMillisecondsSinceEpoch(message.date * 1000);
  }

  void _upsertDialog(ChatDialog dialog) {
    final index = _dialogs.indexWhere((item) => item.id == dialog.id);
    if (index == -1) {
      _dialogs.insert(0, dialog);
    } else {
      _dialogs[index] = dialog;
    }
  }

  void _refreshDialogLastMessage(int chatId, td_api.Message? lastMessage) {
    final index = _dialogs.indexWhere((dialog) => dialog.id == '$chatId');
    if (index == -1) return;
    _dialogs[index] = _dialogs[index].copyWith(
      lastMessage: _messagePreview(lastMessage),
      lastMessageAt: _messageDate(lastMessage),
    );
  }

  void _upsertMessage(ChatMessage? message) {
    if (message == null) return;
    final messages = List<ChatMessage>.from(
      _messages[message.chatId] ?? const [],
    );
    final index = messages.indexWhere((item) => item.id == message.id);
    if (index == -1) {
      messages.add(message);
    } else {
      messages[index] = message;
    }
    _messages[message.chatId] = messages;
    notifyListeners();
  }

  void _replaceMessage(
    String chatId,
    String oldMessageId,
    ChatMessage? newMessage,
  ) {
    if (newMessage == null) return;
    final messages = List<ChatMessage>.from(_messages[chatId] ?? const []);
    final index = messages.indexWhere((item) => item.id == oldMessageId);
    if (index == -1) {
      messages.add(newMessage);
    } else {
      messages[index] = newMessage;
    }
    _messages[chatId] = messages;
    notifyListeners();
  }

  void _setAuthState(AuthState state) {
    _authState = state;
    notifyListeners();
  }
}

class TdlibDirectories {
  const TdlibDirectories({
    required this.databaseDirectory,
    required this.filesDirectory,
  });

  final String databaseDirectory;
  final String filesDirectory;
}

abstract class TdlibDirectoryProvider {
  Future<TdlibDirectories> directories();
}

class AppTdlibDirectoryProvider implements TdlibDirectoryProvider {
  const AppTdlibDirectoryProvider();

  @override
  Future<TdlibDirectories> directories() async {
    final supportDirectory = await getApplicationSupportDirectory();
    final databaseDirectory = Directory('${supportDirectory.path}/tdlib/db');
    final filesDirectory = Directory('${supportDirectory.path}/tdlib/files');
    await databaseDirectory.create(recursive: true);
    await filesDirectory.create(recursive: true);

    return TdlibDirectories(
      databaseDirectory: databaseDirectory.path,
      filesDirectory: filesDirectory.path,
    );
  }
}

abstract class TdlibClient {
  Future<void> initialize(String? libPath);
  int create();
  void send(int clientId, td_api.TdFunction function, Object? extra);
  td_api.TdObject? receive(double timeout);
}

class NativeTdlibClient implements TdlibClient {
  const NativeTdlibClient();

  @override
  Future<void> initialize(String? libPath) {
    return TdPlugin.initialize(libPath);
  }

  @override
  int create() {
    return tdCreate();
  }

  @override
  td_api.TdObject? receive(double timeout) {
    return tdReceive(timeout);
  }

  @override
  void send(int clientId, td_api.TdFunction function, Object? extra) {
    tdSend(clientId, function, extra);
  }
}

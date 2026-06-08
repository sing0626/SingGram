import 'package:flutter/services.dart';

typedef TelegramJsonMap = Map<String, Object?>;

/// Flutter side of the Android TDLib bridge contract.
///
/// TODO(tdlib): Android currently returns placeholder responses. Keep this
/// API stable while native TDLib libraries, client lifecycle, and event streams
/// are added behind the MethodChannel.
class TelegramPlatformBridge {
  TelegramPlatformBridge({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel(channelName);

  static const String channelName = 'tgthird/telegram';

  final MethodChannel _channel;

  Future<TelegramEngineStatus> engineStatus() async {
    final response = await _invokeMap('engineStatus');
    return TelegramEngineStatus.fromJson(response);
  }

  Future<TelegramBridgeActionResult> configure(
    TelegramBridgeConfiguration configuration,
  ) async {
    final response = await _invokeMap('configure', configuration.toJson());
    return TelegramBridgeActionResult.fromJson(response);
  }

  Future<TelegramBridgeActionResult> startLogin(
    TelegramLoginRequest request,
  ) async {
    final response = await _invokeMap('startLogin', request.toJson());
    return TelegramBridgeActionResult.fromJson(response);
  }

  Future<TelegramBridgeActionResult> submitCode(String code) async {
    final response = await _invokeMap('submitCode', <String, Object?>{
      'code': code,
    });
    return TelegramBridgeActionResult.fromJson(response);
  }

  Future<TelegramBridgeActionResult> submitPassword(String password) async {
    final response = await _invokeMap('submitPassword', <String, Object?>{
      'password': password,
    });
    return TelegramBridgeActionResult.fromJson(response);
  }

  Future<TelegramBridgeActionResult> logout() async {
    final response = await _invokeMap('logout');
    return TelegramBridgeActionResult.fromJson(response);
  }

  Future<TelegramChatList> listChats({int limit = 50}) async {
    final response = await _invokeMap('listChats', <String, Object?>{
      'limit': limit,
    });
    return TelegramChatList.fromJson(response);
  }

  Future<TelegramMessageList> listMessages(
    TelegramMessageListRequest request,
  ) async {
    final response = await _invokeMap('listMessages', request.toJson());
    return TelegramMessageList.fromJson(response);
  }

  Future<TelegramSendTextResult> sendText(
    TelegramSendTextRequest request,
  ) async {
    final response = await _invokeMap('sendText', request.toJson());
    return TelegramSendTextResult.fromJson(response);
  }

  Future<TelegramJsonMap> _invokeMap(
    String method, [
    TelegramJsonMap? arguments,
  ]) async {
    final response = await _channel.invokeMethod<Object?>(method, arguments);
    return _jsonMap(response, '$method response');
  }
}

enum TelegramAuthorizationState {
  signedOut,
  configured,
  connecting,
  waitingCode,
  waitingPassword,
  ready,
  closed,
  error,
  unknown,
}

enum TelegramBridgeChatType {
  privateChat,
  basicGroup,
  supergroup,
  channel,
  secret,
  unknown,
}

class TelegramBridgeConfiguration {
  const TelegramBridgeConfiguration({
    required this.apiId,
    required this.apiHash,
    this.databaseDirectory,
    this.filesDirectory,
    this.databaseEncryptionKey,
    this.useTestDc = false,
    this.systemLanguageCode,
    this.deviceModel,
    this.systemVersion,
    this.applicationVersion,
  });

  final int apiId;
  final String apiHash;
  final String? databaseDirectory;
  final String? filesDirectory;
  final String? databaseEncryptionKey;
  final bool useTestDc;
  final String? systemLanguageCode;
  final String? deviceModel;
  final String? systemVersion;
  final String? applicationVersion;

  TelegramJsonMap toJson() {
    return <String, Object?>{
      'apiId': apiId,
      'apiHash': apiHash,
      'useTestDc': useTestDc,
      if (databaseDirectory != null) 'databaseDirectory': databaseDirectory,
      if (filesDirectory != null) 'filesDirectory': filesDirectory,
      if (databaseEncryptionKey != null)
        'databaseEncryptionKey': databaseEncryptionKey,
      if (systemLanguageCode != null) 'systemLanguageCode': systemLanguageCode,
      if (deviceModel != null) 'deviceModel': deviceModel,
      if (systemVersion != null) 'systemVersion': systemVersion,
      if (applicationVersion != null) 'applicationVersion': applicationVersion,
    };
  }
}

class TelegramLoginRequest {
  const TelegramLoginRequest({required this.phoneNumber});

  final String phoneNumber;

  TelegramJsonMap toJson() {
    return <String, Object?>{'phoneNumber': phoneNumber};
  }
}

class TelegramMessageListRequest {
  const TelegramMessageListRequest({
    required this.chatId,
    this.limit = 50,
    this.fromMessageId,
  });

  final String chatId;
  final int limit;
  final String? fromMessageId;

  TelegramJsonMap toJson() {
    return <String, Object?>{
      'chatId': chatId,
      'limit': limit,
      if (fromMessageId != null) 'fromMessageId': fromMessageId,
    };
  }
}

class TelegramSendTextRequest {
  const TelegramSendTextRequest({
    required this.chatId,
    required this.text,
    this.disableNotification = false,
  });

  final String chatId;
  final String text;
  final bool disableNotification;

  TelegramJsonMap toJson() {
    return <String, Object?>{
      'chatId': chatId,
      'text': text,
      'disableNotification': disableNotification,
    };
  }
}

class TelegramEngineStatus {
  const TelegramEngineStatus({
    required this.nativeTdlibAvailable,
    required this.configured,
    required this.authorizationState,
    this.tdlibVersion,
    this.statusMessage,
    this.placeholder = false,
  });

  factory TelegramEngineStatus.fromJson(TelegramJsonMap json) {
    return TelegramEngineStatus(
      nativeTdlibAvailable: _bool(json['nativeTdlibAvailable']),
      configured: _bool(json['configured']),
      authorizationState: _authorizationState(json['authorizationState']),
      tdlibVersion: _string(json['tdlibVersion']),
      statusMessage: _string(json['statusMessage']),
      placeholder: _bool(json['placeholder']),
    );
  }

  final bool nativeTdlibAvailable;
  final bool configured;
  final TelegramAuthorizationState authorizationState;
  final String? tdlibVersion;
  final String? statusMessage;
  final bool placeholder;
}

class TelegramBridgeActionResult {
  const TelegramBridgeActionResult({
    required this.ok,
    required this.authorizationState,
    this.statusMessage,
    this.placeholder = false,
  });

  factory TelegramBridgeActionResult.fromJson(TelegramJsonMap json) {
    return TelegramBridgeActionResult(
      ok: _bool(json['ok']),
      authorizationState: _authorizationState(json['authorizationState']),
      statusMessage: _string(json['statusMessage']),
      placeholder: _bool(json['placeholder']),
    );
  }

  final bool ok;
  final TelegramAuthorizationState authorizationState;
  final String? statusMessage;
  final bool placeholder;
}

class TelegramChatList {
  const TelegramChatList({
    required this.chats,
    this.nextOffset,
    this.placeholder = false,
  });

  factory TelegramChatList.fromJson(TelegramJsonMap json) {
    return TelegramChatList(
      chats: _jsonMapList(
        json['chats'],
        'chats',
      ).map(TelegramBridgeChat.fromJson).toList(growable: false),
      nextOffset: _string(json['nextOffset']),
      placeholder: _bool(json['placeholder']),
    );
  }

  final List<TelegramBridgeChat> chats;
  final String? nextOffset;
  final bool placeholder;
}

class TelegramBridgeChat {
  const TelegramBridgeChat({
    required this.id,
    required this.title,
    required this.type,
    required this.unreadCount,
    this.lastMessagePreview,
    this.updatedAt,
  });

  factory TelegramBridgeChat.fromJson(TelegramJsonMap json) {
    return TelegramBridgeChat(
      id: _requiredString(json['id'], 'chat.id'),
      title: _requiredString(json['title'], 'chat.title'),
      type: _chatType(json['type']),
      unreadCount: _int(json['unreadCount']) ?? 0,
      lastMessagePreview: _string(json['lastMessagePreview']),
      updatedAt: _epochMillis(json['updatedAtEpochMs']),
    );
  }

  final String id;
  final String title;
  final TelegramBridgeChatType type;
  final int unreadCount;
  final String? lastMessagePreview;
  final DateTime? updatedAt;
}

class TelegramMessageList {
  const TelegramMessageList({
    required this.messages,
    this.nextFromMessageId,
    this.placeholder = false,
  });

  factory TelegramMessageList.fromJson(TelegramJsonMap json) {
    return TelegramMessageList(
      messages: _jsonMapList(
        json['messages'],
        'messages',
      ).map(TelegramBridgeMessage.fromJson).toList(growable: false),
      nextFromMessageId: _string(json['nextFromMessageId']),
      placeholder: _bool(json['placeholder']),
    );
  }

  final List<TelegramBridgeMessage> messages;
  final String? nextFromMessageId;
  final bool placeholder;
}

class TelegramBridgeMessage {
  const TelegramBridgeMessage({
    required this.id,
    required this.chatId,
    required this.text,
    required this.sentAt,
    required this.outgoing,
    this.senderId,
    this.senderName,
    this.pending = false,
  });

  factory TelegramBridgeMessage.fromJson(TelegramJsonMap json) {
    return TelegramBridgeMessage(
      id: _requiredString(json['id'], 'message.id'),
      chatId: _requiredString(json['chatId'], 'message.chatId'),
      text: _requiredString(json['text'], 'message.text'),
      sentAt: _requiredDateTime(json['sentAtEpochMs'], 'message.sentAtEpochMs'),
      outgoing: _bool(json['outgoing']),
      senderId: _string(json['senderId']),
      senderName: _string(json['senderName']),
      pending: _bool(json['pending']),
    );
  }

  final String id;
  final String chatId;
  final String text;
  final DateTime sentAt;
  final bool outgoing;
  final String? senderId;
  final String? senderName;
  final bool pending;
}

class TelegramSendTextResult {
  const TelegramSendTextResult({
    required this.ok,
    required this.authorizationState,
    required this.sentMessage,
    this.statusMessage,
    this.placeholder = false,
  });

  factory TelegramSendTextResult.fromJson(TelegramJsonMap json) {
    return TelegramSendTextResult(
      ok: _bool(json['ok']),
      authorizationState: _authorizationState(json['authorizationState']),
      sentMessage: TelegramBridgeMessage.fromJson(
        _jsonMap(json['sentMessage'], 'sentMessage'),
      ),
      statusMessage: _string(json['statusMessage']),
      placeholder: _bool(json['placeholder']),
    );
  }

  final bool ok;
  final TelegramAuthorizationState authorizationState;
  final TelegramBridgeMessage sentMessage;
  final String? statusMessage;
  final bool placeholder;
}

TelegramJsonMap _jsonMap(Object? value, String context) {
  if (value is Map) {
    return value.map((key, value) => MapEntry(key.toString(), value));
  }

  throw PlatformException(
    code: 'bad_response',
    message: 'Expected $context to be a map, got ${value.runtimeType}.',
  );
}

List<TelegramJsonMap> _jsonMapList(Object? value, String context) {
  if (value is! List) {
    throw PlatformException(
      code: 'bad_response',
      message: 'Expected $context to be a list, got ${value.runtimeType}.',
    );
  }

  return value
      .map((item) => _jsonMap(item, '$context item'))
      .toList(growable: false);
}

String? _string(Object? value) {
  return value is String ? value : null;
}

String _requiredString(Object? value, String context) {
  final parsed = _string(value);
  if (parsed != null) return parsed;

  throw PlatformException(
    code: 'bad_response',
    message: 'Expected $context to be a string, got ${value.runtimeType}.',
  );
}

bool _bool(Object? value) {
  return value is bool ? value : false;
}

int? _int(Object? value) {
  if (value is int) return value;
  if (value is double) return value.toInt();
  return null;
}

DateTime? _epochMillis(Object? value) {
  final milliseconds = _int(value);
  if (milliseconds == null) return null;

  return DateTime.fromMillisecondsSinceEpoch(milliseconds, isUtc: true);
}

DateTime _requiredDateTime(Object? value, String context) {
  final parsed = _epochMillis(value);
  if (parsed != null) return parsed;

  throw PlatformException(
    code: 'bad_response',
    message:
        'Expected $context to be epoch milliseconds, '
        'got ${value.runtimeType}.',
  );
}

TelegramAuthorizationState _authorizationState(Object? value) {
  return switch (_string(value)) {
    'signedOut' => TelegramAuthorizationState.signedOut,
    'configured' => TelegramAuthorizationState.configured,
    'connecting' => TelegramAuthorizationState.connecting,
    'waitingCode' => TelegramAuthorizationState.waitingCode,
    'waitingPassword' => TelegramAuthorizationState.waitingPassword,
    'ready' => TelegramAuthorizationState.ready,
    'closed' => TelegramAuthorizationState.closed,
    'error' => TelegramAuthorizationState.error,
    _ => TelegramAuthorizationState.unknown,
  };
}

TelegramBridgeChatType _chatType(Object? value) {
  return switch (_string(value)) {
    'private' => TelegramBridgeChatType.privateChat,
    'basicGroup' => TelegramBridgeChatType.basicGroup,
    'supergroup' => TelegramBridgeChatType.supergroup,
    'channel' => TelegramBridgeChatType.channel,
    'secret' => TelegramBridgeChatType.secret,
    _ => TelegramBridgeChatType.unknown,
  };
}

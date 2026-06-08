import 'dart:math';

import 'package:flutter/foundation.dart';

import '../models/telegram_models.dart';

class FakeTelegramRepository extends ChangeNotifier {
  AuthState authState = const AuthState.signedOut();

  final List<ChatDialog> dialogs = [
    ChatDialog(
      id: 'saved',
      title: 'Saved Messages',
      kind: DialogKind.user,
      unreadCount: 0,
      lastMessage: 'Flutter glass baseline is ready.',
      lastMessageAt: DateTime.now().subtract(const Duration(minutes: 6)),
    ),
    ChatDialog(
      id: 'build',
      title: 'Build Plan',
      kind: DialogKind.group,
      unreadCount: 3,
      lastMessage: 'TDLib bridge can start on its own branch.',
      lastMessageAt: DateTime.now().subtract(const Duration(minutes: 18)),
    ),
    ChatDialog(
      id: 'notes',
      title: 'Private Notes',
      kind: DialogKind.channel,
      unreadCount: 0,
      lastMessage: 'Keep API ID, hash, sessions and APK keys private.',
      lastMessageAt: DateTime.now().subtract(const Duration(hours: 2)),
    ),
  ];

  final Map<String, List<ChatMessage>> _messages = {
    'saved': [
      ChatMessage(
        id: 'saved-1',
        chatId: 'saved',
        text: 'Flutter glass baseline is ready.',
        authorName: 'Sing',
        sentAt: DateTime.now().subtract(const Duration(minutes: 6)),
        outgoing: false,
      ),
    ],
    'build': [
      ChatMessage(
        id: 'build-1',
        chatId: 'build',
        text: 'UI is now Flutter-first.',
        authorName: 'Codex',
        sentAt: DateTime.now().subtract(const Duration(minutes: 22)),
        outgoing: false,
      ),
      ChatMessage(
        id: 'build-2',
        chatId: 'build',
        text: 'Native Android remains available for TDLib.',
        authorName: 'Codex',
        sentAt: DateTime.now().subtract(const Duration(minutes: 18)),
        outgoing: false,
      ),
    ],
    'notes': [
      ChatMessage(
        id: 'notes-1',
        chatId: 'notes',
        text: 'Keep API ID, hash, sessions and APK keys private.',
        authorName: null,
        sentAt: DateTime.now().subtract(const Duration(hours: 2)),
        outgoing: true,
      ),
    ],
  };

  List<ChatMessage> messagesFor(String chatId) {
    return List.unmodifiable(_messages[chatId] ?? const []);
  }

  Future<void> startLogin(LoginCredentials credentials) async {
    authState = const AuthState.connecting();
    notifyListeners();

    await Future<void>.delayed(const Duration(milliseconds: 420));
    authState = AuthState.waitingCode(credentials.phoneNumber);
    notifyListeners();
  }

  Future<void> submitCode(String code) async {
    if (code.trim().isEmpty) {
      authState = const AuthState.error('Login code is empty.');
      notifyListeners();
      return;
    }

    authState = const AuthState.ready(
      TelegramUser(id: 'local-user', displayName: 'Sing', username: 'sing'),
    );
    notifyListeners();
  }

  Future<void> submitPassword(String password) async {
    if (password.trim().isEmpty) {
      authState = const AuthState.error('Password is empty.');
      notifyListeners();
    }
  }

  Future<void> sendText({required String chatId, required String text}) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return;

    final message = ChatMessage(
      id: 'local-${DateTime.now().microsecondsSinceEpoch}-${Random().nextInt(999)}',
      chatId: chatId,
      text: trimmed,
      authorName: null,
      sentAt: DateTime.now(),
      outgoing: true,
    );

    _messages.update(
      chatId,
      (messages) => [...messages, message],
      ifAbsent: () => [message],
    );

    final dialogIndex = dialogs.indexWhere((dialog) => dialog.id == chatId);
    if (dialogIndex >= 0) {
      dialogs[dialogIndex] = dialogs[dialogIndex].copyWith(
        unreadCount: 0,
        lastMessage: trimmed,
        lastMessageAt: message.sentAt,
      );
    }

    notifyListeners();
  }

  Future<void> logout() async {
    authState = const AuthState.signedOut();
    notifyListeners();
  }
}

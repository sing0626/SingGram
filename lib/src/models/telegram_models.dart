enum AuthPhase {
  signedOut,
  connecting,
  waitingCode,
  waitingPassword,
  ready,
  error,
}

class AuthState {
  const AuthState({
    required this.phase,
    this.phoneNumber,
    this.user,
    this.message,
  });

  const AuthState.signedOut() : this(phase: AuthPhase.signedOut);
  const AuthState.connecting() : this(phase: AuthPhase.connecting);
  const AuthState.waitingCode(String phoneNumber)
    : this(phase: AuthPhase.waitingCode, phoneNumber: phoneNumber);
  const AuthState.waitingPassword(String phoneNumber)
    : this(phase: AuthPhase.waitingPassword, phoneNumber: phoneNumber);
  const AuthState.ready(TelegramUser user)
    : this(phase: AuthPhase.ready, user: user);
  const AuthState.error(String message)
    : this(phase: AuthPhase.error, message: message);

  final AuthPhase phase;
  final String? phoneNumber;
  final TelegramUser? user;
  final String? message;
}

class LoginCredentials {
  const LoginCredentials({
    required this.apiId,
    required this.apiHash,
    required this.phoneNumber,
  });

  final int apiId;
  final String apiHash;
  final String phoneNumber;
}

class TelegramUser {
  const TelegramUser({
    required this.id,
    required this.displayName,
    this.username,
  });

  final String id;
  final String displayName;
  final String? username;
}

enum DialogKind { user, group, channel, unknown }

class ChatDialog {
  const ChatDialog({
    required this.id,
    required this.title,
    required this.kind,
    required this.unreadCount,
    required this.lastMessage,
    required this.lastMessageAt,
  });

  final String id;
  final String title;
  final DialogKind kind;
  final int unreadCount;
  final String? lastMessage;
  final DateTime? lastMessageAt;

  ChatDialog copyWith({
    int? unreadCount,
    String? lastMessage,
    DateTime? lastMessageAt,
  }) {
    return ChatDialog(
      id: id,
      title: title,
      kind: kind,
      unreadCount: unreadCount ?? this.unreadCount,
      lastMessage: lastMessage ?? this.lastMessage,
      lastMessageAt: lastMessageAt ?? this.lastMessageAt,
    );
  }
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.chatId,
    required this.text,
    required this.authorName,
    required this.sentAt,
    required this.outgoing,
  });

  final String id;
  final String chatId;
  final String text;
  final String? authorName;
  final DateTime sentAt;
  final bool outgoing;
}

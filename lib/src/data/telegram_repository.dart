import 'package:flutter/foundation.dart';

import '../models/telegram_models.dart';

abstract class TelegramRepository extends ChangeNotifier {
  AuthState get authState;
  List<ChatDialog> get dialogs;

  List<ChatMessage> messagesFor(String chatId);

  Future<void> startLogin(LoginCredentials credentials);
  Future<void> submitCode(String code);
  Future<void> submitPassword(String password);
  Future<void> loadMessages(String chatId);
  Future<void> sendText({required String chatId, required String text});
  Future<void> logout();
}

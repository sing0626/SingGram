import 'package:flutter/foundation.dart';

import '../models/telegram_models.dart';
import 'telegram_api_credentials.dart';

abstract class TelegramRepository extends ChangeNotifier {
  AuthState get authState;
  List<ChatDialog> get dialogs;

  List<ChatMessage> messagesFor(String chatId);

  Future<TelegramApiCredentials?> loadApiCredentials();
  Future<void> saveApiCredentials(TelegramApiCredentials credentials);
  Future<void> startLogin(LoginCredentials credentials);
  Future<void> submitCode(String code);
  Future<void> submitPassword(String password);
  Future<void> loadMessages(String chatId);
  Future<void> sendText({required String chatId, required String text});
  Future<void> logout();
}

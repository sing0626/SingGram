import 'package:flutter/services.dart';

class TelegramPlatformBridge {
  TelegramPlatformBridge();

  static const MethodChannel _channel = MethodChannel('tgthird/telegram');

  Future<String> engineStatus() async {
    return await _channel.invokeMethod<String>('engineStatus') ??
        'TDLib bridge not connected';
  }
}

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tgthird/src/data/telegram_api_credentials.dart';

void main() {
  test('credential validity requires both api id and hash', () {
    expect(
      const TelegramApiCredentials(apiId: 123, apiHash: 'hash').isValid,
      isTrue,
    );
    expect(
      const TelegramApiCredentials(apiId: 0, apiHash: 'hash').isValid,
      isFalse,
    );
    expect(
      const TelegramApiCredentials(apiId: 123, apiHash: '').isValid,
      isFalse,
    );
  });

  test('secure credential store saves and loads credentials', () async {
    FlutterSecureStorage.setMockInitialValues({});
    const store = TelegramCredentialStore();

    await store.save(const TelegramApiCredentials(apiId: 123, apiHash: 'hash'));

    final loaded = await store.load();
    expect(loaded?.apiId, 123);
    expect(loaded?.apiHash, 'hash');
  });
}

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TelegramApiCredentials {
  const TelegramApiCredentials({required this.apiId, required this.apiHash});

  final int apiId;
  final String apiHash;

  bool get isValid => apiId > 0 && apiHash.trim().isNotEmpty;
}

class TelegramCredentialConfig {
  const TelegramCredentialConfig._();

  static const int _embeddedApiId = int.fromEnvironment(
    'TELEGRAM_API_ID',
    defaultValue: 0,
  );
  static const String _embeddedApiHash = String.fromEnvironment(
    'TELEGRAM_API_HASH',
  );

  static TelegramApiCredentials? get embeddedCredentials {
    final credentials = TelegramApiCredentials(
      apiId: _embeddedApiId,
      apiHash: _embeddedApiHash,
    );
    return credentials.isValid ? credentials : null;
  }

  static bool get hasEmbeddedCredentials => embeddedCredentials != null;
}

class TelegramCredentialStore {
  const TelegramCredentialStore({
    this.storage = const FlutterSecureStorage(
      aOptions: AndroidOptions(
        storageNamespace: 'tgthird.telegram_credentials',
      ),
    ),
  });

  static const _apiIdKey = 'telegram_api_id';
  static const _apiHashKey = 'telegram_api_hash';

  final FlutterSecureStorage storage;

  Future<TelegramApiCredentials?> load() async {
    final apiIdRaw = await storage.read(key: _apiIdKey);
    final apiHash = await storage.read(key: _apiHashKey);
    final apiId = int.tryParse(apiIdRaw ?? '');

    if (apiId == null || apiHash == null) return null;

    final credentials = TelegramApiCredentials(apiId: apiId, apiHash: apiHash);
    return credentials.isValid ? credentials : null;
  }

  Future<void> save(TelegramApiCredentials credentials) async {
    if (!credentials.isValid) return;

    await storage.write(key: _apiIdKey, value: credentials.apiId.toString());
    await storage.write(key: _apiHashKey, value: credentials.apiHash);
  }
}

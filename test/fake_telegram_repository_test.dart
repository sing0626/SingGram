import 'package:flutter_test/flutter_test.dart';
import 'package:tgthird/src/data/fake_telegram_repository.dart';
import 'package:tgthird/src/models/telegram_models.dart';

void main() {
  test('login moves from code challenge to ready', () async {
    final repository = FakeTelegramRepository();

    await repository.startLogin(
      const LoginCredentials(
        apiId: 1,
        apiHash: 'hash',
        phoneNumber: '+85200000000',
      ),
    );
    expect(repository.authState.phase, AuthPhase.waitingCode);

    await repository.submitCode('12345');
    expect(repository.authState.phase, AuthPhase.ready);
  });

  test('sendText appends a local outgoing message', () async {
    final repository = FakeTelegramRepository();
    final before = repository.messagesFor('saved').length;

    await repository.sendText(chatId: 'saved', text: 'hello');

    final messages = repository.messagesFor('saved');
    expect(messages.length, before + 1);
    expect(messages.last.text, 'hello');
    expect(messages.last.outgoing, isTrue);
  });
}

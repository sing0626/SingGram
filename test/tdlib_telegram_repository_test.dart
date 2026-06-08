import 'package:flutter_test/flutter_test.dart';
import 'package:tdlib/td_api.dart' as td_api;
import 'package:tgthird/src/data/tdlib_telegram_repository.dart';
import 'package:tgthird/src/models/telegram_models.dart';

void main() {
  test('auth state machine sends phone and waits for login code', () async {
    final tdlib = FakeTdlibClient();
    final repository = TdlibTelegramRepository(
      tdlib: tdlib,
      dirs: const FakeDirectoryProvider(),
    );

    await repository.startLogin(
      const LoginCredentials(
        apiId: 123,
        apiHash: 'hash',
        phoneNumber: '+85200000000',
      ),
    );

    tdlib.push(const td_api.AuthorizationStateWaitTdlibParameters());
    await waitForReceiveLoop();
    expect(tdlib.sentTypes, contains('setTdlibParameters'));
    final parameters = tdlib.sent.last.function as td_api.SetTdlibParameters;
    expect(parameters.apiId, 123);
    expect(parameters.apiHash, 'hash');
    expect(parameters.databaseDirectory, '/tmp/tdlib/db');

    tdlib.push(const td_api.AuthorizationStateWaitPhoneNumber());
    await waitForReceiveLoop();
    expect(tdlib.sentTypes, contains('setAuthenticationPhoneNumber'));
    final phoneRequest =
        tdlib.sent.last.function as td_api.SetAuthenticationPhoneNumber;
    expect(phoneRequest.phoneNumber, '+85200000000');

    tdlib.push(
      td_api.AuthorizationStateWaitCode(
        codeInfo: const td_api.AuthenticationCodeInfo(
          phoneNumber: '+85200000000',
          type: td_api.AuthenticationCodeTypeTelegramMessage(length: 5),
          timeout: 60,
        ),
      ),
    );
    await waitForReceiveLoop();

    expect(repository.authState.phase, AuthPhase.waitingCode);
    expect(repository.authState.phoneNumber, '+85200000000');

    repository.dispose();
  });

  test('ready auth loads real Telegram display name from getMe', () async {
    final tdlib = FakeTdlibClient();
    final repository = TdlibTelegramRepository(
      tdlib: tdlib,
      dirs: const FakeDirectoryProvider(),
    );

    await repository.startLogin(
      const LoginCredentials(
        apiId: 123,
        apiHash: 'hash',
        phoneNumber: '+85200000000',
      ),
    );

    tdlib.push(const td_api.AuthorizationStateReady());
    await waitForReceiveLoop();

    final getMeRequest = tdlib.sent.singleWhere(
      (request) => request.function.getConstructor() == 'getMe',
    );
    tdlib.push(_user(extra: getMeRequest.extra));
    await waitForReceiveLoop();

    final getChatsRequest = tdlib.sent.singleWhere(
      (request) => request.function.getConstructor() == 'getChats',
    );
    tdlib.push(
      td_api.Chats(
        totalCount: 0,
        chatIds: const [],
        extra: getChatsRequest.extra,
      ),
    );
    await waitForReceiveLoop();

    expect(repository.authState.phase, AuthPhase.ready);
    expect(repository.authState.user?.displayName, 'Sing Tester');
    expect(repository.authState.user?.username, 'singtester');

    repository.dispose();
  });

  test('submitCode sends TDLib checkAuthenticationCode', () async {
    final tdlib = FakeTdlibClient();
    final repository = TdlibTelegramRepository(
      tdlib: tdlib,
      dirs: const FakeDirectoryProvider(),
    );

    await repository.startLogin(
      const LoginCredentials(
        apiId: 123,
        apiHash: 'hash',
        phoneNumber: '+85200000000',
      ),
    );

    await repository.submitCode('12345');

    final codeRequest =
        tdlib.sent.last.function as td_api.CheckAuthenticationCode;
    expect(codeRequest.code, '12345');

    repository.dispose();
  });
}

Future<void> waitForReceiveLoop() {
  return Future<void>.delayed(const Duration(milliseconds: 120));
}

td_api.User _user({Object? extra}) {
  return td_api.User(
    id: 42,
    firstName: 'Sing',
    lastName: 'Tester',
    usernames: const td_api.Usernames(
      activeUsernames: ['singtester'],
      disabledUsernames: [],
      editableUsername: 'singtester',
    ),
    phoneNumber: '85200000000',
    status: const td_api.UserStatusEmpty(),
    isContact: false,
    isMutualContact: false,
    isCloseFriend: false,
    isVerified: false,
    isPremium: false,
    isSupport: false,
    restrictionReason: '',
    isScam: false,
    isFake: false,
    hasActiveStories: false,
    hasUnreadActiveStories: false,
    haveAccess: true,
    type: const td_api.UserTypeRegular(),
    languageCode: '',
    addedToAttachmentMenu: false,
    extra: extra,
  );
}

class FakeDirectoryProvider implements TdlibDirectoryProvider {
  const FakeDirectoryProvider();

  @override
  Future<TdlibDirectories> directories() async {
    return const TdlibDirectories(
      databaseDirectory: '/tmp/tdlib/db',
      filesDirectory: '/tmp/tdlib/files',
    );
  }
}

class FakeTdlibClient implements TdlibClient {
  final List<td_api.TdObject> updates = [];
  final List<SentTdlibRequest> sent = [];

  List<String> get sentTypes {
    return sent.map((request) => request.function.getConstructor()).toList();
  }

  void push(td_api.TdObject object) {
    updates.add(object);
  }

  @override
  int create() {
    return 1;
  }

  @override
  Future<void> initialize(String? libPath) async {}

  @override
  td_api.TdObject? receive(double timeout) {
    if (updates.isEmpty) return null;
    return updates.removeAt(0);
  }

  @override
  void send(int clientId, td_api.TdFunction function, Object? extra) {
    sent.add(SentTdlibRequest(function: function, extra: extra));
  }
}

class SentTdlibRequest {
  const SentTdlibRequest({required this.function, required this.extra});

  final td_api.TdFunction function;
  final Object? extra;
}

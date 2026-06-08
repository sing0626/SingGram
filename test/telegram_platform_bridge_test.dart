import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tgthird/src/platform/telegram_platform_bridge.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(TelegramPlatformBridge.channelName);
  late TelegramPlatformBridge bridge;

  setUp(() {
    bridge = TelegramPlatformBridge(channel: channel);
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('engineStatus parses structured platform status', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          expect(call.method, 'engineStatus');
          return <String, Object?>{
            'nativeTdlibAvailable': false,
            'configured': true,
            'authorizationState': 'configured',
            'tdlibVersion': null,
            'statusMessage': 'placeholder',
            'placeholder': true,
          };
        });

    final status = await bridge.engineStatus();

    expect(status.nativeTdlibAvailable, isFalse);
    expect(status.configured, isTrue);
    expect(status.authorizationState, TelegramAuthorizationState.configured);
    expect(status.placeholder, isTrue);
  });

  test(
    'configure sends typed configuration and parses action result',
    () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
            expect(call.method, 'configure');
            final args = call.arguments as Map<dynamic, dynamic>;
            expect(args['apiId'], 123);
            expect(args['apiHash'], 'hash');
            expect(args['useTestDc'], isTrue);
            expect(args['databaseDirectory'], '/td/db');

            return <String, Object?>{
              'ok': true,
              'authorizationState': 'configured',
              'statusMessage': 'accepted',
              'placeholder': true,
            };
          });

      final result = await bridge.configure(
        const TelegramBridgeConfiguration(
          apiId: 123,
          apiHash: 'hash',
          useTestDc: true,
          databaseDirectory: '/td/db',
        ),
      );

      expect(result.ok, isTrue);
      expect(result.authorizationState, TelegramAuthorizationState.configured);
      expect(result.placeholder, isTrue);
    },
  );

  test('listChats parses typed chat rows', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          expect(call.method, 'listChats');
          final args = call.arguments as Map<dynamic, dynamic>;
          expect(args['limit'], 10);

          return <String, Object?>{
            'chats': <Map<String, Object?>>[
              <String, Object?>{
                'id': 'chat-1',
                'title': 'Saved Messages',
                'type': 'private',
                'unreadCount': 0,
                'lastMessagePreview': 'hello',
                'updatedAtEpochMs': 1710000000000,
              },
            ],
            'nextOffset': null,
            'placeholder': true,
          };
        });

    final result = await bridge.listChats(limit: 10);

    expect(result.placeholder, isTrue);
    expect(result.chats, hasLength(1));
    expect(result.chats.single.id, 'chat-1');
    expect(result.chats.single.type, TelegramBridgeChatType.privateChat);
    expect(result.chats.single.updatedAt, isNotNull);
  });

  test('listMessages sends request and parses messages', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          expect(call.method, 'listMessages');
          final args = call.arguments as Map<dynamic, dynamic>;
          expect(args['chatId'], 'chat-1');
          expect(args['limit'], 25);
          expect(args['fromMessageId'], 'cursor');

          return <String, Object?>{
            'messages': <Map<String, Object?>>[
              <String, Object?>{
                'id': 'message-1',
                'chatId': 'chat-1',
                'senderId': 'sender-1',
                'senderName': 'Sender',
                'text': 'hello',
                'sentAtEpochMs': 1710000000000,
                'outgoing': false,
                'pending': false,
              },
            ],
            'nextFromMessageId': null,
            'placeholder': true,
          };
        });

    final result = await bridge.listMessages(
      const TelegramMessageListRequest(
        chatId: 'chat-1',
        limit: 25,
        fromMessageId: 'cursor',
      ),
    );

    expect(result.messages, hasLength(1));
    expect(result.messages.single.text, 'hello');
    expect(result.messages.single.outgoing, isFalse);
  });

  test('sendText sends request and parses queued message result', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          expect(call.method, 'sendText');
          final args = call.arguments as Map<dynamic, dynamic>;
          expect(args['chatId'], 'chat-1');
          expect(args['text'], 'hi');
          expect(args['disableNotification'], isFalse);

          return <String, Object?>{
            'ok': true,
            'authorizationState': 'ready',
            'statusMessage': 'queued',
            'placeholder': true,
            'sentMessage': <String, Object?>{
              'id': 'message-local',
              'chatId': 'chat-1',
              'text': 'hi',
              'sentAtEpochMs': 1710000000000,
              'outgoing': true,
              'pending': true,
            },
          };
        });

    final result = await bridge.sendText(
      const TelegramSendTextRequest(chatId: 'chat-1', text: 'hi'),
    );

    expect(result.ok, isTrue);
    expect(result.authorizationState, TelegramAuthorizationState.ready);
    expect(result.sentMessage.id, 'message-local');
    expect(result.sentMessage.pending, isTrue);
  });
}

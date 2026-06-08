import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import 'data/fake_telegram_repository.dart';
import 'models/telegram_models.dart';
import 'screens/auth_screen.dart';
import 'screens/chat_screen.dart';
import 'widgets/liquid_backdrop.dart';

class TgThirdApp extends StatefulWidget {
  const TgThirdApp({super.key});

  @override
  State<TgThirdApp> createState() => _TgThirdAppState();
}

class _TgThirdAppState extends State<TgThirdApp> {
  late final FakeTelegramRepository repository;

  @override
  void initState() {
    super.initState();
    repository = FakeTelegramRepository();
  }

  @override
  void dispose() {
    repository.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'TG Third',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF46B6A8),
          brightness: Brightness.light,
        ),
        fontFamily: 'Roboto',
        useMaterial3: true,
      ),
      home: GlassScaffold(
        edgeToEdge: true,
        statusBarStyle: GlassStatusBarStyle.dark,
        background: const LiquidBackdrop(),
        body: AnimatedBuilder(
          animation: repository,
          builder: (context, _) {
            return AnimatedSwitcher(
              duration: const Duration(milliseconds: 240),
              child: repository.authState.phase == AuthPhase.ready
                  ? ChatScreen(repository: repository)
                  : AuthScreen(repository: repository),
            );
          },
        ),
      ),
    );
  }
}

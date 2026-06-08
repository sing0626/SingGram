import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../data/telegram_api_credentials.dart';
import '../data/telegram_repository.dart';
import '../models/telegram_models.dart';
import '../widgets/glass_helpers.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({required this.repository, super.key});

  final TelegramRepository repository;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final apiIdController = TextEditingController();
  final apiHashController = TextEditingController();
  final phoneController = TextEditingController();
  final codeController = TextEditingController();
  final passwordController = TextEditingController();
  final embeddedCredentials = TelegramCredentialConfig.embeddedCredentials;

  bool credentialsReady = false;

  @override
  void initState() {
    super.initState();
    _loadCredentialDefaults();
  }

  @override
  void dispose() {
    apiIdController.dispose();
    apiHashController.dispose();
    phoneController.dispose();
    codeController.dispose();
    passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = widget.repository.authState;

    return SafeArea(
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: GlassPanel(
              useOwnLayer: true,
              padding: const EdgeInsets.all(18),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      const AppGlyph(),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'TG Third',
                              style: Theme.of(context).textTheme.headlineSmall
                                  ?.copyWith(fontWeight: FontWeight.w800),
                            ),
                            Text(
                              _authLabel(authState),
                              style: Theme.of(context).textTheme.bodyMedium,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  if (authState.phase == AuthPhase.waitingCode)
                    _CodeForm(
                      controller: codeController,
                      onSubmit: () =>
                          widget.repository.submitCode(codeController.text),
                    )
                  else if (authState.phase == AuthPhase.waitingPassword)
                    _PasswordForm(
                      controller: passwordController,
                      onSubmit: () => widget.repository.submitPassword(
                        passwordController.text,
                      ),
                    )
                  else
                    _CredentialForm(
                      apiIdController: apiIdController,
                      apiHashController: apiHashController,
                      phoneController: phoneController,
                      hasEmbeddedCredentials: embeddedCredentials != null,
                      connecting: authState.phase == AuthPhase.connecting,
                      credentialsReady: credentialsReady,
                      onSubmit: _startLogin,
                    ),
                  if (authState.phase == AuthPhase.error &&
                      authState.message != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      authState.message!,
                      style: const TextStyle(
                        color: Color(0xFF9B2F21),
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _loadCredentialDefaults() async {
    final embedded = embeddedCredentials;
    if (embedded != null) {
      apiIdController.text = embedded.apiId.toString();
      apiHashController.text = embedded.apiHash;
      setState(() => credentialsReady = true);
      return;
    }

    final stored = await widget.repository.loadApiCredentials();
    if (!mounted) return;

    if (stored != null) {
      apiIdController.text = stored.apiId.toString();
      apiHashController.text = stored.apiHash;
    }
    setState(() => credentialsReady = true);
  }

  Future<void> _startLogin() async {
    final credentials = embeddedCredentials;
    final apiId =
        credentials?.apiId ?? int.tryParse(apiIdController.text.trim());
    final apiHash = credentials?.apiHash ?? apiHashController.text.trim();
    final phoneNumber = phoneController.text.trim();

    if (apiId == null || apiHash.isEmpty || phoneNumber.isEmpty) {
      return;
    }

    if (credentials == null) {
      await widget.repository.saveApiCredentials(
        TelegramApiCredentials(apiId: apiId, apiHash: apiHash),
      );
    }

    await widget.repository.startLogin(
      LoginCredentials(
        apiId: apiId,
        apiHash: apiHash,
        phoneNumber: phoneNumber,
      ),
    );
  }

  String _authLabel(AuthState state) {
    switch (state.phase) {
      case AuthPhase.signedOut:
        return 'Signed out';
      case AuthPhase.connecting:
        return 'Connecting';
      case AuthPhase.waitingCode:
        return 'Code required';
      case AuthPhase.waitingPassword:
        return 'Password required';
      case AuthPhase.ready:
        return state.user?.displayName ?? 'Ready';
      case AuthPhase.error:
        return 'Error';
    }
  }
}

class _CredentialForm extends StatelessWidget {
  const _CredentialForm({
    required this.apiIdController,
    required this.apiHashController,
    required this.phoneController,
    required this.hasEmbeddedCredentials,
    required this.connecting,
    required this.credentialsReady,
    required this.onSubmit,
  });

  final TextEditingController apiIdController;
  final TextEditingController apiHashController;
  final TextEditingController phoneController;
  final bool hasEmbeddedCredentials;
  final bool connecting;
  final bool credentialsReady;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (!hasEmbeddedCredentials) ...[
          GlassTextField(
            controller: apiIdController,
            placeholder: 'API ID',
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.next,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            prefixIcon: const Icon(Icons.numbers_rounded, size: 19),
          ),
          const SizedBox(height: 10),
          GlassTextField(
            controller: apiHashController,
            placeholder: 'API hash',
            obscureText: true,
            textInputAction: TextInputAction.next,
            prefixIcon: const Icon(Icons.key_rounded, size: 19),
          ),
          const SizedBox(height: 10),
        ],
        GlassTextField(
          controller: phoneController,
          placeholder: 'Phone number',
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => onSubmit(),
          prefixIcon: const Icon(Icons.phone_rounded, size: 19),
        ),
        const SizedBox(height: 16),
        GlassButton.custom(
          onTap: onSubmit,
          enabled: !connecting && credentialsReady,
          height: 52,
          width: double.infinity,
          shape: const LiquidRoundedSuperellipse(borderRadius: 16),
          child: Text(connecting ? 'Connecting' : 'Login'),
        ),
      ],
    );
  }
}

class _CodeForm extends StatelessWidget {
  const _CodeForm({required this.controller, required this.onSubmit});

  final TextEditingController controller;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GlassTextField(
          controller: controller,
          autofocus: true,
          placeholder: 'Login code',
          keyboardType: TextInputType.number,
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => onSubmit(),
          prefixIcon: const Icon(Icons.password_rounded, size: 19),
        ),
        const SizedBox(height: 16),
        GlassButton.custom(
          onTap: onSubmit,
          height: 52,
          width: double.infinity,
          shape: const LiquidRoundedSuperellipse(borderRadius: 16),
          child: const Text('Continue'),
        ),
      ],
    );
  }
}

class _PasswordForm extends StatelessWidget {
  const _PasswordForm({required this.controller, required this.onSubmit});

  final TextEditingController controller;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GlassTextField(
          controller: controller,
          autofocus: true,
          placeholder: '2FA password',
          obscureText: true,
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => onSubmit(),
          prefixIcon: const Icon(Icons.lock_rounded, size: 19),
        ),
        const SizedBox(height: 16),
        GlassButton.custom(
          onTap: onSubmit,
          height: 52,
          width: double.infinity,
          shape: const LiquidRoundedSuperellipse(borderRadius: 16),
          child: const Text('Continue'),
        ),
      ],
    );
  }
}

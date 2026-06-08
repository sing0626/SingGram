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

  _CountryDialCode selectedCountry = _countryDialCodes.first;
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
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 430),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Center(child: AppGlyph(size: 72)),
                const SizedBox(height: 26),
                Text(
                  _authTitle(authState),
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: const Color(0xFF143C38),
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  _authSubtitle(authState),
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Colors.black.withValues(alpha: 0.58),
                    fontSize: 15,
                    height: 1.35,
                  ),
                ),
                const SizedBox(height: 28),
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
                    country: selectedCountry,
                    hasEmbeddedCredentials: embeddedCredentials != null,
                    connecting: authState.phase == AuthPhase.connecting,
                    credentialsReady: credentialsReady,
                    onCountryTap: _pickCountry,
                    onSubmit: _startLogin,
                  ),
                if (authState.phase == AuthPhase.error &&
                    authState.message != null) ...[
                  const SizedBox(height: 16),
                  Text(
                    authState.message!,
                    textAlign: TextAlign.center,
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
    final phoneNumber = _normalizedPhoneNumber();

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

  Future<void> _pickCountry() async {
    final selected = await showModalBottomSheet<_CountryDialCode>(
      context: context,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return GlassPanel(
          radius: 28,
          margin: const EdgeInsets.all(12),
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: ListView.separated(
            shrinkWrap: true,
            itemCount: _countryDialCodes.length,
            separatorBuilder: (_, _) =>
                Divider(height: 1, color: Colors.black.withValues(alpha: 0.06)),
            itemBuilder: (context, index) {
              final country = _countryDialCodes[index];
              return ListTile(
                title: Text(country.name),
                trailing: Text(
                  country.dialCode,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                onTap: () => Navigator.of(context).pop(country),
              );
            },
          ),
        );
      },
    );

    if (selected != null) {
      setState(() => selectedCountry = selected);
    }
  }

  String _normalizedPhoneNumber() {
    final raw = phoneController.text.trim();
    if (raw.isEmpty) return '';
    if (raw.startsWith('+')) return raw.replaceAll(RegExp(r'\s+'), '');

    final nationalNumber = raw.replaceAll(RegExp(r'[^0-9]'), '');
    if (nationalNumber.isEmpty) return '';
    return '${selectedCountry.dialCode}$nationalNumber';
  }

  String _authTitle(AuthState state) {
    switch (state.phase) {
      case AuthPhase.waitingCode:
        return 'Enter code';
      case AuthPhase.waitingPassword:
        return 'Two-step verification';
      case AuthPhase.connecting:
        return 'Connecting';
      case AuthPhase.ready:
        return state.user?.displayName ?? 'Ready';
      case AuthPhase.error:
      case AuthPhase.signedOut:
        return 'Your phone';
    }
  }

  String _authSubtitle(AuthState state) {
    switch (state.phase) {
      case AuthPhase.waitingCode:
        return 'We have sent you a Telegram login code.';
      case AuthPhase.waitingPassword:
        return 'Enter your Telegram 2FA password to continue.';
      case AuthPhase.connecting:
        return 'Checking your Telegram account.';
      case AuthPhase.ready:
        return 'Signed in';
      case AuthPhase.error:
      case AuthPhase.signedOut:
        return 'Please confirm your country code and enter your phone number.';
    }
  }
}

class _CredentialForm extends StatelessWidget {
  const _CredentialForm({
    required this.apiIdController,
    required this.apiHashController,
    required this.phoneController,
    required this.country,
    required this.hasEmbeddedCredentials,
    required this.connecting,
    required this.credentialsReady,
    required this.onCountryTap,
    required this.onSubmit,
  });

  final TextEditingController apiIdController;
  final TextEditingController apiHashController;
  final TextEditingController phoneController;
  final _CountryDialCode country;
  final bool hasEmbeddedCredentials;
  final bool connecting;
  final bool credentialsReady;
  final VoidCallback onCountryTap;
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
        GlassPanel(
          padding: EdgeInsets.zero,
          radius: 18,
          child: Column(
            children: [
              InkWell(
                onTap: onCountryTap,
                borderRadius: BorderRadius.circular(18),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 14,
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          country.name,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      Text(
                        country.dialCode,
                        style: TextStyle(
                          color: Colors.black.withValues(alpha: 0.58),
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(width: 6),
                      const Icon(Icons.keyboard_arrow_down_rounded),
                    ],
                  ),
                ),
              ),
              Divider(height: 1, color: Colors.black.withValues(alpha: 0.08)),
              Row(
                children: [
                  SizedBox(
                    width: 92,
                    child: Padding(
                      padding: const EdgeInsets.only(left: 16),
                      child: Text(
                        country.dialCode,
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: TextField(
                      controller: phoneController,
                      autofocus: true,
                      keyboardType: TextInputType.phone,
                      textInputAction: TextInputAction.done,
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(
                          RegExp(r'[0-9\s()-]'),
                        ),
                      ],
                      onSubmitted: (_) => onSubmit(),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        hintText: 'Phone number',
                        contentPadding: EdgeInsets.symmetric(
                          vertical: 16,
                          horizontal: 12,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 22),
        Align(
          alignment: Alignment.centerRight,
          child: GlassButton.custom(
            onTap: onSubmit,
            enabled: !connecting && credentialsReady,
            height: 54,
            width: 54,
            shape: const LiquidRoundedSuperellipse(borderRadius: 18),
            child: Icon(
              connecting
                  ? Icons.hourglass_top_rounded
                  : Icons.arrow_forward_rounded,
            ),
          ),
        ),
      ],
    );
  }
}

class _CountryDialCode {
  const _CountryDialCode({required this.name, required this.dialCode});

  final String name;
  final String dialCode;
}

const _countryDialCodes = <_CountryDialCode>[
  _CountryDialCode(name: 'Hong Kong', dialCode: '+852'),
  _CountryDialCode(name: 'Macau', dialCode: '+853'),
  _CountryDialCode(name: 'China', dialCode: '+86'),
  _CountryDialCode(name: 'Taiwan', dialCode: '+886'),
  _CountryDialCode(name: 'United States', dialCode: '+1'),
  _CountryDialCode(name: 'United Kingdom', dialCode: '+44'),
  _CountryDialCode(name: 'Japan', dialCode: '+81'),
  _CountryDialCode(name: 'South Korea', dialCode: '+82'),
  _CountryDialCode(name: 'Singapore', dialCode: '+65'),
  _CountryDialCode(name: 'Malaysia', dialCode: '+60'),
  _CountryDialCode(name: 'Thailand', dialCode: '+66'),
  _CountryDialCode(name: 'Australia', dialCode: '+61'),
  _CountryDialCode(name: 'Canada', dialCode: '+1'),
];

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

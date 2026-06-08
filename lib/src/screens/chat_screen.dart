import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../data/fake_telegram_repository.dart';
import '../models/telegram_models.dart';
import '../widgets/glass_helpers.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({required this.repository, super.key});

  final FakeTelegramRepository repository;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final searchController = TextEditingController();
  final composerController = TextEditingController();
  String? selectedChatId;
  String query = '';

  @override
  void dispose() {
    searchController.dispose();
    composerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final compact = MediaQuery.sizeOf(context).width < 720;
    final selectedDialog = widget.repository.dialogs
        .where((dialog) => dialog.id == selectedChatId)
        .firstOrNull;
    final fallbackDialog = compact
        ? null
        : selectedDialog ?? widget.repository.dialogs.firstOrNull;

    if (compact && selectedDialog == null) {
      return _DialogList(
        repository: widget.repository,
        query: query,
        searchController: searchController,
        onQueryChanged: (value) => setState(() => query = value),
        selectedChatId: selectedChatId,
        onSelect: (chatId) => setState(() => selectedChatId = chatId),
      );
    }

    if (compact) {
      return _Conversation(
        repository: widget.repository,
        dialog: selectedDialog,
        composerController: composerController,
        onBack: () => setState(() => selectedChatId = null),
      );
    }

    return SafeArea(
      child: Row(
        children: [
          SizedBox(
            width: 360,
            child: _DialogList(
              repository: widget.repository,
              query: query,
              searchController: searchController,
              onQueryChanged: (value) => setState(() => query = value),
              selectedChatId: fallbackDialog?.id,
              onSelect: (chatId) => setState(() => selectedChatId = chatId),
            ),
          ),
          Expanded(
            child: _Conversation(
              repository: widget.repository,
              dialog: fallbackDialog,
              composerController: composerController,
            ),
          ),
        ],
      ),
    );
  }
}

class _DialogList extends StatelessWidget {
  const _DialogList({
    required this.repository,
    required this.query,
    required this.searchController,
    required this.onQueryChanged,
    required this.selectedChatId,
    required this.onSelect,
  });

  final FakeTelegramRepository repository;
  final String query;
  final TextEditingController searchController;
  final ValueChanged<String> onQueryChanged;
  final String? selectedChatId;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) {
    final user = repository.authState.user;
    final normalizedQuery = query.trim().toLowerCase();
    final dialogs = repository.dialogs
        .where(
          (dialog) =>
              normalizedQuery.isEmpty ||
              dialog.title.toLowerCase().contains(normalizedQuery),
        )
        .toList();

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 14, 10, 14),
        child: Column(
          children: [
            GlassPanel(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  const AppGlyph(size: 38),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.displayName ?? 'TG Third',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                        Text(
                          '@${user?.username ?? 'me'}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  GlassIconButton(
                    icon: const Icon(Icons.logout_rounded),
                    onPressed: repository.logout,
                    shape: GlassIconButtonShape.roundedSquare,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            GlassSearchBar(
              controller: searchController,
              placeholder: 'Search',
              onChanged: onQueryChanged,
              searchIconColor: const Color(0xFF143C38),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: ListView.separated(
                itemCount: dialogs.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final dialog = dialogs[index];
                  return _DialogTile(
                    dialog: dialog,
                    selected: dialog.id == selectedChatId,
                    onTap: () => onSelect(dialog.id),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DialogTile extends StatelessWidget {
  const _DialogTile({
    required this.dialog,
    required this.selected,
    required this.onTap,
  });

  final ChatDialog dialog;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: GlassPanel(
        padding: const EdgeInsets.all(12),
        useOwnLayer: selected,
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: selected
                  ? const Color(0xFF46B6A8)
                  : const Color(0xFFEAF8F5),
              foregroundColor: const Color(0xFF143C38),
              child: Text(dialog.title.characters.first.toUpperCase()),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    dialog.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    dialog.lastMessage ?? dialog.kind.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: Colors.black.withValues(alpha: 0.58),
                    ),
                  ),
                ],
              ),
            ),
            if (dialog.unreadCount > 0)
              Container(
                height: 24,
                constraints: const BoxConstraints(minWidth: 24),
                padding: const EdgeInsets.symmetric(horizontal: 7),
                decoration: BoxDecoration(
                  color: const Color(0xFFD66D58),
                  borderRadius: BorderRadius.circular(99),
                ),
                alignment: Alignment.center,
                child: Text(
                  dialog.unreadCount.toString(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _Conversation extends StatelessWidget {
  const _Conversation({
    required this.repository,
    required this.dialog,
    required this.composerController,
    this.onBack,
  });

  final FakeTelegramRepository repository;
  final ChatDialog? dialog;
  final TextEditingController composerController;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    final messages = dialog == null
        ? <ChatMessage>[]
        : repository.messagesFor(dialog!.id);

    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(onBack == null ? 8 : 14, 14, 14, 14),
        child: Column(
          children: [
            GlassPanel(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              child: Row(
                children: [
                  if (onBack != null) ...[
                    GlassIconButton(
                      icon: const Icon(Icons.chevron_left_rounded),
                      onPressed: onBack,
                      shape: GlassIconButtonShape.roundedSquare,
                    ),
                    const SizedBox(width: 8),
                  ],
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          dialog?.title ?? 'Dialogs',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                        Text(dialog?.kind.name ?? 'ready'),
                      ],
                    ),
                  ),
                  GlassIconButton(
                    icon: const Icon(Icons.more_horiz_rounded),
                    onPressed: () {},
                    shape: GlassIconButtonShape.roundedSquare,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: messages.isEmpty
                  ? Center(
                      child: GlassPanel(
                        useOwnLayer: true,
                        child: Text(
                          dialog == null ? 'Select a chat' : 'No messages',
                        ),
                      ),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      itemCount: messages.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 8),
                      itemBuilder: (context, index) =>
                          _MessageBubble(message: messages[index]),
                    ),
            ),
            const SizedBox(height: 8),
            GlassPanel(
              padding: const EdgeInsets.all(8),
              child: Row(
                children: [
                  Expanded(
                    child: GlassTextField(
                      controller: composerController,
                      enabled: dialog != null,
                      placeholder: dialog == null ? 'Select a chat' : 'Message',
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _send(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  GlassIconButton(
                    icon: const Icon(Icons.arrow_upward_rounded),
                    onPressed: dialog == null ? null : _send,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _send() {
    final chatId = dialog?.id;
    if (chatId == null) return;

    final text = composerController.text;
    composerController.clear();
    repository.sendText(chatId: chatId, text: text);
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: message.outgoing
          ? Alignment.centerRight
          : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.72,
        ),
        child: GlassPanel(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          radius: 18,
          useOwnLayer: message.outgoing,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (message.authorName != null) ...[
                Text(
                  message.authorName!,
                  style: const TextStyle(
                    color: Color(0xFF9B4D37),
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
              ],
              Text(message.text),
            ],
          ),
        ),
      ),
    );
  }
}

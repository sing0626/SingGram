import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import '../data/telegram_repository.dart';
import '../models/telegram_models.dart';
import '../widgets/glass_helpers.dart';

const _telegramBlue = Color(0xFF2AABEE);
const _telegramInk = Color(0xFF17212B);
const _telegramMuted = Color(0xFF6E7F8D);
const _outgoingBubble = Color(0xDDE1FFC7);
const _incomingBubble = Color(0xF8FFFFFF);

class ChatScreen extends StatefulWidget {
  const ChatScreen({required this.repository, super.key});

  final TelegramRepository repository;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final searchController = TextEditingController();
  final composerController = TextEditingController();
  String? selectedChatId;
  String query = '';
  bool searchOpen = false;

  @override
  void dispose() {
    searchController.dispose();
    composerController.dispose();
    super.dispose();
  }

  Future<void> _selectChat(String chatId) async {
    setState(() => selectedChatId = chatId);
    await widget.repository.loadMessages(chatId);
  }

  void _closeConversation() {
    setState(() => selectedChatId = null);
  }

  @override
  Widget build(BuildContext context) {
    final wide = MediaQuery.sizeOf(context).width >= 860;
    final selectedDialog = widget.repository.dialogs
        .where((dialog) => dialog.id == selectedChatId)
        .firstOrNull;
    final fallbackDialog = wide
        ? selectedDialog ?? widget.repository.dialogs.firstOrNull
        : selectedDialog;

    if (!wide && selectedDialog != null) {
      return _ConversationPage(
        repository: widget.repository,
        dialog: selectedDialog,
        composerController: composerController,
        onBack: _closeConversation,
      );
    }

    if (wide) {
      return SafeArea(
        child: Row(
          children: [
            SizedBox(
              width: 390,
              child: _ChatListPage(
                repository: widget.repository,
                query: query,
                searchOpen: searchOpen,
                searchController: searchController,
                selectedChatId: fallbackDialog?.id,
                showComposeButton: false,
                onSearchToggle: _toggleSearch,
                onQueryChanged: (value) => setState(() => query = value),
                onSelect: _selectChat,
                onMenu: _showAccountSheet,
              ),
            ),
            Container(width: 1, color: Colors.black.withValues(alpha: 0.06)),
            Expanded(
              child: _ConversationPage(
                repository: widget.repository,
                dialog: fallbackDialog,
                composerController: composerController,
              ),
            ),
          ],
        ),
      );
    }

    return _ChatListPage(
      repository: widget.repository,
      query: query,
      searchOpen: searchOpen,
      searchController: searchController,
      selectedChatId: selectedChatId,
      showComposeButton: true,
      onSearchToggle: _toggleSearch,
      onQueryChanged: (value) => setState(() => query = value),
      onSelect: _selectChat,
      onMenu: _showAccountSheet,
    );
  }

  void _toggleSearch() {
    setState(() {
      searchOpen = !searchOpen;
      if (!searchOpen) {
        query = '';
        searchController.clear();
      }
    });
  }

  Future<void> _showAccountSheet() async {
    final user = widget.repository.authState.user;
    await showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return GlassPanel(
          radius: 28,
          margin: const EdgeInsets.all(12),
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: _Avatar(label: user?.displayName ?? 'TG', size: 42),
                title: Text(
                  user?.displayName ?? 'TG Third',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
                subtitle: Text(
                  user?.username == null ? '' : '@${user!.username}',
                ),
              ),
              Divider(height: 1, color: Colors.black.withValues(alpha: 0.08)),
              ListTile(
                leading: const Icon(Icons.logout_rounded),
                title: const Text('Logout'),
                onTap: () {
                  Navigator.of(context).pop();
                  widget.repository.logout();
                },
              ),
            ],
          ),
        );
      },
    );
  }
}

class _ChatListPage extends StatelessWidget {
  const _ChatListPage({
    required this.repository,
    required this.query,
    required this.searchOpen,
    required this.searchController,
    required this.selectedChatId,
    required this.showComposeButton,
    required this.onSearchToggle,
    required this.onQueryChanged,
    required this.onSelect,
    required this.onMenu,
  });

  final TelegramRepository repository;
  final String query;
  final bool searchOpen;
  final TextEditingController searchController;
  final String? selectedChatId;
  final bool showComposeButton;
  final VoidCallback onSearchToggle;
  final ValueChanged<String> onQueryChanged;
  final ValueChanged<String> onSelect;
  final VoidCallback onMenu;

  @override
  Widget build(BuildContext context) {
    final normalizedQuery = query.trim().toLowerCase();
    final dialogs = repository.dialogs
        .where(
          (dialog) =>
              normalizedQuery.isEmpty ||
              dialog.title.toLowerCase().contains(normalizedQuery),
        )
        .toList();

    return SafeArea(
      child: Stack(
        children: [
          Column(
            children: [
              _ChatListAppBar(
                searchOpen: searchOpen,
                searchController: searchController,
                onSearchToggle: onSearchToggle,
                onQueryChanged: onQueryChanged,
                onMenu: onMenu,
              ),
              Expanded(
                child: dialogs.isEmpty
                    ? const Center(child: Text('No chats'))
                    : ListView.separated(
                        padding: const EdgeInsets.only(top: 4, bottom: 96),
                        itemCount: dialogs.length,
                        separatorBuilder: (_, _) => Padding(
                          padding: const EdgeInsets.only(left: 76),
                          child: Divider(
                            height: 1,
                            color: Colors.black.withValues(alpha: 0.06),
                          ),
                        ),
                        itemBuilder: (context, index) {
                          final dialog = dialogs[index];
                          return _ChatRow(
                            dialog: dialog,
                            selected: dialog.id == selectedChatId,
                            onTap: () => onSelect(dialog.id),
                          );
                        },
                      ),
              ),
            ],
          ),
          if (showComposeButton)
            Positioned(
              right: 18,
              bottom: 22,
              child: GlassButton.custom(
                onTap: () {},
                width: 58,
                height: 58,
                shape: const LiquidOval(),
                useOwnLayer: true,
                child: const Icon(Icons.edit_rounded, color: _telegramInk),
              ),
            ),
        ],
      ),
    );
  }
}

class _ChatListAppBar extends StatelessWidget {
  const _ChatListAppBar({
    required this.searchOpen,
    required this.searchController,
    required this.onSearchToggle,
    required this.onQueryChanged,
    required this.onMenu,
  });

  final bool searchOpen;
  final TextEditingController searchController;
  final VoidCallback onSearchToggle;
  final ValueChanged<String> onQueryChanged;
  final VoidCallback onMenu;

  @override
  Widget build(BuildContext context) {
    return GlassContainer(
      height: 64,
      margin: const EdgeInsets.fromLTRB(10, 8, 10, 6),
      padding: const EdgeInsets.symmetric(horizontal: 6),
      shape: const LiquidRoundedSuperellipse(borderRadius: 20),
      useOwnLayer: true,
      settings: const LiquidGlassSettings(
        glassColor: Color(0x99FFFFFF),
        blur: 8,
        thickness: 18,
      ),
      child: Row(
        children: [
          IconButton(
            tooltip: searchOpen ? 'Close search' : 'Menu',
            icon: Icon(
              searchOpen ? Icons.arrow_back_rounded : Icons.menu_rounded,
              color: _telegramInk,
            ),
            onPressed: searchOpen ? onSearchToggle : onMenu,
          ),
          Expanded(
            child: searchOpen
                ? TextField(
                    controller: searchController,
                    autofocus: true,
                    onChanged: onQueryChanged,
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      hintText: 'Search',
                    ),
                  )
                : const Text(
                    'Telegram',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: _telegramInk,
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
          ),
          IconButton(
            tooltip: searchOpen ? 'Clear' : 'Search',
            icon: Icon(
              searchOpen ? Icons.close_rounded : Icons.search_rounded,
              color: _telegramInk,
            ),
            onPressed: searchOpen
                ? () {
                    searchController.clear();
                    onQueryChanged('');
                  }
                : onSearchToggle,
          ),
        ],
      ),
    );
  }
}

class _ChatRow extends StatelessWidget {
  const _ChatRow({
    required this.dialog,
    required this.selected,
    required this.onTap,
  });

  final ChatDialog dialog;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected
          ? _telegramBlue.withValues(alpha: 0.10)
          : Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 76,
          child: Row(
            children: [
              const SizedBox(width: 14),
              _Avatar(label: dialog.title, size: 52),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            dialog.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: _telegramInk,
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          _formatDialogTime(dialog.lastMessageAt),
                          style: const TextStyle(
                            color: _telegramMuted,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 5),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            dialog.lastMessage ?? dialog.kind.name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: _telegramMuted,
                              fontSize: 14,
                            ),
                          ),
                        ),
                        if (dialog.unreadCount > 0) ...[
                          const SizedBox(width: 8),
                          _UnreadBadge(count: dialog.unreadCount),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 14),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConversationPage extends StatelessWidget {
  const _ConversationPage({
    required this.repository,
    required this.dialog,
    required this.composerController,
    this.onBack,
  });

  final TelegramRepository repository;
  final ChatDialog? dialog;
  final TextEditingController composerController;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    final messages = dialog == null
        ? <ChatMessage>[]
        : repository.messagesFor(dialog!.id);

    return SafeArea(
      child: Column(
        children: [
          _ConversationAppBar(dialog: dialog, onBack: onBack),
          Expanded(
            child: dialog == null
                ? const Center(child: Text('Select a chat'))
                : messages.isEmpty
                ? const Center(child: Text('No messages'))
                : ListView.builder(
                    reverse: false,
                    padding: const EdgeInsets.fromLTRB(10, 12, 10, 12),
                    itemCount: messages.length,
                    itemBuilder: (context, index) {
                      final message = messages[index];
                      final previous = index == 0 ? null : messages[index - 1];
                      final showDate =
                          previous == null ||
                          !_sameDate(previous.sentAt, message.sentAt);
                      return Column(
                        children: [
                          if (showDate) _DateSeparator(date: message.sentAt),
                          _MessageBubble(message: message),
                        ],
                      );
                    },
                  ),
          ),
          _ComposerBar(
            controller: composerController,
            enabled: dialog != null,
            onSend: _send,
          ),
        ],
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

class _ConversationAppBar extends StatelessWidget {
  const _ConversationAppBar({required this.dialog, this.onBack});

  final ChatDialog? dialog;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    return GlassContainer(
      height: 64,
      margin: const EdgeInsets.fromLTRB(10, 8, 10, 6),
      padding: const EdgeInsets.symmetric(horizontal: 4),
      shape: const LiquidRoundedSuperellipse(borderRadius: 20),
      useOwnLayer: true,
      settings: const LiquidGlassSettings(
        glassColor: Color(0x99FFFFFF),
        blur: 8,
        thickness: 18,
      ),
      child: Row(
        children: [
          if (onBack != null)
            IconButton(
              tooltip: 'Back',
              icon: const Icon(Icons.arrow_back_rounded, color: _telegramInk),
              onPressed: onBack,
            )
          else
            const SizedBox(width: 8),
          if (dialog != null) ...[
            _Avatar(label: dialog!.title, size: 42),
            const SizedBox(width: 10),
          ],
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  dialog?.title ?? 'Telegram',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: _telegramInk,
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  dialog == null ? 'ready' : _dialogSubtitle(dialog!),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: _telegramMuted, fontSize: 12),
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'More',
            icon: const Icon(Icons.more_vert_rounded, color: _telegramInk),
            onPressed: () {},
          ),
        ],
      ),
    );
  }
}

class _ComposerBar extends StatelessWidget {
  const _ComposerBar({
    required this.controller,
    required this.enabled,
    required this.onSend,
  });

  final TextEditingController controller;
  final bool enabled;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 4, 8, 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: GlassContainer(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              shape: const LiquidRoundedSuperellipse(borderRadius: 22),
              useOwnLayer: true,
              settings: const LiquidGlassSettings(
                glassColor: Color(0xBFFFFFFF),
                blur: 7,
                thickness: 14,
              ),
              child: Row(
                children: [
                  IconButton(
                    tooltip: 'Attach',
                    icon: const Icon(
                      Icons.attach_file_rounded,
                      color: _telegramMuted,
                    ),
                    onPressed: enabled ? () {} : null,
                  ),
                  Expanded(
                    child: TextField(
                      controller: controller,
                      enabled: enabled,
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => onSend(),
                      decoration: InputDecoration(
                        border: InputBorder.none,
                        hintText: enabled ? 'Message' : 'Select a chat',
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Emoji',
                    icon: const Icon(
                      Icons.emoji_emotions_outlined,
                      color: _telegramMuted,
                    ),
                    onPressed: enabled ? () {} : null,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),
          GlassButton.custom(
            onTap: onSend,
            enabled: enabled,
            width: 48,
            height: 48,
            shape: const LiquidOval(),
            useOwnLayer: true,
            child: const Icon(Icons.send_rounded, color: _telegramInk),
          ),
        ],
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final bubbleColor = message.outgoing ? _outgoingBubble : _incomingBubble;
    final alignment = message.outgoing
        ? Alignment.centerRight
        : Alignment.centerLeft;
    final radius = BorderRadius.only(
      topLeft: const Radius.circular(18),
      topRight: const Radius.circular(18),
      bottomLeft: Radius.circular(message.outgoing ? 18 : 6),
      bottomRight: Radius.circular(message.outgoing ? 6 : 18),
    );

    return Align(
      alignment: alignment,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.78,
        ),
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: 3),
          padding: const EdgeInsets.fromLTRB(12, 8, 10, 5),
          decoration: BoxDecoration(
            color: bubbleColor,
            borderRadius: radius,
            border: Border.all(color: Colors.white.withValues(alpha: 0.45)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (message.authorName != null) ...[
                Text(
                  message.authorName!,
                  style: const TextStyle(
                    color: _telegramBlue,
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 3),
              ],
              Text(
                message.text,
                style: const TextStyle(
                  color: _telegramInk,
                  fontSize: 15,
                  height: 1.25,
                ),
              ),
              const SizedBox(height: 2),
              Align(
                alignment: Alignment.centerRight,
                child: Text(
                  _formatMessageTime(message.sentAt),
                  style: TextStyle(
                    color: Colors.black.withValues(alpha: 0.45),
                    fontSize: 11,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DateSeparator extends StatelessWidget {
  const _DateSeparator({required this.date});

  final DateTime date;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: GlassContainer(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        shape: const LiquidRoundedSuperellipse(borderRadius: 14),
        useOwnLayer: true,
        settings: const LiquidGlassSettings(
          glassColor: Color(0x99FFFFFF),
          blur: 7,
          thickness: 12,
        ),
        child: Text(
          _formatDate(date),
          style: const TextStyle(
            color: _telegramMuted,
            fontSize: 12,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.label, required this.size});

  final String label;
  final double size;

  @override
  Widget build(BuildContext context) {
    final initial = label.trim().isEmpty
        ? 'T'
        : label.trim().characters.first.toUpperCase();

    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: const BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF65C7F7), Color(0xFF2AABEE)],
        ),
      ),
      child: Text(
        initial,
        style: TextStyle(
          color: Colors.white,
          fontSize: size * 0.42,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _UnreadBadge extends StatelessWidget {
  const _UnreadBadge({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 22,
      constraints: const BoxConstraints(minWidth: 22),
      padding: const EdgeInsets.symmetric(horizontal: 7),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: _telegramBlue,
        borderRadius: BorderRadius.circular(99),
      ),
      child: Text(
        count > 99 ? '99+' : '$count',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 12,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

String _dialogSubtitle(ChatDialog dialog) {
  return switch (dialog.kind) {
    DialogKind.user => 'last seen recently',
    DialogKind.group => 'group',
    DialogKind.channel => 'channel',
    DialogKind.unknown => 'chat',
  };
}

String _formatDialogTime(DateTime? value) {
  if (value == null) return '';
  final now = DateTime.now();
  if (_sameDate(now, value)) return _formatMessageTime(value);
  return '${value.day}/${value.month}';
}

String _formatMessageTime(DateTime value) {
  final hour = value.hour.toString().padLeft(2, '0');
  final minute = value.minute.toString().padLeft(2, '0');
  return '$hour:$minute';
}

String _formatDate(DateTime value) {
  final now = DateTime.now();
  if (_sameDate(now, value)) return 'Today';
  final yesterday = now.subtract(const Duration(days: 1));
  if (_sameDate(yesterday, value)) return 'Yesterday';
  return '${value.day}/${value.month}/${value.year}';
}

bool _sameDate(DateTime a, DateTime b) {
  return a.year == b.year && a.month == b.month && a.day == b.day;
}

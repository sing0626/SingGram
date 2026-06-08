package com.sing.tgthird.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sing.tgthird.core.model.ChatDialog
import com.sing.tgthird.core.model.Message
import com.sing.tgthird.core.model.TelegramUser
import com.sing.tgthird.core.repository.TelegramRepository
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    repository: TelegramRepository,
    user: TelegramUser,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dialogs by repository.dialogs.collectAsState()
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 720.dp
        val selectedDialog = dialogs.firstOrNull { it.id == selectedChatId }
            ?: if (compact) null else dialogs.firstOrNull()
        val messages by repository.messagesFor(selectedDialog?.id).collectAsState(initial = emptyList())

        LaunchedEffect(dialogs, compact) {
            if (!compact && selectedChatId == null && dialogs.isNotEmpty()) {
                selectedChatId = dialogs.first().id
            }
        }

        if (compact) {
            if (selectedDialog == null) {
                DialogColumn(
                    dialogs = dialogs,
                    selectedChatId = selectedChatId,
                    user = user,
                    onDialogClick = { selectedChatId = it },
                    onLogout = {
                        scope.launch {
                            repository.logout()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ConversationColumn(
                    dialog = selectedDialog,
                    messages = messages,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSend = {
                        val text = draft.trim()
                        if (text.isEmpty()) return@ConversationColumn
                        draft = ""
                        scope.launch {
                            repository.sendText(chatId = selectedDialog.id, text = text)
                        }
                    },
                    onBack = { selectedChatId = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                DialogColumn(
                    dialogs = dialogs,
                    selectedChatId = selectedDialog?.id,
                    user = user,
                    onDialogClick = { selectedChatId = it },
                    onLogout = {
                        scope.launch {
                            repository.logout()
                        }
                    },
                    modifier = Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                )

                ConversationColumn(
                    dialog = selectedDialog,
                    messages = messages,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSend = {
                        val chatId = selectedDialog?.id ?: return@ConversationColumn
                        val text = draft.trim()
                        if (text.isEmpty()) return@ConversationColumn
                        draft = ""
                        scope.launch {
                            repository.sendText(chatId = chatId, text = text)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DialogColumn(
    dialogs: List<ChatDialog>,
    selectedChatId: String?,
    user: TelegramUser,
    onDialogClick: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Avatar(text = user.displayName.take(1).uppercase())
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("@${user.username ?: "me"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onLogout, shape = RoundedCornerShape(8.dp)) {
                Text("Logout")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(dialogs, key = { it.id }) { dialog ->
                DialogRow(
                    dialog = dialog,
                    selected = dialog.id == selectedChatId,
                    onClick = { onDialogClick(dialog.id) }
                )
            }
        }
    }
}

@Composable
private fun DialogRow(
    dialog: ChatDialog,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface

    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(text = dialog.title.take(1).uppercase())
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dialog.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    dialog.lastMessage ?: dialog.kind.name.lowercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (dialog.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dialog.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationColumn(
    dialog: ChatDialog?,
    messages: List<Message>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (onBack != null) {
                Button(onClick = onBack, shape = RoundedCornerShape(8.dp)) {
                    Text("Chats")
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(dialog?.title ?: "Dialogs", style = MaterialTheme.typography.titleLarge)
                Text(dialog?.kind?.name?.lowercase() ?: "ready", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.55f))

        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text("No messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.55f))
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = dialog != null,
                placeholder = { Text("Message") },
                singleLine = true,
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onSend,
                enabled = dialog != null && draft.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    Row(
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (message.outgoing) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.authorName.isNullOrBlank()) {
                    Text(
                        message.authorName.orEmpty(),
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(message.text.ifBlank { "[unsupported message]" })
            }
        }
    }
}

@Composable
private fun Avatar(text: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

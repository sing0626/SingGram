package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SingGramBotAuth;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** Lightweight compose flow for messages sent through the active Bot account. */
public class SingGramBotComposeActivity extends BaseFragment {

    private EditTextBoldCursor recipientField;
    private EditTextBoldCursor messageField;
    private TextView sendButton;
    private boolean resolving;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramBotCompose));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !resolving) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addHero(context, container);
        addHeader(context, container, LocaleController.getString(R.string.SingGramBotComposeRecipient));
        LinearLayout recipientSection = addSection(context, container);
        recipientField = createRecipientField(context);
        recipientSection.addView(recipientField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 16, 0, 16, 0));

        addHeader(context, container, LocaleController.getString(R.string.SingGramBotComposeMessage));
        LinearLayout messageSection = addSection(context, container);
        messageField = createMessageField(context);
        messageSection.addView(messageField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 132, 16, 0, 16, 0));
        addDivider(context, messageSection);
        sendButton = createSendButton(context);
        messageSection.addView(sendButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 16, 8, 16, 12));

        addInfo(context, container, LocaleController.getString(R.string.SingGramBotDeliveryInfo));
        updateSendButton();
        return fragmentView;
    }

    private void addHero(Context context, LinearLayout container) {
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        hero.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView title = createText(context, LocaleController.getString(R.string.SingGramBotCompose), Theme.key_windowBackgroundWhiteBlackText, 20, true);
        hero.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = createText(context, LocaleController.getString(R.string.SingGramBotComposeInfo), Theme.key_windowBackgroundWhiteGrayText2, 14, false);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        hero.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private EditTextBoldCursor createRecipientField(Context context) {
        EditTextBoldCursor field = createField(context, LocaleController.getString(R.string.SingGramBotRecipientHint));
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setContentDescription(LocaleController.getString(R.string.SingGramBotComposeRecipient));
        field.addTextChangedListener(textWatcher());
        return field;
    }

    private EditTextBoldCursor createMessageField(Context context) {
        EditTextBoldCursor field = createField(context, LocaleController.getString(R.string.SingGramBotComposeMessageHint));
        field.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        field.setSingleLine(false);
        field.setMinLines(4);
        field.setMaxLines(7);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setContentDescription(LocaleController.getString(R.string.SingGramBotComposeMessage));
        field.addTextChangedListener(textWatcher());
        return field;
    }

    private EditTextBoldCursor createField(Context context, String hint) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setHint(hint);
        field.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(6), AndroidUtilities.dp(4), AndroidUtilities.dp(6));
        field.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        return field;
    }

    private TextView createSendButton(Context context) {
        TextView button = new TextView(context);
        button.setText(LocaleController.getString(R.string.SingGramBotComposeSend));
        button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), accent, Theme.multAlpha(accent, 0.82f)));
        button.setContentDescription(LocaleController.getString(R.string.SingGramBotComposeSend));
        button.setOnClickListener(v -> sendMessage());
        return button;
    }

    private TextWatcher textWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private void updateSendButton() {
        if (sendButton == null) {
            return;
        }
        boolean valid = !resolving && recipientField != null && messageField != null
                && isValidUsername(normalizeUsername(recipientField.getText().toString()))
                && !TextUtils.isEmpty(messageField.getText().toString().trim());
        sendButton.setEnabled(valid);
        sendButton.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void sendMessage() {
        if (resolving || recipientField == null || messageField == null) {
            return;
        }
        if (!SingGramBotAuth.isBotAccount(currentAccount)) {
            showToast(LocaleController.getString(R.string.SingGramBotWorkspaceUnavailable));
            return;
        }
        String username = normalizeUsername(recipientField.getText().toString());
        String message = messageField.getText().toString().trim();
        if (!isValidUsername(username)) {
            recipientField.setErrorText(LocaleController.getString(R.string.SingGramBotRecipientInvalid));
            return;
        }
        if (TextUtils.isEmpty(message)) {
            messageField.setErrorText(LocaleController.getString(R.string.SingGramBotMessageRequired));
            return;
        }
        recipientField.setErrorText(null);
        messageField.setErrorText(null);
        resolving = true;
        updateSendButton();

        TLRPC.TL_contacts_resolveUsername request = new TLRPC.TL_contacts_resolveUsername();
        request.username = username;
        final String outgoingMessage = message;
        ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            resolving = false;
            updateSendButton();
            if (error != null || !(response instanceof TLRPC.TL_contacts_resolvedPeer)) {
                String reason = error != null && !TextUtils.isEmpty(error.text) ? error.text : LocaleController.getString(R.string.SingGramBotResolveFailed);
                recipientField.setErrorText(reason);
                return;
            }
            TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response;
            long dialogId = DialogObject.getPeerDialogId(resolvedPeer.peer);
            if (dialogId == 0) {
                recipientField.setErrorText(LocaleController.getString(R.string.SingGramBotResolveFailed));
                return;
            }
            MessagesController controller = MessagesController.getInstance(currentAccount);
            controller.putUsers(resolvedPeer.users, false);
            controller.putChats(resolvedPeer.chats, false);
            MessagesStorage.getInstance(currentAccount).putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, true, true);
            SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(outgoingMessage, dialogId));
            messageField.setText("");
            showToast(LocaleController.getString(R.string.SingGramBotMessageQueued));
            openConversation(dialogId);
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    private void openConversation(long dialogId) {
        Bundle args = new Bundle();
        if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        if (getMessagesController().checkCanOpenChat(args, this)) {
            presentFragment(new ChatActivity(args));
        }
    }

    private static String normalizeUsername(String value) {
        String username = value == null ? "" : value.trim();
        return username.startsWith("@") ? username.substring(1) : username;
    }

    private static boolean isValidUsername(String username) {
        if (username.length() < 5 || username.length() > 32) {
            return false;
        }
        for (int index = 0; index < username.length(); index++) {
            char character = username.charAt(index);
            if (!(character == '_' || character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z' || character >= '0' && character <= '9')) {
                return false;
            }
        }
        return true;
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addHeader(Context context, LinearLayout container, String text) {
        TextView textView = createText(context, text, Theme.key_windowBackgroundWhiteBlueHeader, 13, true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(18), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 0));
    }

    private void addInfo(Context context, LinearLayout container, String text) {
        TextView textView = createText(context, text, Theme.key_windowBackgroundWhiteGrayText4, 13, false);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private TextView createText(Context context, String text, int colorKey, int size, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(colorKey));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setIncludeFontPadding(false);
        if (bold) {
            textView.setTypeface(AndroidUtilities.bold());
        }
        return textView;
    }

    private void showToast(String text) {
        Context context = getParentActivity() != null ? getParentActivity() : getContext();
        if (context != null) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> descriptions = new ArrayList<>();
        descriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return descriptions;
    }
}

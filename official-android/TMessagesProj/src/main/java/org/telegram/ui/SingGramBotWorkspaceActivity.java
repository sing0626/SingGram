package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramBotAuth;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** A useful first screen for native MTProto Bot accounts instead of an empty chat tab. */
public class SingGramBotWorkspaceActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private LinearLayout contentContainer;
    private boolean refreshing;

    @Override
    public boolean onFragmentCreate() {
        if (!super.onFragmentCreate() || !SingGramBotAuth.isBotAccount(currentAccount)) {
            return false;
        }
        getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().addObserver(this, NotificationCenter.mainUserInfoChanged);
        // The Bot workspace has no server-side dialogs endpoint. Restore conversations that
        // were already cached locally before starting the update-based inbox sync.
        getMessagesController().loadDialogs(0, 0, 100, true);
        getMessagesController().loadBotUpdates();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
        getNotificationCenter().removeObserver(this, NotificationCenter.mainUserInfoChanged);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramBotWorkspace));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(contentContainer, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        buildContent(context);
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (account != currentAccount || contentContainer == null) {
            return;
        }
        if (id == NotificationCenter.dialogsNeedReload) {
            refreshing = false;
            buildContent(getParentActivity());
        } else if (id == NotificationCenter.mainUserInfoChanged) {
            buildContent(getParentActivity());
        }
    }

    private void buildContent(Context context) {
        if (context == null || contentContainer == null) {
            return;
        }
        contentContainer.removeAllViews();

        addHero(context, contentContainer);

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramBotWorkspaceActions));
        LinearLayout actions = addSection(context, contentContainer);
        addActionRow(context, actions, R.drawable.msg_edit, LocaleController.getString(R.string.SingGramBotCompose), LocaleController.getString(R.string.SingGramBotComposeInfo), v -> presentFragment(new SingGramBotComposeActivity()));
        addDivider(context, actions);
        addActionRow(context, actions, R.drawable.msg_openin, LocaleController.getString(R.string.SingGramBotWorkspaceInbox), LocaleController.getString(R.string.SingGramBotWorkspaceInboxInfo), v -> openInbox());
        addDivider(context, actions);
        addActionRow(context, actions, R.drawable.msg_retry, refreshing ? LocaleController.getString(R.string.SingGramBotWorkspaceRefreshing) : LocaleController.getString(R.string.SingGramBotWorkspaceRefresh), LocaleController.getString(R.string.SingGramBotWorkspaceRefreshInfo), v -> refreshInbox());

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramBotWorkspaceRecent));
        LinearLayout conversations = addSection(context, contentContainer);
        int conversationCount = addConversationRows(context, conversations);
        if (conversationCount == 0) {
            addEmptyState(context, conversations);
        }

        addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramBotDeliveryInfo));
    }

    private void addHero(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        String name = user == null ? LocaleController.getString(R.string.SingGramLoginBotAccount) : UserObject.getUserName(user);
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(user);
        }
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(28));
        avatar.getImageReceiver().setCurrentAccount(currentAccount);
        if (user != null) {
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        card.addView(avatar, LayoutHelper.createLinear(56, 56, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView label = createText(context, LocaleController.getString(R.string.SingGramBotWorkspaceAccount), Theme.key_windowBackgroundWhiteBlueText, 13, true);
        textContainer.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = createText(context, name, Theme.key_windowBackgroundWhiteBlackText, 20, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        textContainer.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        String username = user != null && !TextUtils.isEmpty(user.username) ? "@" + user.username : LocaleController.getString(R.string.SingGramLoginBotAccount);
        TextView usernameView = createText(context, username, Theme.key_windowBackgroundWhiteGrayText2, 14, false);
        usernameView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        textContainer.addView(usernameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int dialogCount = getConversationCount();
        int unread = MessagesStorage.getInstance(currentAccount).getMainUnreadCount();
        TextView summary = createText(context, LocaleController.formatString(R.string.SingGramBotWorkspaceSummary, dialogCount, unread), Theme.key_windowBackgroundWhiteGrayText4, 13, false);
        summary.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        textContainer.addView(summary, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private int addConversationRows(Context context, LinearLayout container) {
        ArrayList<TLRPC.Dialog> dialogs = getWorkspaceDialogs();
        int added = 0;
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog == null || DialogObject.isFolderDialogId(dialog.id) || DialogObject.isEncryptedDialog(dialog.id)) {
                continue;
            }
            String name = getDialogTitle(dialog.id);
            if (added > 0) {
                addDivider(context, container);
            }
            final long dialogId = dialog.id;
            String subtitle = dialog.unread_count > 0
                    ? LocaleController.formatString(R.string.SingGramBotWorkspaceUnread, dialog.unread_count)
                    : LocaleController.getString(R.string.SingGramBotWorkspaceConversationReady);
            addActionRow(context, container, R.drawable.msg_openin, name, subtitle, v -> openConversation(dialogId));
            added++;
            if (added >= 8) {
                break;
            }
        }
        return added;
    }

    private int getConversationCount() {
        int count = 0;
        for (TLRPC.Dialog dialog : getWorkspaceDialogs()) {
            if (dialog != null && !DialogObject.isFolderDialogId(dialog.id) && !DialogObject.isEncryptedDialog(dialog.id)) {
                count++;
            }
        }
        return count;
    }

    private ArrayList<TLRPC.Dialog> getWorkspaceDialogs() {
        ArrayList<TLRPC.Dialog> dialogs = null;
        if (SingGramBotAuth.isBotAccount(currentAccount)) {
            ArrayList<TLRPC.Dialog> allDialogs = getMessagesController().getAllDialogs();
            if (allDialogs != null && !allDialogs.isEmpty()) {
                dialogs = new ArrayList<>();
                for (TLRPC.Dialog dialog : allDialogs) {
                    if (dialog != null && dialog.folder_id == 0 && !DialogObject.isFolderDialogId(dialog.id) && !DialogObject.isEncryptedDialog(dialog.id)) {
                        DialogObject.ensureDialogPeer(dialog);
                        dialogs.add(dialog);
                    }
                }
            }
        }
        if (dialogs == null || dialogs.isEmpty()) {
            dialogs = getMessagesController().getDialogs(0);
        }
        if (dialogs == null || dialogs.isEmpty()) {
            // Updates can create a dialog in allDialogs before the folder index is rebuilt.
            dialogs = getMessagesController().getAllDialogs();
        }
        return dialogs == null ? new ArrayList<>() : new ArrayList<>(dialogs);
    }

    private String getDialogTitle(long dialogId) {
        String name = DialogObject.getName(currentAccount, dialogId);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        // A short bot update may only contain an id. Keep the row actionable until the peer
        // details arrive in a later update instead of hiding the conversation altogether.
        return dialogId > 0 ? "User " + dialogId : "Chat " + Math.abs(dialogId);
    }

    private void addEmptyState(Context context, LinearLayout container) {
        LinearLayout empty = new LinearLayout(context);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        container.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = createText(context, LocaleController.getString(R.string.SingGramBotWorkspaceEmpty), Theme.key_windowBackgroundWhiteBlackText, 16, true);
        empty.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = createText(context, LocaleController.getString(R.string.SingGramBotWorkspaceEmptyInfo), Theme.key_windowBackgroundWhiteGrayText2, 14, false);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        empty.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void refreshInbox() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        buildContent(getParentActivity());
        getMessagesController().loadDialogs(0, 0, 100, true);
        // Older builds may have advanced a Bot update cursor before they could persist a
        // conversation. Retry retained updates while leaving all existing cache intact.
        getMessagesController().forceBotInboxResync();
    }

    private void openInbox() {
        if (getParentLayout() == null) {
            return;
        }
        ArrayList<BaseFragment> stack = new ArrayList<>(getParentLayout().getFragmentStack());
        MainTabsActivity mainTabs = null;
        int mainTabsIndex = -1;
        for (int index = stack.size() - 1; index >= 0; index--) {
            if (stack.get(index) instanceof MainTabsActivity) {
                mainTabs = (MainTabsActivity) stack.get(index);
                mainTabsIndex = index;
                break;
            }
        }
        if (mainTabs == null) {
            finishFragment();
            return;
        }
        for (int index = stack.size() - 1; index > mainTabsIndex; index--) {
            BaseFragment fragment = stack.get(index);
            if (fragment != this) {
                getParentLayout().removeFragmentFromStack(fragment, true);
            }
        }
        mainTabs.openBotInbox();
        finishFragment();
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

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addActionRow(Context context, LinearLayout container, int iconResource, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AndroidUtilities.dp(68));
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        row.setContentDescription(title + ". " + subtitle);
        row.setOnClickListener(listener);
        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconResource);
        icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        row.addView(icon, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        row.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView titleView = createText(context, title, Theme.key_windowBackgroundWhiteBlackText, 16, false);
        titleView.setMaxLines(2);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitleView = createText(context, subtitle, Theme.key_windowBackgroundWhiteGrayText2, 13, false);
        subtitleView.setMaxLines(2);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        textContainer.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> descriptions = new ArrayList<>();
        descriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return descriptions;
    }
}

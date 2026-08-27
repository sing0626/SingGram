package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
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

/** Central account management for personal Telegram accounts and native Bot accounts. */
public class SingGramUserCenterActivity extends BaseFragment {

    private LinearLayout contentContainer;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramUserCenter));
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
    public void onResume() {
        super.onResume();
        buildContent(getParentActivity());
    }

    private void buildContent(Context context) {
        if (context == null || contentContainer == null) {
            return;
        }
        contentContainer.removeAllViews();

        addCurrentAccountHero(context, contentContainer);

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUserCenterAccounts));
        LinearLayout accounts = addSection(context, contentContainer);
        boolean added = false;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                continue;
            }
            MessagesController.getInstance(account).loadDialogs(0, 0, 30, true);
            if (added) {
                addDivider(context, accounts);
            }
            addAccountRow(context, accounts, account);
            added = true;
        }
        if (!added) {
            addEmptyAccountState(context, accounts);
        }

        if (SingGramBotAuth.isBotAccount(UserConfig.selectedAccount)) {
            addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramBotWorkspace));
            LinearLayout botTools = addSection(context, contentContainer);
            addActionRow(context, botTools, R.drawable.msg_edit, LocaleController.getString(R.string.SingGramBotWorkspace), LocaleController.getString(R.string.SingGramBotWorkspaceInfo), true, v -> presentFragment(new SingGramBotWorkspaceActivity()));
        }

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUserCenterAddAccount));
        LinearLayout addAccount = addSection(context, contentContainer);
        boolean hasSlot = SingGramBotAuth.findFreeAccount() >= 0;
        addActionRow(context, addAccount, R.drawable.settings_account, LocaleController.getString(R.string.SingGramLoginPersonalAccount), LocaleController.getString(R.string.SingGramLoginPersonalAccountInfo), hasSlot, v -> SingGramLoginChoiceActivity.openPersonalLogin(this));
        addDivider(context, addAccount);
        addActionRow(context, addAccount, R.drawable.msg_addbot, LocaleController.getString(R.string.SingGramLoginBotAccount), LocaleController.getString(R.string.SingGramLoginBotAccountInfo), hasSlot, v -> presentFragment(new SingGramBotLoginActivity()));
        if (!hasSlot) {
            addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramAccountSlotsFull));
        }

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUserCenterManage));
        LinearLayout manage = addSection(context, contentContainer);
        addActionRow(context, manage, R.drawable.settings_account, LocaleController.getString(R.string.SingGramAccountProfiles), LocaleController.getString(R.string.SingGramAccountProfilesInfo), true, v -> presentFragment(new SingGramAccountProfilesActivity()));
        addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramUserCenterInfo));
    }

    private void addCurrentAccountHero(Context context, LinearLayout container) {
        int account = UserConfig.selectedAccount;
        LinearLayout hero = new LinearLayout(context);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        hero.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(user);
        }
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(28));
        avatar.getImageReceiver().setCurrentAccount(account);
        if (user != null) {
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        hero.addView(avatar, LayoutHelper.createLinear(56, 56, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        hero.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView label = createText(context, LocaleController.getString(R.string.SingGramUserCenterCurrent), Theme.key_windowBackgroundWhiteBlueText, 13, true);
        textContainer.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView name = createText(context, accountName(account), Theme.key_windowBackgroundWhiteBlackText, 20, true);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        textContainer.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView accountType = createText(context, typeAndUnreadSummary(account), Theme.key_windowBackgroundWhiteGrayText2, 14, false);
        accountType.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        textContainer.addView(accountType, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView accountSlots = createText(context, LocaleController.formatString(R.string.SingGramAccountSlots, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT), Theme.key_windowBackgroundWhiteGrayText4, 13, false);
        accountSlots.setPadding(0, AndroidUtilities.dp(5), 0, 0);
        textContainer.addView(accountSlots, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addAccountRow(Context context, LinearLayout container, int account) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AndroidUtilities.dp(76));
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        row.setContentDescription(accountName(account) + ". " + typeAndUnreadSummary(account));
        row.setOnClickListener(v -> openAccount(account));
        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (user != null) {
            avatarDrawable.setInfo(user);
        }
        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(23));
        avatar.getImageReceiver().setCurrentAccount(account);
        if (user != null) {
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        row.addView(avatar, LayoutHelper.createLinear(46, 46, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        row.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL));

        TextView title = createText(context, accountName(account), Theme.key_windowBackgroundWhiteBlackText, 16, false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        textContainer.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = createText(context, typeAndUnreadSummary(account), Theme.key_windowBackgroundWhiteGrayText2, 13, false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        textContainer.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (account == UserConfig.selectedAccount) {
            TextView current = createText(context, LocaleController.getString(R.string.SingGramUserCenterCurrent), Theme.key_windowBackgroundWhiteBlueText, 12, true);
            current.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT));
            current.setMaxLines(2);
            current.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(current, LayoutHelper.createLinear(70, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        } else {
            ImageView arrow = new ImageView(context);
            arrow.setImageResource(R.drawable.msg_arrowright);
            arrow.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4), PorterDuff.Mode.SRC_IN);
            row.addView(arrow, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        }
    }

    private void addActionRow(Context context, LinearLayout container, int iconResource, String title, String subtitle, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AndroidUtilities.dp(68));
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1.0f : 0.5f);
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        row.setContentDescription(title + ". " + subtitle);
        if (enabled) {
            row.setOnClickListener(listener);
        }
        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconResource);
        icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), PorterDuff.Mode.SRC_IN);
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

    private void addEmptyAccountState(Context context, LinearLayout container) {
        TextView empty = createText(context, LocaleController.getString(R.string.SingGramUserCenterNoAccounts), Theme.key_windowBackgroundWhiteGrayText2, 15, false);
        empty.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        container.addView(empty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void openAccount(int account) {
        if (account == UserConfig.selectedAccount) {
            if (SingGramBotAuth.isBotAccount(account)) {
                presentFragment(new SingGramBotWorkspaceActivity());
            }
            return;
        }
        if (LaunchActivity.instance == null) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAccountOverviewSwitchUnavailable), Toast.LENGTH_SHORT).show();
            return;
        }
        LaunchActivity.instance.switchToAccount(account, true);
    }

    private String accountName(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        return user == null
                ? LocaleController.formatString(R.string.SingGramAccountOverviewAccountFallback, account + 1)
                : UserObject.getUserName(user);
    }

    private String typeAndUnreadSummary(int account) {
        String type = SingGramBotAuth.isBotAccount(account)
                ? LocaleController.getString(R.string.SingGramLoginBotAccount)
                : LocaleController.getString(R.string.SingGramLoginPersonalAccount);
        int unread = MessagesStorage.getInstance(account).getMainUnreadCount();
        return LocaleController.formatString(R.string.SingGramUserCenterAccountSummary, type, account == UserConfig.selectedAccount ? LocaleController.getString(R.string.SingGramUserCenterCurrent) : LocaleController.getString(R.string.SingGramUserCenterSignedIn), unread);
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
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

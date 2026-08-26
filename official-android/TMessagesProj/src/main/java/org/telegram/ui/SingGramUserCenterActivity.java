package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
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
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** Central account management surface for personal Telegram accounts and Bot accounts. */
public class SingGramUserCenterActivity extends BaseFragment {

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

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addSummary(context, container);
        addHeader(context, container, LocaleController.getString(R.string.SingGramUserCenterAccounts));
        LinearLayout accounts = addSection(context, container);
        boolean added = false;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                continue;
            }
            MessagesController.getInstance(account).loadDialogs(0, 0, 30, true);
            if (added) {
                addDivider(context, accounts);
            }
            addAccountCell(context, accounts, account);
            added = true;
        }
        if (!added) {
            addInfoCell(context, accounts, LocaleController.getString(R.string.SingGramUserCenterNoAccounts), "");
        }

        addHeader(context, container, LocaleController.getString(R.string.SingGramUserCenterAddAccount));
        LinearLayout addAccount = addSection(context, container);
        boolean hasSlot = SingGramBotAuth.findFreeAccount() >= 0;
        addActionCell(context, addAccount, LocaleController.getString(R.string.SingGramLoginPersonalAccount), LocaleController.getString(R.string.SingGramLoginPersonalAccountInfo), hasSlot, v -> SingGramLoginChoiceActivity.openPersonalLogin(this));
        addDivider(context, addAccount);
        addActionCell(context, addAccount, LocaleController.getString(R.string.SingGramLoginBotAccount), LocaleController.getString(R.string.SingGramLoginBotAccountInfo), hasSlot, v -> presentFragment(new SingGramBotLoginActivity()));
        if (!hasSlot) {
            addInfo(context, container, LocaleController.getString(R.string.SingGramAccountSlotsFull));
        }

        addHeader(context, container, LocaleController.getString(R.string.SingGramUserCenterManage));
        LinearLayout manage = addSection(context, container);
        addActionCell(context, manage, LocaleController.getString(R.string.SingGramAccountProfiles), LocaleController.getString(R.string.SingGramAccountProfilesInfo), true, v -> presentFragment(new SingGramAccountProfilesActivity()));
        addInfo(context, container, LocaleController.getString(R.string.SingGramUserCenterInfo));
        return fragmentView;
    }

    private void addSummary(Context context, LinearLayout container) {
        LinearLayout summary = new LinearLayout(context);
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        summary.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(summary, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString(R.string.SingGramUserCenter));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setIncludeFontPadding(false);
        summary.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView account = new TextView(context);
        account.setText(currentAccountSummary());
        account.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        account.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        account.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        account.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        summary.addView(account, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private String currentAccountSummary() {
        int account = UserConfig.selectedAccount;
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return LocaleController.formatString(R.string.SingGramAccountSlots, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT);
        }
        return LocaleController.formatString(R.string.SingGramUserCenterCurrentSummary, accountTitle(account), accountType(account));
    }

    private void addAccountCell(Context context, LinearLayout container, int account) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(accountTitle(account), accountSummary(account), true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> showAccountDetails(account));
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addActionCell(Context context, LinearLayout container, String title, String value, boolean enabled, View.OnClickListener listener) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(title, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.5f);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        if (enabled) {
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void showAccountDetails(int account) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(accountTitle(account));
        builder.setMessage(accountDetails(account));
        if (account != UserConfig.selectedAccount) {
            builder.setPositiveButton(LocaleController.getString(R.string.SingGramAccountOverviewSwitch), (dialog, which) -> switchToAccount(account));
        } else {
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void switchToAccount(int account) {
        if (LaunchActivity.instance == null) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramAccountOverviewSwitchUnavailable), Toast.LENGTH_SHORT).show();
            return;
        }
        LaunchActivity.instance.switchToAccount(account, true);
        finishFragment();
    }

    private String accountTitle(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        String name = user == null ? LocaleController.formatString(R.string.SingGramAccountOverviewAccountFallback, account + 1) : UserObject.getUserName(user);
        return LocaleController.formatString(R.string.SingGramAccountProfileAccount, account + 1, name);
    }

    private String accountType(int account) {
        return SingGramBotAuth.isBotAccount(account)
                ? LocaleController.getString(R.string.SingGramLoginBotAccount)
                : LocaleController.getString(R.string.SingGramLoginPersonalAccount);
    }

    private String accountSummary(int account) {
        int unread = MessagesStorage.getInstance(account).getMainUnreadCount();
        String state = account == UserConfig.selectedAccount
                ? LocaleController.getString(R.string.SingGramUserCenterCurrent)
                : LocaleController.getString(R.string.SingGramUserCenterSignedIn);
        return LocaleController.formatString(R.string.SingGramUserCenterAccountSummary, accountType(account), state, unread);
    }

    private String accountDetails(int account) {
        int unread = MessagesStorage.getInstance(account).getMainUnreadCount();
        String profile = profileSummary(account);
        String state = account == UserConfig.selectedAccount
                ? LocaleController.getString(R.string.SingGramUserCenterCurrent)
                : LocaleController.getString(R.string.SingGramUserCenterSignedIn);
        return LocaleController.formatString(R.string.SingGramUserCenterAccountDetails, accountType(account), state, unread, profile);
    }

    private String profileSummary(int account) {
        String label = SingGramConfig.getAccountProfileLabel(account);
        String group = SingGramConfig.getAccountProfileGroup(account);
        int color = SingGramConfig.getAccountProfileColor(account);
        if (TextUtils.isEmpty(label) && TextUtils.isEmpty(group) && color == 0) {
            return LocaleController.getString(R.string.SingGramAccountProfileUnset);
        }
        return LocaleController.formatString(R.string.SingGramAccountProfileSummary, TextUtils.isEmpty(label) ? "-" : label, TextUtils.isEmpty(group) ? "-" : group, color);
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addHeader(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(18), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfoCell(Context context, LinearLayout container, String text, String value) {
        TextView textView = new TextView(context);
        textView.setText(TextUtils.isEmpty(value) ? text : text + "\n" + value);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 16, 0, 16, 0));
    }

    private void addInfo(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
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

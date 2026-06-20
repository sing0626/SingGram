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

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
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

public class SingGramAccountOverviewActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramAccountOverview));
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
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addHeader(context, container, LocaleController.getString(R.string.SingGramAccountOverviewActive));
        LinearLayout listSection = addSection(context, container);
        boolean added = false;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!AccountInstance.getInstance(account).getUserConfig().isClientActivated()) {
                continue;
            }
            MessagesController.getInstance(account).loadDialogs(0, 0, 30, true);
            if (added) {
                addDivider(context, listSection);
            }
            addAccountCell(context, listSection, account);
            added = true;
        }
        if (!added) {
            addInfoCell(context, listSection, LocaleController.getString(R.string.SingGramAccountOverviewEmpty), "");
        }
        addInfo(context, container, LocaleController.getString(R.string.SingGramAccountOverviewFootnote));
        return fragmentView;
    }

    private void addAccountCell(Context context, LinearLayout container, int account) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(accountTitle(account), accountSummary(account), true, false);
        cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        cell.setOnClickListener(v -> showAccountDetails(account));
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void showAccountDetails(int account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(accountTitle(account));
        builder.setMessage(accountDetails(account));
        builder.setPositiveButton(LocaleController.getString(R.string.SingGramAccountOverviewSwitch), (dialog, which) -> switchToAccount(account));
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

    private String accountSummary(int account) {
        int unread = MessagesStorage.getInstance(account).getMainUnreadCount();
        String profile = profileSummary(account);
        String recent = recentDialogsSummary(account, 2);
        return LocaleController.formatString(R.string.SingGramAccountOverviewSummary, unread, profile, recent);
    }

    private String accountDetails(int account) {
        int unread = MessagesStorage.getInstance(account).getMainUnreadCount();
        StringBuilder builder = new StringBuilder();
        builder.append(LocaleController.formatString(R.string.SingGramAccountOverviewUnread, unread)).append('\n');
        builder.append(profileSummary(account)).append("\n\n");
        builder.append(LocaleController.getString(R.string.SingGramAccountOverviewRecentChats)).append('\n');
        builder.append(recentDialogsSummary(account, 5));
        return builder.toString();
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

    private String recentDialogsSummary(int account, int limit) {
        ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(account).getDialogs(0);
        if (dialogs.isEmpty()) {
            return LocaleController.getString(R.string.SingGramAccountOverviewRecentEmpty);
        }
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog == null || DialogObject.isFolderDialogId(dialog.id)) {
                continue;
            }
            String name = DialogObject.getName(account, dialog.id);
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("* ").append(name);
            if (dialog.unread_count > 0) {
                builder.append(" - ").append(LocaleController.formatString(R.string.SingGramAccountOverviewUnreadShort, dialog.unread_count));
            }
            added++;
            if (added >= limit) {
                break;
            }
        }
        return builder.length() == 0 ? LocaleController.getString(R.string.SingGramAccountOverviewRecentEmpty) : builder.toString();
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
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setIncludeFontPadding(false);
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
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}

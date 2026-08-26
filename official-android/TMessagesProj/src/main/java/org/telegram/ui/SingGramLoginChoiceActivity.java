package org.telegram.ui;

import android.content.Context;
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
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramBotAuth;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/** Lets people choose an account type before entering either native login flow. */
public class SingGramLoginChoiceActivity extends BaseFragment {

    public static void openPersonalLogin(BaseFragment fragment) {
        int account = SingGramBotAuth.findFreeAccount();
        if (account < 0) {
            Context context = fragment.getParentActivity();
            if (context != null) {
                Toast.makeText(context, LocaleController.getString(R.string.SingGramAccountSlotsFull), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (UserConfig.getActivatedAccountsCount() == 0) {
            fragment.presentFragment(new LoginActivity());
        } else {
            fragment.presentFragment(new LoginActivity(account));
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramLoginTitle));
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
        container.setPadding(0, AndroidUtilities.dp(28), 0, AndroidUtilities.dp(28));
        scrollView.addView(container, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        addIntroduction(context, container);
        addHeader(context, container, LocaleController.getString(R.string.SingGramLoginChooseAccount));

        LinearLayout choices = addSection(context, container);
        boolean hasSlot = SingGramBotAuth.findFreeAccount() >= 0;
        addChoice(context, choices, R.drawable.settings_account,
                LocaleController.getString(R.string.SingGramLoginPersonalAccount),
                LocaleController.getString(R.string.SingGramLoginPersonalAccountInfo),
                hasSlot, this::openPersonalLogin);
        addDivider(context, choices);
        addChoice(context, choices, R.drawable.msg_addbot,
                LocaleController.getString(R.string.SingGramLoginBotAccount),
                LocaleController.getString(R.string.SingGramLoginBotAccountInfo),
                hasSlot, () -> presentFragment(new SingGramBotLoginActivity()));

        if (!hasSlot) {
            addInfo(context, container, LocaleController.getString(R.string.SingGramAccountSlotsFull));
        } else {
            addInfo(context, container, LocaleController.formatString(R.string.SingGramAccountSlots, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT));
        }
        return fragmentView;
    }

    private void openPersonalLogin() {
        openPersonalLogin(this);
    }

    private void addIntroduction(Context context, LinearLayout container) {
        TextView title = new TextView(context);
        title.setText(LocaleController.getString(R.string.SingGramLoginWelcome));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString(R.string.SingGramLoginInfo));
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(24));
        container.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(section, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));
        return section;
    }

    private void addChoice(Context context, LinearLayout container, int icon, String title, String summary, boolean enabled, Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(AndroidUtilities.dp(76));
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1.0f : 0.5f);
        row.setOnClickListener(v -> {
            if (enabled) {
                action.run();
            }
        });

        ImageView image = new ImageView(context);
        image.setImageResource(icon);
        image.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        image.setContentDescription(title);
        row.addView(image, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setIncludeFontPadding(false);
        text.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView summaryView = new TextView(context);
        summaryView.setText(summary);
        summaryView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        summaryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        summaryView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        summaryView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        text.addView(summaryView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addHeader(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
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
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
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

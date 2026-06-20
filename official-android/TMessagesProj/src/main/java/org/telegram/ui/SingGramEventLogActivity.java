package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
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
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramEventLog;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramEventLogActivity extends BaseFragment {

    private TextView logView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramEventLog));
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

        logView = new TextView(context);
        logView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        logView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        logView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        logView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        logView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        logView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_windowBackgroundWhite)));
        container.addView(logView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), 0);
        container.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addButton(context, row, LocaleController.getString(R.string.Copy), v -> copyLog());
        addButton(context, row, LocaleController.getString(R.string.ClearButton), v -> confirmClear());

        TextView info = new TextView(context);
        info.setText(LocaleController.getString(R.string.SingGramEventLogInfo));
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        info.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        info.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        info.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);
        container.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        reload();
        return fragmentView;
    }

    private void reload() {
        if (logView == null) {
            return;
        }
        String text = SingGramEventLog.getLogText();
        logView.setText(TextUtils.isEmpty(text) ? LocaleController.getString(R.string.SingGramEventLogEmpty) : text);
    }

    private void copyLog() {
        String text = SingGramEventLog.getLogText();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramEventLogEmpty), Toast.LENGTH_SHORT).show();
            return;
        }
        AndroidUtilities.addToClipboard(text);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.TextCopied), Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.SingGramEventLogClear));
        builder.setMessage(LocaleController.getString(R.string.SingGramEventLogClearInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialog, which) -> {
            SingGramEventLog.clear();
            reload();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void addButton(Context context, LinearLayout row, String text, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        button.setTypeface(AndroidUtilities.bold());
        button.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        button.setBackground(Theme.createRadSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.10f), Theme.getColor(Theme.key_listSelector), 8, 8));
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setOnClickListener(listener);
        row.addView(button, LayoutHelper.createLinear(0, 42, 1f, 4, 0, 4, 0));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}

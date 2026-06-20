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
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.SingGramUpdateClient;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramUpdateActivity extends BaseFragment {

    private LinearLayout contentContainer;
    private SingGramUpdateClient.UpdateInfo updateInfo;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramUpdates));
        actionBar.setAllowOverlayTitle(true);
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

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(contentContainer, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        buildContent(context);
        checkForUpdates();
        return fragmentView;
    }

    private void buildContent(Context context) {
        contentContainer.removeAllViews();

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateCurrentBuild));
        LinearLayout currentSection = addSection(context, contentContainer);
        addInfoCell(context, currentSection, LocaleController.getString(R.string.SingGramDoctorVersion), BuildVars.BUILD_VERSION_STRING);
        addDivider(context, currentSection);
        addInfoCell(context, currentSection, LocaleController.getString(R.string.SingGramUpdateVersionCode), String.valueOf(SharedConfig.buildVersion()));

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateLatestBuild));
        LinearLayout latestSection = addSection(context, contentContainer);
        if (updateInfo == null) {
            addInfoCell(context, latestSection, LocaleController.getString(R.string.SingGramUpdateStatus), LocaleController.getString(R.string.SingGramUpdateNotChecked));
        } else {
            addInfoCell(context, latestSection, LocaleController.getString(R.string.SingGramUpdateStatus), updateInfo.summary());
            addDivider(context, latestSection);
            addInfoCell(context, latestSection, LocaleController.getString(R.string.SingGramDoctorVersion), valueOrDash(updateInfo.versionName));
            addDivider(context, latestSection);
            addInfoCell(context, latestSection, LocaleController.getString(R.string.SingGramUpdateVersionCode), updateInfo.versionCode <= 0 ? "-" : String.valueOf(updateInfo.versionCode));
            if (!TextUtils.isEmpty(updateInfo.publishedAt)) {
                addDivider(context, latestSection);
                addInfoCell(context, latestSection, LocaleController.getString(R.string.SingGramUpdatePublishedAt), updateInfo.publishedAt);
            }
        }

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateActions));
        LinearLayout actionSection = addSection(context, contentContainer);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCheck), LocaleController.getString(R.string.SingGramUpdateCheckInfo), true, v -> checkForUpdates());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateOpenApk), apkValue(), updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> openApk());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCopyApk), LocaleController.getString(R.string.SingGramUpdateCopyApkInfo), updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> copyApkUrl());

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateNotes));
        LinearLayout notesSection = addSection(context, contentContainer);
        addInfoBlock(context, notesSection, notesValue());
        addActionCell(context, notesSection, LocaleController.getString(R.string.SingGramUpdateCopyNotes), LocaleController.getString(R.string.SingGramUpdateCopyNotesInfo), updateInfo != null && !TextUtils.isEmpty(updateInfo.notes), v -> copyNotes());
        addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateInfo));
    }

    private String valueOrDash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private String apkValue() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return LocaleController.getString(R.string.SingGramUpdateNoApk);
        }
        if (TextUtils.isEmpty(updateInfo.sha256)) {
            return updateInfo.apkUrl;
        }
        return updateInfo.apkUrl + "\nSHA-256 " + updateInfo.sha256;
    }

    private String notesValue() {
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramUpdateNotesEmpty);
        }
        return TextUtils.isEmpty(updateInfo.notes) ? LocaleController.getString(R.string.SingGramUpdateNotesEmpty) : updateInfo.notes;
    }

    private void checkForUpdates() {
        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        SingGramUpdateClient.check(new SingGramUpdateClient.Callback() {
            @Override
            public void onResult(SingGramUpdateClient.UpdateInfo info) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                updateInfo = info;
                SingGramConfig.setLastUpdateCheck(info.versionCode, info.versionName);
                Toast.makeText(getParentActivity(), info.hasUpdate() ? LocaleController.getString(R.string.SingGramUpdateAvailable) : LocaleController.getString(R.string.SingGramUpdateCurrent), Toast.LENGTH_SHORT).show();
                buildContent(getParentActivity());
            }

            @Override
            public void onError(String error) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }
                Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                buildContent(getParentActivity());
            }
        });
    }

    private void openApk() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return;
        }
        Browser.openUrl(getParentActivity(), updateInfo.apkUrl);
    }

    private void copyApkUrl() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return;
        }
        AndroidUtilities.addToClipboard(updateInfo.apkUrl);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateApkCopied), Toast.LENGTH_SHORT).show();
    }

    private void copyNotes() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.notes)) {
            return;
        }
        AndroidUtilities.addToClipboard(updateInfo.notes);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateNotesCopied), Toast.LENGTH_SHORT).show();
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
        addActionCell(context, container, text, value, false, null);
    }

    private void addActionCell(Context context, LinearLayout container, String text, String value, boolean enabled, View.OnClickListener listener) {
        org.telegram.ui.Cells.TextCheckCell cell = new org.telegram.ui.Cells.TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfoBlock(Context context, LinearLayout container, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
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
        textView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(2));
        container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }
}

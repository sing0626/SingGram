package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramUpdateActivity extends BaseFragment {

    private LinearLayout contentContainer;
    private SingGramUpdateClient.UpdateInfo updateInfo;
    private boolean checking;
    private String lastError;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramOtaCenter));
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
        scrollView.setFillViewport(true);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(28));
        scrollView.addView(contentContainer, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        buildContent(context, false);
        checkForUpdates();
        return fragmentView;
    }

    private void buildContent(Context context, boolean animated) {
        if (context == null || contentContainer == null) {
            return;
        }
        contentContainer.removeAllViews();

        addHeroCard(context, contentContainer);

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramOtaVersionMap));
        LinearLayout versionSection = addSection(context, contentContainer);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaInstalled), BuildVars.BUILD_VERSION_STRING + " / " + SharedConfig.buildVersion());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaLatest), latestBuildValue());
        addDivider(context, versionSection);
        addInfoCell(context, versionSection, LocaleController.getString(R.string.SingGramOtaBuildDelta), buildDeltaValue());

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateActions));
        LinearLayout actionSection = addSection(context, contentContainer);
        addPrimaryButton(context, actionSection, checking ? LocaleController.getString(R.string.SingGramOtaChecking) : LocaleController.getString(R.string.SingGramUpdateCheck), LocaleController.getString(R.string.SingGramUpdateCheckInfo), true, v -> checkForUpdates());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramOtaOpenLatest), apkValue(), updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> openApk());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCopyApk), LocaleController.getString(R.string.SingGramUpdateCopyApkInfo), updateInfo != null && !TextUtils.isEmpty(updateInfo.apkUrl), v -> copyApkUrl());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateOpenRelease), LocaleController.getString(R.string.SingGramUpdateOpenReleaseInfo), true, v -> openReleasePage());
        addDivider(context, actionSection);
        addActionCell(context, actionSection, LocaleController.getString(R.string.SingGramUpdateCopyRelease), SingGramUpdateClient.DEFAULT_RELEASE_URL, true, v -> copyReleaseUrl());

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramOtaReleaseManifest));
        LinearLayout manifestSection = addSection(context, contentContainer);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateStatus), statusTitle());
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdatePublishedAt), valueOrDash(updateInfo == null ? "" : updateInfo.publishedAt));
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateApkSize), updateInfo != null && updateInfo.apkSizeBytes > 0 ? AndroidUtilities.formatFileSize(updateInfo.apkSizeBytes) : "-");
        addDivider(context, manifestSection);
        addInfoCell(context, manifestSection, LocaleController.getString(R.string.SingGramUpdateSha256), valueOrDash(updateInfo == null ? "" : updateInfo.sha256));

        addHeader(context, contentContainer, LocaleController.getString(R.string.SingGramUpdateNotes));
        LinearLayout notesSection = addSection(context, contentContainer);
        addInfoBlock(context, notesSection, notesValue());
        addActionCell(context, notesSection, LocaleController.getString(R.string.SingGramUpdateCopyNotes), LocaleController.getString(R.string.SingGramUpdateCopyNotesInfo), updateInfo != null && !TextUtils.isEmpty(updateInfo.notes), v -> copyNotes());
        addInfo(context, contentContainer, LocaleController.getString(R.string.SingGramOtaInfo));

        if (animated) {
            animateRefresh();
        }
    }

    private void addHeroCard(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(liquidGlassBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView eyebrow = new TextView(context);
        eyebrow.setText(LocaleController.getString(R.string.SingGramOtaHeroTitle));
        eyebrow.setTextColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText), 0.90f));
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        eyebrow.setIncludeFontPadding(false);
        card.addView(eyebrow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(statusTitle());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        title.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(statusSubtitle());
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramOtaInstalled), String.valueOf(SharedConfig.buildVersion()));
        addHeroMetric(context, row, LocaleController.getString(R.string.SingGramOtaLatest), updateInfo == null || updateInfo.versionCode <= 0 ? "-" : String.valueOf(updateInfo.versionCode));
    }

    private void addHeroMetric(Context context, LinearLayout row, String label, String value) {
        LinearLayout metric = new LinearLayout(context);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        metric.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite), 0.64f)));
        row.addView(metric, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 6, 0));

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        labelView.setSingleLine(true);
        labelView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(labelView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setSingleLine(true);
        valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private GradientDrawable liquidGlassBackground() {
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.24f : 0.14f),
                Theme.multAlpha(white, SingGramConfig.isLiquidGlassEnabled() ? 0.92f : 0.84f),
                Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.12f : 0.08f)
        });
        drawable.setCornerRadius(AndroidUtilities.dp(SingGramConfig.isLiquidGlassEnabled() ? 18 : 12));
        drawable.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(accent, SingGramConfig.isLiquidGlassEnabled() ? 0.20f : 0.12f));
        return drawable;
    }

    private String statusTitle() {
        if (checking) {
            return LocaleController.getString(R.string.SingGramOtaChecking);
        }
        if (!TextUtils.isEmpty(lastError)) {
            return LocaleController.getString(R.string.SingGramOtaErrorTitle);
        }
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        return updateInfo.statusText();
    }

    private String statusSubtitle() {
        if (checking) {
            return LocaleController.getString(R.string.SingGramOtaCheckingInfo);
        }
        if (!TextUtils.isEmpty(lastError)) {
            return lastError;
        }
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramOtaNotCheckedInfo);
        }
        if (updateInfo.hasUpdate()) {
            return LocaleController.getString(R.string.SingGramOtaAvailableInfo);
        }
        return LocaleController.getString(R.string.SingGramOtaCurrentInfo);
    }

    private String latestBuildValue() {
        if (updateInfo == null || updateInfo.versionCode <= 0) {
            return "-";
        }
        String version = TextUtils.isEmpty(updateInfo.versionName) ? LocaleController.getString(R.string.SingGramUpdates) : updateInfo.versionName;
        return version + " / " + updateInfo.versionCode;
    }

    private String buildDeltaValue() {
        if (updateInfo == null || updateInfo.versionCode <= 0) {
            return "-";
        }
        int delta = updateInfo.versionCode - SharedConfig.buildVersion();
        if (delta > 0) {
            return LocaleController.formatString(R.string.SingGramOtaBuildsAhead, delta);
        } else if (delta < 0) {
            return LocaleController.formatString(R.string.SingGramOtaBuildsNewerInstalled, Math.abs(delta));
        }
        return LocaleController.getString(R.string.SingGramUpdateSameVersion);
    }

    private String valueOrDash(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private String apkValue() {
        if (updateInfo == null || TextUtils.isEmpty(updateInfo.apkUrl)) {
            return LocaleController.getString(R.string.SingGramUpdateNoApk);
        }
        return LocaleController.getString(R.string.SingGramOtaOpenLatestInfo);
    }

    private String notesValue() {
        if (updateInfo == null) {
            return LocaleController.getString(R.string.SingGramUpdateNotesEmpty);
        }
        return TextUtils.isEmpty(updateInfo.notes) ? LocaleController.getString(R.string.SingGramUpdateNotesEmpty) : updateInfo.notes;
    }

    private void checkForUpdates() {
        if (checking) {
            return;
        }
        checking = true;
        lastError = null;
        buildContent(getParentActivity(), true);
        SingGramUpdateClient.check(new SingGramUpdateClient.Callback() {
            @Override
            public void onResult(SingGramUpdateClient.UpdateInfo info) {
                checking = false;
                updateInfo = info;
                SingGramConfig.setLastUpdateCheck(info.versionCode, info.versionName);
                Toast.makeText(getParentActivity(), info.hasUpdate() ? LocaleController.getString(R.string.SingGramUpdateAvailable) : LocaleController.getString(R.string.SingGramUpdateCurrent), Toast.LENGTH_SHORT).show();
                buildContent(getParentActivity(), true);
            }

            @Override
            public void onError(String error) {
                checking = false;
                lastError = error;
                Toast.makeText(getParentActivity(), error, Toast.LENGTH_LONG).show();
                buildContent(getParentActivity(), true);
            }
        });
    }

    private void animateRefresh() {
        contentContainer.animate().cancel();
        contentContainer.setAlpha(0.0f);
        contentContainer.setTranslationY(AndroidUtilities.dp(8));
        contentContainer.animate()
                .alpha(1.0f)
                .translationY(0)
                .setDuration(190)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start();
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

    private void openReleasePage() {
        Browser.openUrl(getParentActivity(), SingGramUpdateClient.DEFAULT_RELEASE_URL);
    }

    private void copyReleaseUrl() {
        AndroidUtilities.addToClipboard(SingGramUpdateClient.DEFAULT_RELEASE_URL);
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramUpdateReleaseCopied), Toast.LENGTH_SHORT).show();
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
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addPrimaryButton(Context context, LinearLayout container, String text, String value, boolean enabled, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(text + "\n" + value);
        button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.58f);
        button.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        button.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        button.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed), 8, 8));
        button.setOnClickListener(listener);
        container.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 12, 12, 12));
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

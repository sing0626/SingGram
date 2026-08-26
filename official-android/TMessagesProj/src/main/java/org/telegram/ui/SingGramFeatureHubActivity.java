package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SingGramChatNotesStore;
import org.telegram.messenger.SingGramConfig;
import org.telegram.messenger.SingGramDownloadStats;
import org.telegram.messenger.SingGramEventLog;
import org.telegram.messenger.SingGramPushDiagnostics;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramFeatureHubActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramFeatureHub));
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

        addHero(context, container);

        addHeader(context, container, LocaleController.getString(R.string.SingGramFeatureHubControlCenter));
        LinearLayout statusSection = addSection(context, container);
        addFeatureCell(context, statusSection, LocaleController.getString(R.string.SingGramAINewApiConnection), aiProviderStatusValue(), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, v -> presentFragment(SingGramSettingsActivity.aiPage()));
        addDivider(context, statusSection);
        addFeatureCell(context, statusSection, LocaleController.getString(R.string.SingGramCrashSafeMode), crashValue(), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, v -> presentFragment(SingGramSettingsActivity.diagnosticsPage()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramFeatureHubNow));
        LinearLayout liveSection = addSection(context, container);
        addFeatureCell(context, liveSection, LocaleController.getString(R.string.SingGramLiquidGlassStudio), liquidGlassValue(), R.drawable.settings_chat, 0xFFB659FF, 0xFF617CFF, v -> presentFragment(new SingGramLiquidGlassStudioActivity()));
        addDivider(context, liveSection);
        addFeatureCell(context, liveSection, LocaleController.getString(R.string.SingGramUpdates), updateValue(), R.drawable.settings_features, 0xFF4EA5F6, 0xFF3577E5, v -> presentFragment(new SingGramUpdateActivity()));
        addDivider(context, liveSection);
        addFeatureCell(context, liveSection, LocaleController.getString(R.string.SingGramDownloadCenter), downloadValue(), R.drawable.settings_data, 0xFF40B7FF, 0xFF168BDE, v -> presentFragment(new SingGramDownloadStatusActivity()));
        addDivider(context, liveSection);
        addFeatureCell(context, liveSection, LocaleController.getString(R.string.SingGramDoctor), pushValue(), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, v -> presentFragment(new SingGramDoctorActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramFeatureHubPrivacyAi));
        LinearLayout aiPrivacySection = addSection(context, container);
        addFeatureCell(context, aiPrivacySection, LocaleController.getString(R.string.SingGramPrivacyPanel), privacyValue(), R.drawable.settings_privacy, 0xFF55CA47, 0xFF27B434, v -> presentFragment(SingGramSettingsActivity.privacyPage()));
        addDivider(context, aiPrivacySection);
        addFeatureCell(context, aiPrivacySection, LocaleController.getString(R.string.SingGramAIShortcutTools), aiValue(), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, v -> presentFragment(SingGramSettingsActivity.aiPage()));
        addDivider(context, aiPrivacySection);
        addFeatureCell(context, aiPrivacySection, LocaleController.getString(R.string.SingGramChatNotesAll), LocaleController.formatString(R.string.SingGramChatNotesAllCount, SingGramChatNotesStore.getNotesCount()), R.drawable.msg_addbio, 0xFF55CA47, 0xFF27B434, v -> presentFragment(new SingGramChatNotesListActivity()));

        addHeader(context, container, LocaleController.getString(R.string.SingGramFeatureHubSetup));
        LinearLayout setupSection = addSection(context, container);
        addFeatureCell(context, setupSection, LocaleController.getString(R.string.SingGramAccountOverview), LocaleController.formatString(R.string.SingGramDoctorAccountsValue, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT), R.drawable.settings_account, 0xFF4EA5F6, 0xFF3577E5, v -> presentFragment(new SingGramAccountOverviewActivity()));
        addDivider(context, setupSection);
        addFeatureCell(context, setupSection, LocaleController.getString(R.string.SingGramCrashRecovery), crashValue(), R.drawable.settings_power, 0xFFFF8B3D, 0xFFE45644, v -> presentFragment(SingGramSettingsActivity.diagnosticsPage()));
        addDivider(context, setupSection);
        addFeatureCell(context, setupSection, LocaleController.getString(R.string.SingGramMaterialYouIcon), LocaleController.getString(R.string.SingGramMaterialYouIconInfo), R.drawable.settings_features, 0xFF8A98A7, 0xFF5D6C7B, null);
        addDivider(context, setupSection);
        addFeatureCell(context, setupSection, LocaleController.getString(R.string.SingGramCommandPalette), LocaleController.getString(R.string.SingGramCommandPaletteInfo), R.drawable.premium_ai_editor, 0xFF23B9C9, 0xFF2684E8, v -> presentFragment(new SingGramCommandPaletteActivity()));

        addInfo(context, container, LocaleController.getString(R.string.SingGramFeatureHubFootnote));
        return fragmentView;
    }

    private void addHero(Context context, LinearLayout container) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.setBackground(heroBackground());
        container.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 4));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString(R.string.SingGramFeatureHubTitle));
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        title.setIncludeFontPadding(false);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText(LocaleController.getString(R.string.SingGramFeatureHubSubtitle));
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitle.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(14));
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addMetric(context, row, LocaleController.getString(R.string.SingGramFeatureHubBuild), BuildVars.BUILD_VERSION_STRING);
        addMetric(context, row, LocaleController.getString(R.string.SingGramFeatureHubEnabled), enabledCount() + "/11");
    }

    private GradientDrawable heroBackground() {
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                Theme.multAlpha(accent, 0.24f),
                Theme.multAlpha(white, 0.92f),
                Theme.multAlpha(0xFF55CA47, 0.14f)
        });
        drawable.setCornerRadius(AndroidUtilities.dp(SingGramConfig.isLiquidGlassEnabled() ? 18 : 12));
        drawable.setStroke(AndroidUtilities.dp(1), Theme.multAlpha(accent, 0.18f));
        return drawable;
    }

    private void addMetric(Context context, LinearLayout row, String label, String value) {
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
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setSingleLine(true);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        metric.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private int enabledCount() {
        int count = 0;
        if (SingGramConfig.isLiquidGlassEnabled()) count++;
        if (SingGramConfig.getLastUpdateVersionCode() > 0) count++;
        if (SingGramConfig.isDownloadBoostEnabled()) count++;
        if (SingGramPushDiagnostics.getSnapshot().registeredAccounts > 0) count++;
        if (SingGramConfig.isGhostModeEnabled()) count++;
        if (SingGramConfig.shouldKeepDeletedMessages() || SingGramConfig.shouldKeepOriginalEdits()) count++;
        if (SingGramConfig.isAiEnabled()) count++;
        if (SingGramConfig.shouldAiPreferCantonese()) count++;
        if (UserConfig.getActivatedAccountsCount() > 1) count++;
        if (SingGramConfig.isCrashSafeModeEnabled()) count++;
        return count;
    }

    private String liquidGlassValue() {
        String state = stateValue(SingGramConfig.isLiquidGlassEnabled());
        String custom = SingGramConfig.isLiquidGlassCustomEnabled() ? LocaleController.getString(R.string.SingGramDoctorCustom) : LocaleController.getString(R.string.SingGramLiquidGlassStudioPreset);
        return state + " / " + custom;
    }

    private String updateValue() {
        int versionCode = SingGramConfig.getLastUpdateVersionCode();
        if (versionCode <= 0) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        String state = versionCode > SharedConfig.buildVersion()
                ? LocaleController.getString(R.string.SingGramUpdateAvailable)
                : LocaleController.getString(R.string.SingGramUpdateCurrent);
        String versionName = SingGramConfig.getLastUpdateVersionName();
        return (TextUtils.isEmpty(versionName) ? String.valueOf(versionCode) : versionName) + " / " + state;
    }

    private String downloadValue() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        return LocaleController.formatString(R.string.SingGramDownloadStatusSummary, stateValue(SingGramConfig.isDownloadBoostEnabled()), downloadLevelName(), snapshot.activeCount, AndroidUtilities.formatFileSize(snapshot.speedBytesPerSecond) + "/s");
    }

    private String pushValue() {
        return SingGramPushDiagnostics.summary(SingGramPushDiagnostics.getSnapshot());
    }

    private String privacyValue() {
        return LocaleController.formatString(R.string.SingGramFeatureHubPrivacyValue, stateValue(SingGramConfig.isGhostModeEnabled()), SingGramConfig.getGhostDialogCount(), SingGramEventLog.getEventCount());
    }

    private String aiValue() {
        String configured = SingGramConfig.isAiConfigured() ? SingGramConfig.getAiModel() : LocaleController.getString(R.string.SingGramDoctorNotConfigured);
        return stateValue(SingGramConfig.isAiEnabled()) + " / " + configured;
    }

    private String aiProviderStatusValue() {
        String provider = SingGramConfig.getAiProviderSummary();
        if (TextUtils.isEmpty(provider)) {
            provider = LocaleController.getString(R.string.SingGramAIProviderNone);
        }
        return provider + " / " + aiValue();
    }

    private String crashValue() {
        if (SingGramConfig.getLastCrashTime() > 0) {
            return LocaleController.getString(R.string.SingGramCrashSafeModeAutoInfo);
        }
        return stateValue(SingGramConfig.isCrashSafeModeEnabled()) + " / " + LocaleController.getString(R.string.SingGramCrashRecoveryInfo);
    }

    private String downloadLevelName() {
        int level = SingGramConfig.getDownloadBoostLevel();
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramDownloadBoostBalanced);
        }
        if (level >= 2) {
            return LocaleController.getString(R.string.SingGramDownloadBoostMaximum);
        }
        return LocaleController.getString(R.string.SingGramDownloadBoostAggressive);
    }

    private String stateValue(boolean enabled) {
        return LocaleController.getString(enabled ? R.string.SingGramStateOn : R.string.SingGramStateOff);
    }

    private void addFeatureCell(Context context, LinearLayout container, String text, String value, int icon, int colorTop, int colorBottom, View.OnClickListener listener) {
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setMinimumHeight(AndroidUtilities.dp(72));
        cell.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(11), AndroidUtilities.dp(18), AndroidUtilities.dp(11));

        FrameLayout iconLayout = new FrameLayout(context);
        GradientDrawable iconBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { colorTop, colorBottom });
        iconBackground.setCornerRadius(AndroidUtilities.dp(11));
        iconLayout.setBackground(iconBackground);

        ImageView iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImageResource(icon);
        iconView.setColorFilter(Color.WHITE);
        iconLayout.addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitleView = new TextView(context);
        subtitleView.setText(value);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setSingleLine(false);
        subtitleView.setMaxLines(3);
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        subtitleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 5, 0, 0));

        if (LocaleController.isRTL) {
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 0, 0, 16, 0));
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        } else {
            cell.addView(iconLayout, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL | Gravity.LEFT));
            cell.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 16, 0, 0, 0));
        }

        if (listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        } else {
            cell.setAlpha(0.82f);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private LinearLayout addSection(Context context, LinearLayout container) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setClipToPadding(false);
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

    private void addDivider(Context context, LinearLayout container) {
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 68, 0, 16, 0));
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

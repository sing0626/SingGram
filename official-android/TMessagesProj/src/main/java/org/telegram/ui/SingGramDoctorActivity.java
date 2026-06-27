package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
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
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class SingGramDoctorActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.SingGramDoctor));
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

        addHeader(context, container, LocaleController.getString(R.string.SingGramDoctorSystem));
        LinearLayout systemSection = addSection(context, container);
        addInfoCell(context, systemSection, LocaleController.getString(R.string.SingGramDoctorVersion), BuildVars.BUILD_VERSION_STRING);
        addDivider(context, systemSection);
        addInfoCell(context, systemSection, LocaleController.getString(R.string.SingGramDoctorPackage), packageName());
        addDivider(context, systemSection);
        addInfoCell(context, systemSection, LocaleController.getString(R.string.SingGramDoctorAccounts), LocaleController.formatString(R.string.SingGramDoctorAccountsValue, UserConfig.getActivatedAccountsCount(), UserConfig.MAX_ACCOUNT_COUNT));

        addHeader(context, container, LocaleController.getString(R.string.SingGramPushNotifications));
        LinearLayout pushSection = addSection(context, container);
        addPushDiagnosticsCells(context, pushSection, false);
        addDivider(context, pushSection);
        addActionCell(context, pushSection, LocaleController.getString(R.string.SingGramDoctorRepairPush), LocaleController.getString(R.string.SingGramDoctorRepairPushInfo), true, v -> repairPush());
        addDivider(context, pushSection);
        addActionCell(context, pushSection, LocaleController.getString(R.string.SingGramDoctorOpenNotificationSettings), LocaleController.getString(R.string.SingGramDoctorOpenNotificationSettingsInfo), true, v -> openNotificationSettings());
        addDivider(context, pushSection);
        addActionCell(context, pushSection, LocaleController.getString(R.string.SingGramPushTestNotification), LocaleController.getString(R.string.SingGramPushTestNotificationInfo), true, v -> sendTestNotification());

        addHeader(context, container, LocaleController.getString(R.string.SingGramDoctorFeatureHealth));
        LinearLayout featureSection = addSection(context, container);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramAI), aiValue());
        addDivider(context, featureSection);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramAIUsageToday), SingGramConfig.getAiUsageSummary());
        addDivider(context, featureSection);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramGhostMode), ghostValue());
        addDivider(context, featureSection);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramMessageProtection), protectionValue());
        addDivider(context, featureSection);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramDownload), downloadValue());
        addDivider(context, featureSection);
        addInfoCell(context, featureSection, LocaleController.getString(R.string.SingGramLiquidGlass), liquidGlassValue());

        addHeader(context, container, LocaleController.getString(R.string.SingGramDiagnostics));
        LinearLayout diagnosticsSection = addSection(context, container);
        addInfoCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramCrashSafeMode), crashSafeValue());
        addDivider(context, diagnosticsSection);
        addInfoCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramUpdates), updateValue());
        addDivider(context, diagnosticsSection);
        addInfoCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramOtaInstallHistory), updateInstallHistoryValue());
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramDoctorCopyReport), LocaleController.getString(R.string.SingGramCopyDiagnostics), true, v -> copyReport());
        addDivider(context, diagnosticsSection);
        addActionCell(context, diagnosticsSection, LocaleController.getString(R.string.SingGramDoctorClearCrash), lastCrashValue(), SingGramConfig.getLastCrashTime() > 0, v -> clearCrash());
        addInfo(context, container, LocaleController.getString(R.string.SingGramDoctorInfo));
        return fragmentView;
    }

    private String packageName() {
        return ApplicationLoader.applicationContext == null ? "com.sing.singgram" : ApplicationLoader.applicationContext.getPackageName();
    }

    private String aiValue() {
        String configured = TextUtils.isEmpty(SingGramConfig.getAiBaseUrl()) ? LocaleController.getString(R.string.SingGramDoctorNotConfigured) : SingGramConfig.getAiModel();
        return stateValue(SingGramConfig.isAiEnabled()) + " / " + configured;
    }

    private String ghostValue() {
        return stateValue(SingGramConfig.isGhostModeEnabled()) + " / " + LocaleController.formatString(R.string.SingGramGhostManagerSummary, SingGramConfig.getGhostDialogCount(), SingGramConfig.getReadReceiptsAllowedDialogCount());
    }

    private String protectionValue() {
        String antiDelete = stateValue(SingGramConfig.shouldKeepDeletedMessages());
        String antiEdit = stateValue(SingGramConfig.shouldKeepOriginalEdits());
        return LocaleController.formatString(R.string.SingGramDoctorProtectionValue, antiDelete, antiEdit, SingGramEventLog.getEventCount());
    }

    private String downloadValue() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        String speed = AndroidUtilities.formatFileSize(snapshot.speedBytesPerSecond) + "/s";
        return LocaleController.formatString(R.string.SingGramDownloadStatusSummary, stateValue(SingGramConfig.isDownloadBoostEnabled()), downloadLevelName(), snapshot.activeCount, speed);
    }

    private String liquidGlassValue() {
        String custom = SingGramConfig.isLiquidGlassCustomEnabled() ? LocaleController.getString(R.string.SingGramDoctorCustom) : LocaleController.getString(R.string.SingGramLiquidGlassStudioPreset);
        return stateValue(SingGramConfig.isLiquidGlassEnabled()) + " / " + levelValue(SingGramConfig.getLiquidGlassLevel()) + " / " + custom;
    }

    private String crashSafeValue() {
        String value = stateValue(SingGramConfig.isCrashSafeModeEnabled());
        if (SingGramConfig.getLastCrashTime() > 0) {
            value += " / " + lastCrashValue();
        }
        return value;
    }

    private String updateValue() {
        int versionCode = SingGramConfig.getLastUpdateVersionCode();
        if (versionCode <= 0) {
            return LocaleController.getString(R.string.SingGramUpdateNotChecked);
        }
        String versionName = SingGramConfig.getLastUpdateVersionName();
        String version = TextUtils.isEmpty(versionName) ? String.valueOf(versionCode) : versionName;
        String state = versionCode > SharedConfig.buildVersion()
                ? LocaleController.getString(R.string.SingGramUpdateAvailable)
                : LocaleController.getString(R.string.SingGramUpdateCurrent);
        return version + " / " + state;
    }

    private String updateInstallHistoryValue() {
        String history = SingGramConfig.getUpdateInstallHistory();
        return TextUtils.isEmpty(history) ? LocaleController.getString(R.string.SingGramOtaInstallHistoryEmpty) : history;
    }

    private String lastCrashValue() {
        if (SingGramConfig.getLastCrashTime() <= 0) {
            return LocaleController.getString(R.string.SingGramDoctorNoCrash);
        }
        String value = LocaleController.formatDateTime(SingGramConfig.getLastCrashTime() / 1000, true);
        String reason = SingGramConfig.getLastCrashReason();
        if (!TextUtils.isEmpty(reason)) {
            value += "\n" + reason;
        }
        return value;
    }

    private String stateValue(boolean enabled) {
        return LocaleController.getString(enabled ? R.string.SingGramCommandPaletteOn : R.string.SingGramCommandPaletteOff);
    }

    private String levelValue(int level) {
        if (level <= 0) {
            return LocaleController.getString(R.string.SingGramLiquidGlassSoft);
        }
        if (level >= 2) {
            return LocaleController.getString(R.string.SingGramLiquidGlassStrong);
        }
        return LocaleController.getString(R.string.SingGramLiquidGlassStandard);
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

    private void copyReport() {
        AndroidUtilities.addToClipboard(buildReport());
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDiagnosticsCopied), Toast.LENGTH_SHORT).show();
    }

    private String buildReport() {
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram doctor\n");
        builder.append("version: ").append(BuildVars.BUILD_VERSION_STRING).append('\n');
        builder.append("debug: ").append(BuildVars.DEBUG_VERSION).append('\n');
        builder.append("package: ").append(packageName()).append('\n');
        builder.append("accounts: ").append(UserConfig.getActivatedAccountsCount()).append('/').append(UserConfig.MAX_ACCOUNT_COUNT).append('\n');
        builder.append("ai_enabled: ").append(SingGramConfig.isAiEnabled()).append('\n');
        builder.append("ai_configured: ").append(!TextUtils.isEmpty(SingGramConfig.getAiBaseUrl())).append('\n');
        builder.append("ai_fallback: ").append(SingGramConfig.isAiFallbackEnabled()).append('\n');
        builder.append("ai_usage_today: ").append(SingGramConfig.getAiUsageSummary()).append('\n');
        builder.append("ai_last_error: ").append(SingGramConfig.getAiLastErrorSummary()).append('\n');
        builder.append("ghost_mode: ").append(SingGramConfig.isGhostModeEnabled()).append('\n');
        builder.append("ghost_selected_chats_only: ").append(SingGramConfig.isGhostSelectedChatsOnly()).append('\n');
        builder.append("ghost_selected_count: ").append(SingGramConfig.getGhostDialogCount()).append('\n');
        builder.append("read_receipt_exceptions: ").append(SingGramConfig.getReadReceiptsAllowedDialogCount()).append('\n');
        builder.append("anti_delete: ").append(SingGramConfig.shouldKeepDeletedMessages()).append('\n');
        builder.append("anti_edit: ").append(SingGramConfig.shouldKeepOriginalEdits()).append('\n');
        builder.append("event_log_count: ").append(SingGramEventLog.getEventCount()).append('\n');
        builder.append("download_boost: ").append(SingGramConfig.isDownloadBoostEnabled()).append('\n');
        builder.append("download_boost_level: ").append(SingGramConfig.getDownloadBoostLevel()).append('\n');
        builder.append("download_active_count: ").append(snapshot.activeCount).append('\n');
        builder.append("download_speed_bps: ").append(snapshot.speedBytesPerSecond).append('\n');
        builder.append("liquid_glass: ").append(SingGramConfig.isLiquidGlassEnabled()).append('\n');
        builder.append("liquid_glass_level: ").append(SingGramConfig.getLiquidGlassLevel()).append('\n');
        builder.append("liquid_glass_custom: ").append(SingGramConfig.isLiquidGlassCustomEnabled()).append('\n');
        builder.append("crash_safe_mode: ").append(SingGramConfig.isCrashSafeModeEnabled()).append('\n');
        builder.append("last_crash_time: ").append(SingGramConfig.getLastCrashTime()).append('\n');
        builder.append("last_crash_reason: ").append(SingGramConfig.getLastCrashReason()).append('\n');
        builder.append("last_update_version_code: ").append(SingGramConfig.getLastUpdateVersionCode()).append('\n');
        builder.append("last_update_version_name: ").append(SingGramConfig.getLastUpdateVersionName()).append('\n');
        builder.append("last_update_check_time: ").append(SingGramConfig.getLastUpdateCheckTime()).append('\n');
        builder.append("update_install_history: ").append(SingGramConfig.getUpdateInstallHistory()).append('\n');
        builder.append(SingGramPushDiagnostics.buildReport());
        return builder.toString();
    }

    private void addPushDiagnosticsCells(Context context, LinearLayout section, boolean actionsOnly) {
        SingGramPushDiagnostics.Snapshot snapshot = SingGramPushDiagnostics.getSnapshot();
        if (!actionsOnly) {
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushProvider), SingGramPushDiagnostics.summary(snapshot));
            addDivider(context, section);
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushToken), snapshot.tokenStatus);
            addDivider(context, section);
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushPermissions), SingGramPushDiagnostics.permissionValue(snapshot));
            addDivider(context, section);
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushKeepAlive), SingGramPushDiagnostics.keepAliveValue(snapshot));
            addDivider(context, section);
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushAccountsRegistered), SingGramPushDiagnostics.registeredAccountsValue(snapshot));
            addDivider(context, section);
            addInfoCell(context, section, LocaleController.getString(R.string.SingGramPushChannels), SingGramPushDiagnostics.channelValue(snapshot));
            addDivider(context, section);
        }
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushRefreshToken), LocaleController.getString(R.string.SingGramPushRefreshTokenInfo), true, v -> refreshPushToken());
        addDivider(context, section);
        addActionCell(context, section, LocaleController.getString(R.string.SingGramPushResetChannels), LocaleController.getString(R.string.SingGramPushResetChannelsInfo), true, v -> resetPushChannels());
    }

    private void refreshPushToken() {
        SingGramPushDiagnostics.requestTokenRefresh();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushRefreshRequested), Toast.LENGTH_SHORT).show();
    }

    private void resetPushChannels() {
        SingGramPushDiagnostics.resetNotificationChannels();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushChannelsReset), Toast.LENGTH_SHORT).show();
    }

    private void sendTestNotification() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushTestNotificationBlocked), Toast.LENGTH_LONG).show();
            openNotificationSettings();
            return;
        }
        NotificationsController.checkOtherNotificationsChannel();
        NotificationCompat.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new NotificationCompat.Builder(context, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL)
                : new NotificationCompat.Builder(context);
        builder
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.getString(R.string.SingGramPushTestNotificationTitle))
                .setContentText(LocaleController.getString(R.string.SingGramPushTestNotificationText))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(LocaleController.getString(R.string.SingGramPushTestNotificationText)))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            manager.notify(76026, builder.build());
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushTestNotificationSent), Toast.LENGTH_SHORT).show();
        } catch (Throwable ignore) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramPushTestNotificationBlocked), Toast.LENGTH_LONG).show();
            openNotificationSettings();
        }
    }

    private void repairPush() {
        SingGramPushDiagnostics.repairPushSettings();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDoctorRepairPushDone), Toast.LENGTH_SHORT).show();
        removeSelfFromStack();
        presentFragment(new SingGramDoctorActivity());
    }

    private void openNotificationSettings() {
        if (getParentActivity() == null) {
            return;
        }
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + packageName()));
            }
            getParentActivity().startActivity(intent);
        } catch (Throwable ignore) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + packageName()));
            getParentActivity().startActivity(intent);
        }
    }

    private void clearCrash() {
        SingGramConfig.clearLastCrash();
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.SingGramDoctorCrashCleared), Toast.LENGTH_SHORT).show();
        removeSelfFromStack();
        presentFragment(new SingGramDoctorActivity());
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
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(false);
        cell.setAlpha(0.86f);
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addActionCell(Context context, LinearLayout container, String text, String value, boolean enabled, View.OnClickListener listener) {
        TextCheckCell cell = new TextCheckCell(context, 16);
        cell.setTextAndValue(text, value, true, false);
        cell.setEnabled(enabled);
        cell.setAlpha(enabled ? 1.0f : 0.58f);
        if (enabled && listener != null) {
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            cell.setOnClickListener(listener);
        }
        container.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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

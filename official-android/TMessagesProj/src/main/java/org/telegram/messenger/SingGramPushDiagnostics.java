package org.telegram.messenger;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public final class SingGramPushDiagnostics {

    private SingGramPushDiagnostics() {

    }

    public static final class Snapshot {
        public String providerName;
        public String pushTypeName;
        public boolean providerAvailable;
        public boolean hasToken;
        public String tokenPreview;
        public String tokenStatus;
        public boolean appNotificationsEnabled;
        public boolean runtimePermissionGranted;
        public boolean pushServiceEnabled;
        public boolean pushConnectionEnabled;
        public int activeAccounts;
        public int registeredAccounts;
        public int channelCount;
        public int blockedChannelCount;
        public int alertingChannelCount;
        public int quietChannelCount;
    }

    public static Snapshot getSnapshot() {
        Snapshot snapshot = new Snapshot();
        Context context = ApplicationLoader.applicationContext;
        PushListenerController.IPushListenerServiceProvider provider = getProvider();
        snapshot.pushTypeName = pushTypeName(provider == null ? SharedConfig.pushType : provider.getPushType());
        snapshot.providerName = provider == null ? LocaleController.getString(R.string.SingGramPushProviderUnknown) : providerName(provider);
        snapshot.providerAvailable = provider != null && hasServices(provider);
        snapshot.hasToken = !TextUtils.isEmpty(SharedConfig.pushString);
        snapshot.tokenPreview = snapshot.hasToken ? maskToken(SharedConfig.pushString) : "";
        snapshot.tokenStatus = tokenStatus(snapshot.hasToken, snapshot.tokenPreview, SharedConfig.pushStringStatus);
        snapshot.appNotificationsEnabled = areAppNotificationsEnabled(context);
        snapshot.runtimePermissionGranted = isRuntimePermissionGranted(context);
        readAccounts(snapshot);
        readKeepAlive(snapshot);
        readChannels(context, snapshot);
        return snapshot;
    }

    public static String summary(Snapshot snapshot) {
        return LocaleController.formatString(R.string.SingGramPushSummary,
                snapshot.providerName,
                state(snapshot.providerAvailable),
                snapshot.hasToken ? state(true) : snapshot.tokenStatus,
                state(snapshot.appNotificationsEnabled && snapshot.runtimePermissionGranted));
    }

    public static String permissionValue(Snapshot snapshot) {
        return LocaleController.formatString(R.string.SingGramPushPermissionValue,
                state(snapshot.appNotificationsEnabled),
                state(snapshot.runtimePermissionGranted));
    }

    public static String keepAliveValue(Snapshot snapshot) {
        return LocaleController.formatString(R.string.SingGramPushKeepAliveValue,
                state(snapshot.pushServiceEnabled),
                state(snapshot.pushConnectionEnabled));
    }

    public static String registeredAccountsValue(Snapshot snapshot) {
        return LocaleController.formatString(R.string.SingGramPushAccountsRegisteredValue,
                snapshot.registeredAccounts,
                snapshot.activeAccounts);
    }

    public static String channelValue(Snapshot snapshot) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return LocaleController.getString(R.string.SingGramPushChannelsUnavailable);
        }
        return LocaleController.formatString(R.string.SingGramPushChannelsValue,
                snapshot.channelCount,
                snapshot.blockedChannelCount,
                snapshot.alertingChannelCount,
                snapshot.quietChannelCount);
    }

    public static void requestTokenRefresh() {
        try {
            PushListenerController.IPushListenerServiceProvider provider = getProvider();
            if (provider != null) {
                provider.onRequestPushToken();
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void resetNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Context context = ApplicationLoader.applicationContext;
                if (context != null) {
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    SharedPreferences preferences = context.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                    String otherChannel = preferences.getString("OtherKey", NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
                    if (!TextUtils.isEmpty(otherChannel) && notificationManager != null) {
                        notificationManager.deleteNotificationChannel(otherChannel);
                    }
                    preferences.edit().remove("OtherKey").commit();
                    NotificationsController.OTHER_NOTIFICATIONS_CHANNEL = null;
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig userConfig = UserConfig.getInstance(a);
            if (!userConfig.isClientActivated()) {
                continue;
            }
            NotificationsController controller = NotificationsController.getInstance(a);
            controller.deleteAllNotificationChannels();
            controller.deleteNotificationChannelGlobal(NotificationsController.TYPE_PRIVATE);
            controller.deleteNotificationChannelGlobal(NotificationsController.TYPE_GROUP);
            controller.deleteNotificationChannelGlobal(NotificationsController.TYPE_CHANNEL);
            controller.deleteNotificationChannelGlobal(NotificationsController.TYPE_STORIES);
            controller.deleteNotificationChannelGlobal(NotificationsController.TYPE_REACTIONS_MESSAGES);
        }
        NotificationsController.checkOtherNotificationsChannel();
    }

    public static void repairPushSettings() {
        try {
            SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
            preferences.edit()
                    .putBoolean("pushService", true)
                    .putBoolean("pushConnection", true)
                    .commit();
            MessagesController.getMainSettings(UserConfig.selectedAccount).edit()
                    .putBoolean("keepAliveService", true)
                    .commit();
            MessagesController.getInstance(UserConfig.selectedAccount).keepAliveService = true;
            MessagesController.getInstance(UserConfig.selectedAccount).backgroundConnection = true;
            org.telegram.tgnet.ConnectionsManager.getInstance(UserConfig.selectedAccount).setPushConnectionEnabled(true);
            ApplicationLoader.startPushService();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        requestTokenRefresh();
        resetNotificationChannels();
    }

    public static String buildReport() {
        Snapshot snapshot = getSnapshot();
        StringBuilder builder = new StringBuilder();
        builder.append("push_provider: ").append(snapshot.providerName).append('\n');
        builder.append("push_type: ").append(snapshot.pushTypeName).append('\n');
        builder.append("push_services_available: ").append(snapshot.providerAvailable).append('\n');
        builder.append("push_token_present: ").append(snapshot.hasToken).append('\n');
        builder.append("push_token_preview: ").append(snapshot.tokenPreview).append('\n');
        builder.append("push_status: ").append(snapshot.tokenStatus).append('\n');
        builder.append("notifications_enabled: ").append(snapshot.appNotificationsEnabled).append('\n');
        builder.append("runtime_notification_permission: ").append(snapshot.runtimePermissionGranted).append('\n');
        builder.append("push_service_enabled: ").append(snapshot.pushServiceEnabled).append('\n');
        builder.append("push_connection_enabled: ").append(snapshot.pushConnectionEnabled).append('\n');
        builder.append("push_registered_accounts: ").append(snapshot.registeredAccounts).append('/').append(snapshot.activeAccounts).append('\n');
        builder.append("notification_channels: ").append(snapshot.channelCount).append('\n');
        builder.append("notification_channels_blocked: ").append(snapshot.blockedChannelCount).append('\n');
        builder.append("notification_channels_alerting: ").append(snapshot.alertingChannelCount).append('\n');
        builder.append("notification_channels_quiet: ").append(snapshot.quietChannelCount).append('\n');
        return builder.toString();
    }

    private static PushListenerController.IPushListenerServiceProvider getProvider() {
        try {
            return ApplicationLoader.getPushProvider();
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    private static boolean hasServices(PushListenerController.IPushListenerServiceProvider provider) {
        try {
            return provider.hasServices();
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    private static String providerName(PushListenerController.IPushListenerServiceProvider provider) {
        if (provider.getPushType() == PushListenerController.PUSH_TYPE_FIREBASE) {
            return LocaleController.getString(R.string.SingGramPushProviderFcm);
        } else if (provider.getPushType() == PushListenerController.PUSH_TYPE_HUAWEI) {
            return LocaleController.getString(R.string.SingGramPushProviderHuawei);
        }
        return provider.getLogTitle();
    }

    private static String pushTypeName(int pushType) {
        if (pushType == PushListenerController.PUSH_TYPE_FIREBASE) {
            return LocaleController.getString(R.string.SingGramPushProviderFcm);
        } else if (pushType == PushListenerController.PUSH_TYPE_HUAWEI) {
            return LocaleController.getString(R.string.SingGramPushProviderHuawei);
        }
        return LocaleController.getString(R.string.SingGramPushProviderUnknown);
    }

    private static String tokenStatus(boolean hasToken, String tokenPreview, String rawStatus) {
        if (hasToken) {
            return LocaleController.formatString(R.string.SingGramPushTokenReady, tokenPreview);
        }
        if (TextUtils.isEmpty(rawStatus)) {
            return LocaleController.getString(R.string.SingGramPushTokenMissing);
        }
        if ("__NO_GOOGLE_PLAY_SERVICES__".equals(rawStatus)) {
            return LocaleController.getString(R.string.SingGramPushNoPlayServices);
        }
        if ("__FIREBASE_FAILED__".equals(rawStatus)) {
            return LocaleController.getString(R.string.SingGramPushTokenFailed);
        }
        if (rawStatus.startsWith("__FIREBASE_GENERATING_SINCE_") || rawStatus.startsWith("__HUAWEI_GENERATING_SINCE_")) {
            return LocaleController.getString(R.string.SingGramPushTokenGenerating);
        }
        return rawStatus;
    }

    private static boolean areAppNotificationsEnabled(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    private static boolean isRuntimePermissionGranted(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        try {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    private static void readAccounts(Snapshot snapshot) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig userConfig = UserConfig.getInstance(a);
            if (userConfig.isClientActivated()) {
                snapshot.activeAccounts++;
                if (userConfig.registeredForPush) {
                    snapshot.registeredAccounts++;
                }
            }
        }
    }

    private static void readKeepAlive(Snapshot snapshot) {
        try {
            SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
            if (preferences.contains("pushService")) {
                snapshot.pushServiceEnabled = preferences.getBoolean("pushService", true);
            } else {
                snapshot.pushServiceEnabled = MessagesController.getMainSettings(UserConfig.selectedAccount).getBoolean("keepAliveService", false);
            }
            if (preferences.contains("pushConnection")) {
                snapshot.pushConnectionEnabled = preferences.getBoolean("pushConnection", MessagesController.getInstance(UserConfig.selectedAccount).backgroundConnection);
            } else {
                snapshot.pushConnectionEnabled = MessagesController.getMainSettings(UserConfig.selectedAccount).getBoolean("keepAliveService", false);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void readChannels(Context context, Snapshot snapshot) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            snapshot.channelCount = channels.size();
            for (int i = 0; i < channels.size(); i++) {
                NotificationChannel channel = channels.get(i);
                int importance = channel.getImportance();
                if (importance == NotificationManager.IMPORTANCE_NONE) {
                    snapshot.blockedChannelCount++;
                } else if (importance >= NotificationManager.IMPORTANCE_HIGH) {
                    snapshot.alertingChannelCount++;
                } else {
                    snapshot.quietChannelCount++;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static String maskToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return "";
        }
        if (token.length() <= 12) {
            return token.substring(0, Math.min(4, token.length())) + "...";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }

    private static String state(boolean enabled) {
        return LocaleController.getString(enabled ? R.string.SingGramStateOn : R.string.SingGramStateOff);
    }
}

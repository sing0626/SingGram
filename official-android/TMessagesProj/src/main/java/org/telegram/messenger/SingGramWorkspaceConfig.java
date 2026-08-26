package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Local, per-account controls for notification focus, chat privacy, and automatic downloads. */
public final class SingGramWorkspaceConfig {

    private static final String PREFS_PREFIX = "singgram_workspace_";
    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";
    private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";
    private static final String KEY_PRIORITY_KEYWORDS = "priority_keywords";
    private static final String KEY_GROUP_FOCUS_ENABLED = "group_focus_enabled";
    private static final String KEY_AUTOMATIC_DOWNLOADS_PAUSED = "automatic_downloads_paused";
    private static final String KEY_AUTOMATIC_DOWNLOADS_WIFI_ONLY = "automatic_downloads_wifi_only";
    private static final String KEY_PRIORITY_DIALOGS = "priority_dialogs";
    private static final String KEY_SENSITIVE_DIALOGS = "sensitive_dialogs";
    private static final String KEY_WATCHED_GROUPS = "watched_groups";
    private static final String KEY_GROUP_KEYWORDS_PREFIX = "group_keywords_";

    private static final int DEFAULT_QUIET_HOURS_START = 22 * 60;
    private static final int DEFAULT_QUIET_HOURS_END = 8 * 60;

    private SingGramWorkspaceConfig() {
    }

    private static SharedPreferences prefs(int account) {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        int safeAccount = account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT ? account : UserConfig.selectedAccount;
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_PREFIX + safeAccount, Context.MODE_PRIVATE);
    }

    public static boolean isQuietHoursEnabled(int account) {
        SharedPreferences preferences = prefs(account);
        return preferences != null && preferences.getBoolean(KEY_QUIET_HOURS_ENABLED, false);
    }

    public static void setQuietHoursEnabled(int account, boolean enabled) {
        putBoolean(account, KEY_QUIET_HOURS_ENABLED, enabled);
    }

    public static int getQuietHoursStartMinutes(int account) {
        return getMinutes(account, KEY_QUIET_HOURS_START, DEFAULT_QUIET_HOURS_START);
    }

    public static void setQuietHoursStartMinutes(int account, int minutes) {
        putInt(account, KEY_QUIET_HOURS_START, normalizeMinutes(minutes));
    }

    public static int getQuietHoursEndMinutes(int account) {
        return getMinutes(account, KEY_QUIET_HOURS_END, DEFAULT_QUIET_HOURS_END);
    }

    public static void setQuietHoursEndMinutes(int account, int minutes) {
        putInt(account, KEY_QUIET_HOURS_END, normalizeMinutes(minutes));
    }

    public static String getQuietHoursLabel(int account) {
        return formatMinutes(getQuietHoursStartMinutes(account)) + " - " + formatMinutes(getQuietHoursEndMinutes(account));
    }

    public static String formatMinutes(int minutes) {
        int normalized = normalizeMinutes(minutes);
        return String.format(Locale.US, "%02d:%02d", normalized / 60, normalized % 60);
    }

    public static String getPriorityKeywords(int account) {
        SharedPreferences preferences = prefs(account);
        return preferences == null ? "" : preferences.getString(KEY_PRIORITY_KEYWORDS, "");
    }

    public static void setPriorityKeywords(int account, String keywords) {
        putString(account, KEY_PRIORITY_KEYWORDS, normalizeKeywords(keywords));
    }

    public static boolean isGroupFocusEnabled(int account) {
        SharedPreferences preferences = prefs(account);
        return preferences != null && preferences.getBoolean(KEY_GROUP_FOCUS_ENABLED, false);
    }

    public static void setGroupFocusEnabled(int account, boolean enabled) {
        putBoolean(account, KEY_GROUP_FOCUS_ENABLED, enabled);
    }

    public static boolean areAutomaticDownloadsPaused(int account) {
        SharedPreferences preferences = prefs(account);
        return preferences != null && preferences.getBoolean(KEY_AUTOMATIC_DOWNLOADS_PAUSED, false);
    }

    public static void setAutomaticDownloadsPaused(int account, boolean paused) {
        putBoolean(account, KEY_AUTOMATIC_DOWNLOADS_PAUSED, paused);
        refreshAutomaticDownloads(account);
    }

    public static boolean areAutomaticDownloadsWifiOnly(int account) {
        SharedPreferences preferences = prefs(account);
        return preferences != null && preferences.getBoolean(KEY_AUTOMATIC_DOWNLOADS_WIFI_ONLY, false);
    }

    public static void setAutomaticDownloadsWifiOnly(int account, boolean wifiOnly) {
        putBoolean(account, KEY_AUTOMATIC_DOWNLOADS_WIFI_ONLY, wifiOnly);
        refreshAutomaticDownloads(account);
    }

    public static boolean shouldDeferAutomaticDownload(int account) {
        return areAutomaticDownloadsPaused(account)
                || areAutomaticDownloadsWifiOnly(account) && !ApplicationLoader.isConnectedToWiFi();
    }

    public static boolean isPriorityDialog(int account, long dialogId) {
        return getDialogSet(account, KEY_PRIORITY_DIALOGS).contains(String.valueOf(dialogId));
    }

    public static void setPriorityDialog(int account, long dialogId, boolean enabled) {
        setDialogValue(account, KEY_PRIORITY_DIALOGS, dialogId, enabled);
    }

    public static boolean isSensitiveDialog(int account, long dialogId) {
        return getDialogSet(account, KEY_SENSITIVE_DIALOGS).contains(String.valueOf(dialogId));
    }

    public static void setSensitiveDialog(int account, long dialogId, boolean enabled) {
        setDialogValue(account, KEY_SENSITIVE_DIALOGS, dialogId, enabled);
    }

    public static boolean isWatchedGroup(int account, long dialogId) {
        return getDialogSet(account, KEY_WATCHED_GROUPS).contains(String.valueOf(dialogId));
    }

    public static void setWatchedGroup(int account, long dialogId, boolean enabled) {
        setDialogValue(account, KEY_WATCHED_GROUPS, dialogId, enabled);
    }

    public static String getGroupKeywords(int account, long dialogId) {
        SharedPreferences preferences = prefs(account);
        return preferences == null ? "" : preferences.getString(KEY_GROUP_KEYWORDS_PREFIX + dialogId, "");
    }

    public static void setGroupKeywords(int account, long dialogId, String keywords) {
        putString(account, KEY_GROUP_KEYWORDS_PREFIX + dialogId, normalizeKeywords(keywords));
    }

    public static int getPriorityDialogCount(int account) {
        return getDialogSet(account, KEY_PRIORITY_DIALOGS).size();
    }

    public static int getWatchedGroupCount(int account) {
        return getDialogSet(account, KEY_WATCHED_GROUPS).size();
    }

    public static boolean shouldShowNotification(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null
                || messageObject.isStoryPush || messageObject.isStoryReactionPush
                || messageObject.isReactionPush || messageObject.isOauthPush) {
            return true;
        }
        long dialogId = messageObject.getDialogId();
        String messageText = messageText(messageObject);
        if (isPriorityDialog(account, dialogId) || matchesKeywords(getPriorityKeywords(account), messageText)) {
            return true;
        }
        boolean groupDialog = DialogObject.isChatDialog(dialogId);
        if (groupDialog && matchesKeywords(getGroupKeywords(account, dialogId), messageText)) {
            return true;
        }
        if (groupDialog && isGroupFocusEnabled(account) && !isWatchedGroup(account, dialogId)) {
            return false;
        }
        return !isQuietHoursEnabled(account) || !isQuietNow(account);
    }

    private static boolean isQuietNow(int account) {
        int start = getQuietHoursStartMinutes(account);
        int end = getQuietHoursEndMinutes(account);
        Calendar calendar = Calendar.getInstance();
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (start == end) {
            return true;
        }
        if (start < end) {
            return now >= start && now < end;
        }
        return now >= start || now < end;
    }

    private static String messageText(MessageObject messageObject) {
        StringBuilder builder = new StringBuilder();
        if (messageObject.messageText != null) {
            builder.append(messageObject.messageText);
        }
        if (messageObject.messageOwner.message != null) {
            builder.append('\n').append(messageObject.messageOwner.message);
        }
        return builder.toString().toLowerCase(Locale.US);
    }

    private static boolean matchesKeywords(String keywords, String messageText) {
        if (TextUtils.isEmpty(keywords) || TextUtils.isEmpty(messageText)) {
            return false;
        }
        for (String keyword : splitKeywords(keywords)) {
            if (messageText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<String> splitKeywords(String value) {
        ArrayList<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(value)) {
            return result;
        }
        for (String item : value.split("[,;\\n]")) {
            String normalized = item.trim().toLowerCase(Locale.US);
            if (!TextUtils.isEmpty(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static String normalizeKeywords(String value) {
        ArrayList<String> keywords = splitKeywords(value);
        return TextUtils.join(", ", keywords);
    }

    private static Set<String> getDialogSet(int account, String key) {
        SharedPreferences preferences = prefs(account);
        if (preferences == null) {
            return new HashSet<>();
        }
        Set<String> values = preferences.getStringSet(key, Collections.emptySet());
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    private static void setDialogValue(int account, String key, long dialogId, boolean enabled) {
        SharedPreferences preferences = prefs(account);
        if (preferences == null) {
            return;
        }
        Set<String> values = getDialogSet(account, key);
        if (enabled) {
            values.add(String.valueOf(dialogId));
        } else {
            values.remove(String.valueOf(dialogId));
        }
        preferences.edit().putStringSet(key, values).apply();
    }

    private static int getMinutes(int account, String key, int fallback) {
        SharedPreferences preferences = prefs(account);
        return preferences == null ? fallback : normalizeMinutes(preferences.getInt(key, fallback));
    }

    private static int normalizeMinutes(int minutes) {
        int normalized = minutes % (24 * 60);
        return normalized < 0 ? normalized + 24 * 60 : normalized;
    }

    private static void putBoolean(int account, String key, boolean value) {
        SharedPreferences preferences = prefs(account);
        if (preferences != null) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }

    private static void putInt(int account, String key, int value) {
        SharedPreferences preferences = prefs(account);
        if (preferences != null) {
            preferences.edit().putInt(key, value).apply();
        }
    }

    private static void putString(int account, String key, String value) {
        SharedPreferences preferences = prefs(account);
        if (preferences != null) {
            preferences.edit().putString(key, value == null ? "" : value).apply();
        }
    }

    private static void refreshAutomaticDownloads(int account) {
        if (account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT) {
            DownloadController.getInstance(account).refreshSingGramAutomaticDownloads();
        }
    }
}

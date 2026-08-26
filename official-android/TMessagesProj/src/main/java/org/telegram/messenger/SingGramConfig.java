package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SingGramConfig {

    private static final String PREFS_NAME = "singgram";

    private static final String KEY_AI_ENABLED = "ai_enabled";
    private static final String KEY_AI_BASE_URL = "ai_base_url";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_MODEL = "ai_model";
    private static final String KEY_AI_SYSTEM_PROMPT = "ai_system_prompt";
    private static final String KEY_AI_PROVIDERS = "ai_providers";
    private static final String KEY_AI_ACTIVE_PROVIDER = "ai_active_provider";
    private static final String KEY_AI_LAST_HEALTH_TIME = "ai_last_health_time";
    private static final String KEY_AI_LAST_HEALTH_SUMMARY = "ai_last_health_summary";
    private static final String KEY_AI_FALLBACK = "ai_fallback";
    private static final String KEY_AI_STATS_DAY = "ai_stats_day";
    private static final String KEY_AI_STATS_DAY_REQUESTS = "ai_stats_day_requests";
    private static final String KEY_AI_STATS_DAY_SUCCESS = "ai_stats_day_success";
    private static final String KEY_AI_STATS_DAY_ERRORS = "ai_stats_day_errors";
    private static final String KEY_AI_STATS_DAY_429 = "ai_stats_day_429";
    private static final String KEY_AI_STATS_DAY_LATENCY = "ai_stats_day_latency";
    private static final String KEY_AI_STATS_TOTAL_REQUESTS = "ai_stats_total_requests";
    private static final String KEY_AI_STATS_TOTAL_SUCCESS = "ai_stats_total_success";
    private static final String KEY_AI_STATS_TOTAL_ERRORS = "ai_stats_total_errors";
    private static final String KEY_AI_STATS_TOTAL_429 = "ai_stats_total_429";
    private static final String KEY_AI_STATS_LAST_ERROR = "ai_stats_last_error";
    private static final String KEY_AI_STATS_LAST_TIME = "ai_stats_last_time";
    private static final String KEY_LIQUID_GLASS = "liquid_glass";
    private static final String KEY_HIDE_PHONE_IN_SETTINGS = "hide_phone_in_settings";
    private static final String KEY_AI_CONTEXT_MENU = "ai_context_menu";
    private static final String KEY_AI_TRANSLATE_ACTION = "ai_translate_action";
    private static final String KEY_AI_INSERT_RESULT = "ai_insert_result";
    private static final String KEY_QUICK_REPLY_IDEAS = "quick_reply_ideas";
    private static final String KEY_AI_PREFER_CANTONESE = "ai_prefer_cantonese";
    private static final String KEY_GHOST_MODE = "ghost_mode";
    private static final String KEY_DISABLE_READ_RECEIPTS = "disable_read_receipts";
    private static final String KEY_READ_RECEIPTS_ALLOWED_DIALOG_IDS = "read_receipts_allowed_dialog_ids";
    private static final String KEY_HIDE_TYPING_STATUS = "hide_typing_status";
    private static final String KEY_GHOST_SELECTED_CHATS_ONLY = "ghost_selected_chats_only";
    private static final String KEY_GHOST_DIALOG_IDS = "ghost_dialog_ids";
    private static final String KEY_KEEP_DELETED_MESSAGES = "keep_deleted_messages";
    private static final String KEY_KEEP_ORIGINAL_EDITS = "keep_original_edits";
    private static final String KEY_LIQUID_GLASS_STRONG = "liquid_glass_strong";
    private static final String KEY_LIQUID_GLASS_LEVEL = "liquid_glass_level";
    private static final String KEY_LIQUID_GLASS_CUSTOM = "liquid_glass_custom";
    private static final String KEY_LIQUID_GLASS_THICKNESS_DP = "liquid_glass_thickness_dp";
    private static final String KEY_LIQUID_GLASS_INTENSITY_PERMILLE = "liquid_glass_intensity_permille";
    private static final String KEY_LIQUID_GLASS_INDEX_PERMILLE = "liquid_glass_index_permille";
    private static final String KEY_ACCOUNT_PROFILE_LABEL = "account_profile_label_";
    private static final String KEY_ACCOUNT_PROFILE_GROUP = "account_profile_group_";
    private static final String KEY_ACCOUNT_PROFILE_COLOR = "account_profile_color_";
    private static final String KEY_DOWNLOAD_BOOST = "download_boost";
    private static final String KEY_DOWNLOAD_BOOST_LEVEL = "download_boost_level";
    private static final String KEY_DOWNLOAD_BOOST_AUTO = "download_boost_auto";
    private static final String KEY_CRASH_SAFE_MODE = "crash_safe_mode";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    private static final String KEY_LAST_CRASH_REASON = "last_crash_reason";
    private static final String KEY_SHOW_DIAGNOSTICS = "show_diagnostics";
    private static final String KEY_LAST_UPDATE_VERSION_CODE = "last_update_version_code";
    private static final String KEY_LAST_UPDATE_VERSION_NAME = "last_update_version_name";
    private static final String KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time";
    private static final String KEY_LAST_UPDATE_APK_PATH = "last_update_apk_path";
    private static final String KEY_UPDATE_INSTALL_HISTORY = "update_install_history";
    private static final String KEY_PENDING_UPDATE_INSTALL = "pending_update_install";
    private static final String KEY_LAST_FEATURE_HUB_INTRO_BUILD = "last_feature_hub_intro_build";

    public static final String DEFAULT_AI_MODEL = "gpt-4o-mini";
    private static boolean crashHandlerInstalled;

    public static class AiProvider {
        public final String name;
        public final String baseUrl;
        public final String apiKey;
        public final String model;

        public AiProvider(String name, String baseUrl, String apiKey, String model) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }
    }

    private static SharedPreferences prefs() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAiEnabled() {
        SharedPreferences preferences = prefs();
        return !isCrashSafeModeEnabled() && (preferences == null || preferences.getBoolean(KEY_AI_ENABLED, true));
    }

    public static void setAiEnabled(boolean enabled) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_AI_ENABLED, enabled).apply();
        }
    }

    public static String getAiBaseUrl() {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_AI_BASE_URL, "");
    }

    public static void setAiBaseUrl(String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(KEY_AI_BASE_URL, normalizeAiBaseUrl(value)).apply();
        }
    }

    public static String getAiApiKey() {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_AI_API_KEY, "");
    }

    public static void setAiApiKey(String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(KEY_AI_API_KEY, value == null ? "" : value.trim()).apply();
        }
    }

    public static String getAiModel() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return DEFAULT_AI_MODEL;
        }
        String model = preferences.getString(KEY_AI_MODEL, DEFAULT_AI_MODEL);
        return TextUtils.isEmpty(model) ? DEFAULT_AI_MODEL : model.trim();
    }

    public static void setAiModel(String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            String model = TextUtils.isEmpty(value) ? DEFAULT_AI_MODEL : value.trim();
            preferences.edit().putString(KEY_AI_MODEL, model).apply();
        }
    }

    public static String getAiSystemPrompt() {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_AI_SYSTEM_PROMPT, "");
    }

    public static void setAiSystemPrompt(String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(KEY_AI_SYSTEM_PROMPT, value == null ? "" : value.trim()).apply();
        }
    }

    public static boolean isAiConfigured() {
        return !TextUtils.isEmpty(getAiBaseUrl());
    }

    public static ArrayList<AiProvider> getAiProviders() {
        ArrayList<AiProvider> providers = new ArrayList<>();
        String value = getString(KEY_AI_PROVIDERS, "");
        if (TextUtils.isEmpty(value)) {
            return providers;
        }
        for (String line : value.split("\\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                continue;
            }
            String name = unescapeProfilePart(parts[0]);
            String baseUrl = normalizeAiBaseUrl(unescapeProfilePart(parts[1]));
            String apiKey = unescapeProfilePart(parts[2]);
            String model = unescapeProfilePart(parts[3]);
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(baseUrl)) {
                providers.add(new AiProvider(name, baseUrl, apiKey, TextUtils.isEmpty(model) ? DEFAULT_AI_MODEL : model));
            }
        }
        return providers;
    }

    public static boolean saveCurrentAiProvider() {
        String baseUrl = normalizeAiBaseUrl(getAiBaseUrl());
        if (TextUtils.isEmpty(baseUrl)) {
            return false;
        }
        String name = providerNameForBaseUrl(baseUrl);
        ArrayList<AiProvider> providers = getAiProviders();
        AiProvider current = new AiProvider(name, baseUrl, getAiApiKey(), getAiModel());
        boolean replaced = false;
        for (int i = 0; i < providers.size(); i++) {
            AiProvider provider = providers.get(i);
            if (TextUtils.equals(provider.name, name) || TextUtils.equals(provider.baseUrl, baseUrl)) {
                providers.set(i, current);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            providers.add(current);
        }
        saveAiProviders(providers);
        setString(KEY_AI_ACTIVE_PROVIDER, name);
        return true;
    }

    public static void applyAiProvider(AiProvider provider) {
        if (provider == null) {
            return;
        }
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .putString(KEY_AI_BASE_URL, normalizeAiBaseUrl(provider.baseUrl))
                    .putString(KEY_AI_API_KEY, provider.apiKey == null ? "" : provider.apiKey.trim())
                    .putString(KEY_AI_MODEL, TextUtils.isEmpty(provider.model) ? DEFAULT_AI_MODEL : provider.model.trim())
                    .putString(KEY_AI_ACTIVE_PROVIDER, provider.name == null ? "" : provider.name.trim())
                    .apply();
        }
    }

    public static void deleteAiProvider(AiProvider target) {
        if (target == null) {
            return;
        }
        ArrayList<AiProvider> providers = getAiProviders();
        for (int i = providers.size() - 1; i >= 0; i--) {
            AiProvider provider = providers.get(i);
            if (TextUtils.equals(provider.name, target.name) && TextUtils.equals(provider.baseUrl, target.baseUrl)) {
                providers.remove(i);
            }
        }
        saveAiProviders(providers);
        if (TextUtils.equals(getActiveAiProviderName(), target.name)) {
            setString(KEY_AI_ACTIVE_PROVIDER, "");
        }
    }

    public static void setAiLastHealth(String summary) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .putLong(KEY_AI_LAST_HEALTH_TIME, System.currentTimeMillis())
                    .putString(KEY_AI_LAST_HEALTH_SUMMARY, summary == null ? "" : summary)
                    .apply();
        }
    }

    public static long getAiLastHealthTime() {
        SharedPreferences preferences = prefs();
        return preferences == null ? 0 : preferences.getLong(KEY_AI_LAST_HEALTH_TIME, 0);
    }

    public static String getAiLastHealthSummary() {
        return getString(KEY_AI_LAST_HEALTH_SUMMARY, "");
    }

    public static boolean isAiFallbackEnabled() {
        return getBoolean(KEY_AI_FALLBACK, true);
    }

    public static void setAiFallbackEnabled(boolean enabled) {
        setBoolean(KEY_AI_FALLBACK, enabled);
    }

    public static void recordAiRequest(boolean success, long latencyMs, String error) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        String today = todayKey();
        SharedPreferences.Editor editor = preferences.edit();
        boolean newDay = !TextUtils.equals(preferences.getString(KEY_AI_STATS_DAY, ""), today);
        if (newDay) {
            editor.putString(KEY_AI_STATS_DAY, today)
                    .putInt(KEY_AI_STATS_DAY_REQUESTS, 0)
                    .putInt(KEY_AI_STATS_DAY_SUCCESS, 0)
                    .putInt(KEY_AI_STATS_DAY_ERRORS, 0)
                    .putInt(KEY_AI_STATS_DAY_429, 0)
                    .putLong(KEY_AI_STATS_DAY_LATENCY, 0);
        }
        int dayRequests = newDay ? 0 : preferences.getInt(KEY_AI_STATS_DAY_REQUESTS, 0);
        int daySuccess = newDay ? 0 : preferences.getInt(KEY_AI_STATS_DAY_SUCCESS, 0);
        int dayErrors = newDay ? 0 : preferences.getInt(KEY_AI_STATS_DAY_ERRORS, 0);
        int day429 = newDay ? 0 : preferences.getInt(KEY_AI_STATS_DAY_429, 0);
        long dayLatency = newDay ? 0 : preferences.getLong(KEY_AI_STATS_DAY_LATENCY, 0);
        editor.putInt(KEY_AI_STATS_DAY_REQUESTS, dayRequests + 1)
                .putInt(KEY_AI_STATS_TOTAL_REQUESTS, preferences.getInt(KEY_AI_STATS_TOTAL_REQUESTS, 0) + 1)
                .putLong(KEY_AI_STATS_LAST_TIME, System.currentTimeMillis());
        if (success) {
            editor.putInt(KEY_AI_STATS_DAY_SUCCESS, daySuccess + 1)
                    .putInt(KEY_AI_STATS_TOTAL_SUCCESS, preferences.getInt(KEY_AI_STATS_TOTAL_SUCCESS, 0) + 1)
                    .putLong(KEY_AI_STATS_DAY_LATENCY, dayLatency + Math.max(0, latencyMs))
                    .putString(KEY_AI_STATS_LAST_ERROR, "");
        } else {
            boolean rateLimited = !TextUtils.isEmpty(error) && error.contains("429");
            editor.putInt(KEY_AI_STATS_DAY_ERRORS, dayErrors + 1)
                    .putInt(KEY_AI_STATS_TOTAL_ERRORS, preferences.getInt(KEY_AI_STATS_TOTAL_ERRORS, 0) + 1)
                    .putString(KEY_AI_STATS_LAST_ERROR, error == null ? "" : error);
            if (rateLimited) {
                editor.putInt(KEY_AI_STATS_DAY_429, day429 + 1)
                        .putInt(KEY_AI_STATS_TOTAL_429, preferences.getInt(KEY_AI_STATS_TOTAL_429, 0) + 1);
            }
        }
        editor.apply();
    }

    public static String getAiUsageSummary() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return "";
        }
        ensureAiStatsDay(preferences);
        int requests = preferences.getInt(KEY_AI_STATS_DAY_REQUESTS, 0);
        int success = preferences.getInt(KEY_AI_STATS_DAY_SUCCESS, 0);
        int errors = preferences.getInt(KEY_AI_STATS_DAY_ERRORS, 0);
        int rateLimited = preferences.getInt(KEY_AI_STATS_DAY_429, 0);
        long latency = preferences.getLong(KEY_AI_STATS_DAY_LATENCY, 0);
        long average = success <= 0 ? 0 : latency / success;
        return LocaleController.formatString(R.string.SingGramAIUsageSummary, requests, success, errors, rateLimited, average);
    }

    public static String getAiLastErrorSummary() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return "";
        }
        String error = preferences.getString(KEY_AI_STATS_LAST_ERROR, "");
        long time = preferences.getLong(KEY_AI_STATS_LAST_TIME, 0);
        if (TextUtils.isEmpty(error)) {
            return LocaleController.getString(R.string.SingGramAIStatsNoError);
        }
        String date = time <= 0 ? "" : LocaleController.formatDateTime(time / 1000, true) + "\n";
        return date + error;
    }

    public static String getActiveAiProviderName() {
        return getString(KEY_AI_ACTIVE_PROVIDER, "");
    }

    public static String getAiProviderSummary() {
        String active = getActiveAiProviderName();
        if (!TextUtils.isEmpty(active)) {
            return active;
        }
        String baseUrl = getAiBaseUrl();
        return TextUtils.isEmpty(baseUrl) ? "" : providerNameForBaseUrl(baseUrl);
    }

    private static void saveAiProviders(ArrayList<AiProvider> providers) {
        StringBuilder builder = new StringBuilder();
        for (AiProvider provider : providers) {
            if (provider == null || TextUtils.isEmpty(provider.name) || TextUtils.isEmpty(provider.baseUrl)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(escapeProfilePart(provider.name))
                    .append('|')
                    .append(escapeProfilePart(normalizeAiBaseUrl(provider.baseUrl)))
                    .append('|')
                    .append(escapeProfilePart(provider.apiKey))
                    .append('|')
                    .append(escapeProfilePart(provider.model));
        }
        setString(KEY_AI_PROVIDERS, builder.toString());
    }

    public static void appendUpdateInstallHistory(String line) {
        if (TextUtils.isEmpty(line)) {
            return;
        }
        String current = getString(KEY_UPDATE_INSTALL_HISTORY, "");
        String entry = LocaleController.formatDateTime(System.currentTimeMillis() / 1000, true) + " - " + line.trim();
        String value = TextUtils.isEmpty(current) ? entry : entry + "\n" + current;
        String[] lines = value.split("\\n");
        StringBuilder builder = new StringBuilder();
        int count = Math.min(lines.length, 8);
        for (int i = 0; i < count; i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines[i]);
        }
        setString(KEY_UPDATE_INSTALL_HISTORY, builder.toString());
    }

    public static String getUpdateInstallHistory() {
        return getString(KEY_UPDATE_INSTALL_HISTORY, "");
    }

    public static String normalizeAiBaseUrl(String value) {
        String url = value == null ? "" : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String lower = url.toLowerCase();
        String[] suffixes = new String[] {
                "/v1/chat/completions",
                "/v1/responses",
                "/v1/models",
                "/chat/completions",
                "/responses",
                "/models",
                "/v1"
        };
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                url = url.substring(0, url.length() - suffix.length());
                break;
            }
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String providerNameForBaseUrl(String baseUrl) {
        String value = normalizeAiBaseUrl(baseUrl);
        if (TextUtils.isEmpty(value)) {
            return "NewAPI";
        }
        String host = value.replace("https://", "").replace("http://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        return TextUtils.isEmpty(host) ? "NewAPI" : host;
    }

    private static void ensureAiStatsDay(SharedPreferences preferences) {
        String today = todayKey();
        if (TextUtils.equals(preferences.getString(KEY_AI_STATS_DAY, ""), today)) {
            return;
        }
        preferences.edit()
                .putString(KEY_AI_STATS_DAY, today)
                .putInt(KEY_AI_STATS_DAY_REQUESTS, 0)
                .putInt(KEY_AI_STATS_DAY_SUCCESS, 0)
                .putInt(KEY_AI_STATS_DAY_ERRORS, 0)
                .putInt(KEY_AI_STATS_DAY_429, 0)
                .putLong(KEY_AI_STATS_DAY_LATENCY, 0)
                .apply();
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    public static boolean isLiquidGlassEnabled() {
        SharedPreferences preferences = prefs();
        return !isCrashSafeModeEnabled() && (preferences == null || preferences.getBoolean(KEY_LIQUID_GLASS, true));
    }

    public static void setLiquidGlassEnabled(boolean enabled) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply();
        }
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences preferences = prefs();
        return preferences == null ? defaultValue : preferences.getBoolean(key, defaultValue);
    }

    private static void setBoolean(String key, boolean enabled) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).apply();
        }
    }

    private static void notifyPrivacySettingsChanged() {
        AndroidUtilities.runOnUIThread(() -> {
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                if (UserConfig.getInstance(account).isClientActivated()) {
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
                }
            }
        });
    }

    private static int getInt(String key, int defaultValue) {
        SharedPreferences preferences = prefs();
        return preferences == null ? defaultValue : preferences.getInt(key, defaultValue);
    }

    private static void setInt(String key, int value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putInt(key, value).apply();
        }
    }

    private static String getString(String key, String defaultValue) {
        SharedPreferences preferences = prefs();
        return preferences == null ? defaultValue : preferences.getString(key, defaultValue);
    }

    private static void setString(String key, String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(key, value == null ? "" : value.trim()).apply();
        }
    }

    public static boolean shouldHidePhoneInSettings() {
        return getBoolean(KEY_HIDE_PHONE_IN_SETTINGS, false);
    }

    public static void setHidePhoneInSettings(boolean enabled) {
        setBoolean(KEY_HIDE_PHONE_IN_SETTINGS, enabled);
        notifyPrivacySettingsChanged();
    }

    public static boolean isAiContextMenuEnabled() {
        return getBoolean(KEY_AI_CONTEXT_MENU, true);
    }

    public static void setAiContextMenuEnabled(boolean enabled) {
        setBoolean(KEY_AI_CONTEXT_MENU, enabled);
    }

    public static boolean isAiTranslateActionEnabled() {
        return getBoolean(KEY_AI_TRANSLATE_ACTION, true);
    }

    public static void setAiTranslateActionEnabled(boolean enabled) {
        setBoolean(KEY_AI_TRANSLATE_ACTION, enabled);
    }

    public static boolean isAiInsertResultEnabled() {
        return getBoolean(KEY_AI_INSERT_RESULT, true);
    }

    public static void setAiInsertResultEnabled(boolean enabled) {
        setBoolean(KEY_AI_INSERT_RESULT, enabled);
    }

    public static boolean isQuickReplyIdeasEnabled() {
        return getBoolean(KEY_QUICK_REPLY_IDEAS, true);
    }

    public static void setQuickReplyIdeasEnabled(boolean enabled) {
        setBoolean(KEY_QUICK_REPLY_IDEAS, enabled);
    }

    public static boolean shouldAiPreferCantonese() {
        return getBoolean(KEY_AI_PREFER_CANTONESE, true);
    }

    public static void setAiPreferCantonese(boolean enabled) {
        setBoolean(KEY_AI_PREFER_CANTONESE, enabled);
    }

    public static boolean isGhostModeEnabled() {
        return getBoolean(KEY_GHOST_MODE, false);
    }

    public static void setGhostModeEnabled(boolean enabled) {
        setBoolean(KEY_GHOST_MODE, enabled);
        notifyPrivacySettingsChanged();
    }

    public static boolean isDisableReadReceiptsEnabled() {
        return getBoolean(KEY_DISABLE_READ_RECEIPTS, true);
    }

    public static boolean shouldDisableReadReceipts() {
        return isGhostModeEnabled() && isDisableReadReceiptsEnabled();
    }

    public static boolean shouldDisableReadReceipts(long dialogId) {
        return shouldDisableReadReceipts() && isGhostModeAllowedForDialog(dialogId) && !isReadReceiptsAllowedForDialog(dialogId);
    }

    public static void setDisableReadReceiptsEnabled(boolean enabled) {
        setBoolean(KEY_DISABLE_READ_RECEIPTS, enabled);
        notifyPrivacySettingsChanged();
    }

    public static boolean isReadReceiptsAllowedForDialog(long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        return getReadReceiptAllowedDialogIds().contains(String.valueOf(dialogId));
    }

    public static void setReadReceiptsAllowedForDialog(long dialogId, boolean enabled) {
        if (dialogId == 0) {
            return;
        }
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        HashSet<String> ids = new HashSet<>(getReadReceiptAllowedDialogIds());
        String key = String.valueOf(dialogId);
        if (enabled) {
            ids.add(key);
        } else {
            ids.remove(key);
        }
        preferences.edit().putStringSet(KEY_READ_RECEIPTS_ALLOWED_DIALOG_IDS, ids).apply();
        notifyPrivacySettingsChanged();
    }

    public static int getReadReceiptsAllowedDialogCount() {
        return getReadReceiptAllowedDialogIds().size();
    }

    public static Set<String> getReadReceiptAllowedDialogIdSnapshot() {
        return getReadReceiptAllowedDialogIds();
    }

    public static String exportReadReceiptAllowedDialogIds() {
        StringBuilder builder = new StringBuilder();
        for (String id : getReadReceiptAllowedDialogIds()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public static void importReadReceiptAllowedDialogIds(String value) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        HashSet<String> ids = new HashSet<>();
        if (!TextUtils.isEmpty(value)) {
            for (String id : value.split(",")) {
                String trimmed = id.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    ids.add(trimmed);
                }
            }
        }
        preferences.edit().putStringSet(KEY_READ_RECEIPTS_ALLOWED_DIALOG_IDS, ids).apply();
        notifyPrivacySettingsChanged();
    }

    private static Set<String> getReadReceiptAllowedDialogIds() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return new HashSet<>();
        }
        Set<String> ids = preferences.getStringSet(KEY_READ_RECEIPTS_ALLOWED_DIALOG_IDS, null);
        return ids == null ? new HashSet<>() : new HashSet<>(ids);
    }

    public static boolean isHideTypingStatusEnabled() {
        return getBoolean(KEY_HIDE_TYPING_STATUS, true);
    }

    public static boolean shouldHideTypingStatus() {
        return isGhostModeEnabled() && isHideTypingStatusEnabled();
    }

    public static boolean shouldHideTypingStatus(long dialogId) {
        return shouldHideTypingStatus() && isGhostModeAllowedForDialog(dialogId);
    }

    public static void setHideTypingStatusEnabled(boolean enabled) {
        setBoolean(KEY_HIDE_TYPING_STATUS, enabled);
        notifyPrivacySettingsChanged();
    }

    public static boolean isGhostSelectedChatsOnly() {
        return getBoolean(KEY_GHOST_SELECTED_CHATS_ONLY, false);
    }

    public static void setGhostSelectedChatsOnly(boolean enabled) {
        setBoolean(KEY_GHOST_SELECTED_CHATS_ONLY, enabled);
        notifyPrivacySettingsChanged();
    }

    public static boolean isGhostModeAllowedForDialog(long dialogId) {
        return !isGhostSelectedChatsOnly() || isGhostModeForDialog(dialogId);
    }

    public static boolean isGhostModeForDialog(long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        return getGhostDialogIds().contains(String.valueOf(dialogId));
    }

    public static void setGhostModeForDialog(long dialogId, boolean enabled) {
        if (dialogId == 0) {
            return;
        }
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        HashSet<String> ids = new HashSet<>(getGhostDialogIds());
        String key = String.valueOf(dialogId);
        if (enabled) {
            ids.add(key);
        } else {
            ids.remove(key);
        }
        preferences.edit().putStringSet(KEY_GHOST_DIALOG_IDS, ids).apply();
        notifyPrivacySettingsChanged();
    }

    public static int getGhostDialogCount() {
        return getGhostDialogIds().size();
    }

    public static Set<String> getGhostDialogIdSnapshot() {
        return getGhostDialogIds();
    }

    public static String exportGhostDialogIds() {
        StringBuilder builder = new StringBuilder();
        for (String id : getGhostDialogIds()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public static void importGhostDialogIds(String value) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        HashSet<String> ids = new HashSet<>();
        if (!TextUtils.isEmpty(value)) {
            for (String id : value.split(",")) {
                String trimmed = id.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    ids.add(trimmed);
                }
            }
        }
        preferences.edit().putStringSet(KEY_GHOST_DIALOG_IDS, ids).apply();
        notifyPrivacySettingsChanged();
    }

    private static Set<String> getGhostDialogIds() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return new HashSet<>();
        }
        Set<String> ids = preferences.getStringSet(KEY_GHOST_DIALOG_IDS, null);
        return ids == null ? new HashSet<>() : new HashSet<>(ids);
    }

    public static boolean shouldKeepDeletedMessages() {
        return getBoolean(KEY_KEEP_DELETED_MESSAGES, false);
    }

    public static void setKeepDeletedMessages(boolean enabled) {
        setBoolean(KEY_KEEP_DELETED_MESSAGES, enabled);
    }

    public static boolean shouldKeepOriginalEdits() {
        return getBoolean(KEY_KEEP_ORIGINAL_EDITS, false);
    }

    public static void setKeepOriginalEdits(boolean enabled) {
        setBoolean(KEY_KEEP_ORIGINAL_EDITS, enabled);
    }

    public static boolean isLiquidGlassStrongEnabled() {
        return getLiquidGlassLevel() >= 2 || getBoolean(KEY_LIQUID_GLASS_STRONG, false);
    }

    public static void setLiquidGlassStrongEnabled(boolean enabled) {
        setBoolean(KEY_LIQUID_GLASS_STRONG, enabled);
        if (enabled && getLiquidGlassLevel() < 2) {
            setLiquidGlassLevel(2);
        }
    }

    public static int getLiquidGlassLevel() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return 1;
        }
        int level = preferences.getInt(KEY_LIQUID_GLASS_LEVEL, getBoolean(KEY_LIQUID_GLASS_STRONG, false) ? 2 : 1);
        if (level < 0) {
            return 0;
        }
        return Math.min(level, 2);
    }

    public static void setLiquidGlassLevel(int level) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            int normalized = Math.max(0, Math.min(level, 2));
            preferences.edit().putInt(KEY_LIQUID_GLASS_LEVEL, normalized).putBoolean(KEY_LIQUID_GLASS_STRONG, normalized >= 2).apply();
        }
    }

    public static boolean isLiquidGlassCustomEnabled() {
        return getBoolean(KEY_LIQUID_GLASS_CUSTOM, false);
    }

    public static void setLiquidGlassCustomEnabled(boolean enabled) {
        setBoolean(KEY_LIQUID_GLASS_CUSTOM, enabled);
    }

    public static int getLiquidGlassThicknessDp() {
        return Math.max(4, Math.min(getInt(KEY_LIQUID_GLASS_THICKNESS_DP, 11), 32));
    }

    public static void setLiquidGlassThicknessDp(int value) {
        setInt(KEY_LIQUID_GLASS_THICKNESS_DP, Math.max(4, Math.min(value, 32)));
    }

    public static int getLiquidGlassIntensityPermille() {
        return Math.max(250, Math.min(getInt(KEY_LIQUID_GLASS_INTENSITY_PERMILLE, 750), 1200));
    }

    public static void setLiquidGlassIntensityPermille(int value) {
        setInt(KEY_LIQUID_GLASS_INTENSITY_PERMILLE, Math.max(250, Math.min(value, 1200)));
    }

    public static int getLiquidGlassIndexPermille() {
        return Math.max(1000, Math.min(getInt(KEY_LIQUID_GLASS_INDEX_PERMILLE, 1500), 2200));
    }

    public static void setLiquidGlassIndexPermille(int value) {
        setInt(KEY_LIQUID_GLASS_INDEX_PERMILLE, Math.max(1000, Math.min(value, 2200)));
    }

    public static void resetLiquidGlassStudio() {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .putBoolean(KEY_LIQUID_GLASS_CUSTOM, false)
                    .putInt(KEY_LIQUID_GLASS_THICKNESS_DP, 11)
                    .putInt(KEY_LIQUID_GLASS_INTENSITY_PERMILLE, 750)
                    .putInt(KEY_LIQUID_GLASS_INDEX_PERMILLE, 1500)
                    .apply();
        }
    }

    public static boolean isDownloadBoostEnabled() {
        return !isCrashSafeModeEnabled() && getBoolean(KEY_DOWNLOAD_BOOST, false);
    }

    public static void setDownloadBoostEnabled(boolean enabled) {
        setBoolean(KEY_DOWNLOAD_BOOST, enabled);
    }

    public static boolean isDownloadBoostAutoEnabled() {
        return isDownloadBoostEnabled() && getBoolean(KEY_DOWNLOAD_BOOST_AUTO, true);
    }

    public static void setDownloadBoostAutoEnabled(boolean enabled) {
        setBoolean(KEY_DOWNLOAD_BOOST_AUTO, enabled);
    }

    public static int getDownloadBoostLevel() {
        return Math.max(0, Math.min(getInt(KEY_DOWNLOAD_BOOST_LEVEL, 1), 2));
    }

    public static void setDownloadBoostLevel(int level) {
        setInt(KEY_DOWNLOAD_BOOST_LEVEL, Math.max(0, Math.min(level, 2)));
    }

    public static int getEffectiveDownloadBoostLevel() {
        if (!isDownloadBoostEnabled()) {
            return 0;
        }
        if (!isDownloadBoostAutoEnabled()) {
            return getDownloadBoostLevel();
        }
        SingGramDownloadStats.Snapshot snapshot = SingGramDownloadStats.getSnapshot();
        if (snapshot.activeCount >= 3 || snapshot.speedBytesPerSecond >= 4L * 1024L * 1024L) {
            return 2;
        }
        if (snapshot.activeCount >= 2 || snapshot.speedBytesPerSecond >= 1024L * 1024L) {
            return 1;
        }
        return 0;
    }

    public static int getBoostedSmallQueueMaxActiveOperations(int defaultValue) {
        if (!isDownloadBoostEnabled()) {
            return defaultValue;
        }
        int level = getEffectiveDownloadBoostLevel();
        int target = level == 0 ? 7 : level == 1 ? 9 : 10;
        return Math.max(defaultValue, target);
    }

    public static int getBoostedLargeQueueMaxActiveOperations(int defaultValue) {
        if (!isDownloadBoostEnabled()) {
            return defaultValue;
        }
        int level = getEffectiveDownloadBoostLevel();
        int target = level == 0 ? 3 : level == 1 ? 4 : 5;
        return Math.max(defaultValue, target);
    }

    public static int getBoostedDownloadRequestCount(int defaultValue) {
        if (!isDownloadBoostEnabled()) {
            return defaultValue;
        }
        int level = getEffectiveDownloadBoostLevel();
        int target = level == 0 ? 8 : level == 1 ? 10 : 12;
        return Math.max(defaultValue, target);
    }

    public static String getAccountProfileLabel(int account) {
        return getString(KEY_ACCOUNT_PROFILE_LABEL + account, "");
    }

    public static void setAccountProfileLabel(int account, String value) {
        setString(KEY_ACCOUNT_PROFILE_LABEL + account, value);
    }

    public static String getAccountProfileGroup(int account) {
        return getString(KEY_ACCOUNT_PROFILE_GROUP + account, "");
    }

    public static void setAccountProfileGroup(int account, String value) {
        setString(KEY_ACCOUNT_PROFILE_GROUP + account, value);
    }

    public static int getAccountProfileColor(int account) {
        return Math.max(0, Math.min(getInt(KEY_ACCOUNT_PROFILE_COLOR + account, 0), 7));
    }

    public static void setAccountProfileColor(int account, int value) {
        setInt(KEY_ACCOUNT_PROFILE_COLOR + account, Math.max(0, Math.min(value, 7)));
    }

    public static String exportAccountProfiles() {
        StringBuilder builder = new StringBuilder();
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            String label = getAccountProfileLabel(account);
            String group = getAccountProfileGroup(account);
            int color = getAccountProfileColor(account);
            if (TextUtils.isEmpty(label) && TextUtils.isEmpty(group) && color == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(account).append('|').append(escapeProfilePart(label)).append('|').append(escapeProfilePart(group)).append('|').append(color);
        }
        return builder.toString();
    }

    public static void importAccountProfiles(String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        for (String line : value.split("\\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                continue;
            }
            try {
                int account = Integer.parseInt(parts[0]);
                if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
                    continue;
                }
                setAccountProfileLabel(account, unescapeProfilePart(parts[1]));
                setAccountProfileGroup(account, unescapeProfilePart(parts[2]));
                setAccountProfileColor(account, Integer.parseInt(parts[3]));
            } catch (Exception ignore) {

            }
        }
    }

    private static String escapeProfilePart(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    private static String unescapeProfilePart(String value) {
        return value == null ? "" : value.replace("\\n", "\n").replace("\\p", "|").replace("\\\\", "\\");
    }

    public static boolean isDiagnosticsEnabled() {
        return getBoolean(KEY_SHOW_DIAGNOSTICS, true);
    }

    public static void setDiagnosticsEnabled(boolean enabled) {
        setBoolean(KEY_SHOW_DIAGNOSTICS, enabled);
    }

    public static void setLastUpdateCheck(int versionCode, String versionName) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .putInt(KEY_LAST_UPDATE_VERSION_CODE, versionCode)
                    .putString(KEY_LAST_UPDATE_VERSION_NAME, versionName == null ? "" : versionName)
                    .putLong(KEY_LAST_UPDATE_CHECK_TIME, System.currentTimeMillis())
                    .apply();
        }
    }

    public static int getLastUpdateVersionCode() {
        return getInt(KEY_LAST_UPDATE_VERSION_CODE, 0);
    }

    public static String getLastUpdateVersionName() {
        return getString(KEY_LAST_UPDATE_VERSION_NAME, "");
    }

    public static long getLastUpdateCheckTime() {
        SharedPreferences preferences = prefs();
        return preferences == null ? 0 : preferences.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0);
    }

    public static void setLastUpdateApkPath(String path) {
        setString(KEY_LAST_UPDATE_APK_PATH, path);
    }

    public static String getLastUpdateApkPath() {
        return getString(KEY_LAST_UPDATE_APK_PATH, "");
    }

    public static void clearLastUpdateApkPath() {
        setString(KEY_LAST_UPDATE_APK_PATH, "");
    }

    public static void setPendingUpdateInstall(boolean pending) {
        setBoolean(KEY_PENDING_UPDATE_INSTALL, pending);
    }

    public static boolean hasPendingUpdateInstall() {
        return getBoolean(KEY_PENDING_UPDATE_INSTALL, false);
    }

    public static boolean shouldShowFeatureHubIntro() {
        SharedPreferences preferences = prefs();
        return preferences != null && preferences.getInt(KEY_LAST_FEATURE_HUB_INTRO_BUILD, 0) < SharedConfig.buildVersion();
    }

    public static void markFeatureHubIntroShown() {
        setInt(KEY_LAST_FEATURE_HUB_INTRO_BUILD, SharedConfig.buildVersion());
    }

    public static boolean isCrashSafeModeEnabled() {
        return getBoolean(KEY_CRASH_SAFE_MODE, false);
    }

    public static void setCrashSafeModeEnabled(boolean enabled) {
        setBoolean(KEY_CRASH_SAFE_MODE, enabled);
    }

    public static void applyCrashRecoveryPreset() {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .putBoolean(KEY_CRASH_SAFE_MODE, true)
                    .putBoolean(KEY_AI_ENABLED, false)
                    .putBoolean(KEY_LIQUID_GLASS, false)
                    .putBoolean(KEY_DOWNLOAD_BOOST, false)
                    .apply();
        }
    }

    public static long getLastCrashTime() {
        SharedPreferences preferences = prefs();
        return preferences == null ? 0 : preferences.getLong(KEY_LAST_CRASH_TIME, 0);
    }

    public static String getLastCrashReason() {
        return getString(KEY_LAST_CRASH_REASON, "");
    }

    public static void clearLastCrash() {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().remove(KEY_LAST_CRASH_TIME).remove(KEY_LAST_CRASH_REASON).apply();
        }
    }

    public static void installCrashSafeHandler() {
        if (crashHandlerInstalled || ApplicationLoader.applicationContext == null) {
            return;
        }
        crashHandlerInstalled = true;
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            recordCrashForSafeMode(throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    private static void recordCrashForSafeMode(Throwable throwable) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        String reason = throwable == null ? "" : throwable.getClass().getSimpleName();
        String message = throwable == null ? "" : throwable.getMessage();
        if (!TextUtils.isEmpty(message)) {
            reason += ": " + message;
        }
        preferences.edit()
                .putBoolean(KEY_CRASH_SAFE_MODE, true)
                .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
                .putString(KEY_LAST_CRASH_REASON, reason)
                .commit();
    }
}

package org.telegram.messenger;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SingGramUpdateClient {

    public static final String DEFAULT_UPDATE_URL = "https://github.com/sing0626/SingGram/releases/latest/download/update.json";
    public static final String DEFAULT_RELEASE_URL = "https://github.com/sing0626/SingGram/releases/latest";

    public interface Callback {
        void onResult(UpdateInfo info);
        void onError(String error);
    }

    public static class UpdateInfo {
        public int versionCode;
        public String versionName;
        public String apkUrl;
        public String sha256;
        public long apkSizeBytes;
        public String notes;
        public String publishedAt;

        public boolean hasUpdate() {
            return versionCode > 0 && versionCode > SharedConfig.buildVersion();
        }

        public String summary() {
            String version = TextUtils.isEmpty(versionName) ? "SingGram" : versionName;
            return version + " / " + statusText();
        }

        public String statusText() {
            int current = SharedConfig.buildVersion();
            if (versionCode <= 0) {
                return LocaleController.getString(R.string.SingGramUpdateNotChecked);
            } else if (versionCode > current) {
                return LocaleController.getString(R.string.SingGramUpdateAvailable);
            } else if (versionCode == current) {
                return LocaleController.getString(R.string.SingGramUpdateSameVersion);
            }
            return LocaleController.getString(R.string.SingGramUpdateInstalledNewer);
        }
    }

    public static void check(Callback callback) {
        if (callback == null) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(DEFAULT_UPDATE_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Accept", "application/json");
                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readStream(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    callbackOnError(callback, "HTTP " + responseCode + (TextUtils.isEmpty(response) ? "" : ": " + response));
                    return;
                }
                callbackOnResult(callback, parse(response));
            } catch (Exception e) {
                FileLog.e(e);
                callbackOnError(callback, e.getMessage());
            }
        });
    }

    private static UpdateInfo parse(String response) throws Exception {
        JSONObject object = new JSONObject(response);
        UpdateInfo info = new UpdateInfo();
        info.versionCode = object.optInt("versionCode", 0);
        info.versionName = object.optString("versionName", "");
        info.apkUrl = object.optString("apkUrl", "");
        info.sha256 = object.optString("sha256", "");
        info.apkSizeBytes = object.optLong("apkSizeBytes", 0);
        info.publishedAt = object.optString("publishedAt", "");

        String notesText = object.optString("notesText", "");
        if (TextUtils.isEmpty(notesText)) {
            notesText = object.optString("notes", "");
        }
        JSONArray changes = object.optJSONArray("changes");
        if (changes != null && changes.length() > 0) {
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(notesText)) {
                builder.append(notesText.trim()).append("\n\n");
            }
            for (int i = 0; i < changes.length(); i++) {
                String change = changes.optString(i, "");
                if (!TextUtils.isEmpty(change)) {
                    builder.append("- ").append(change.trim()).append('\n');
                }
            }
            notesText = builder.toString().trim();
        }
        info.notes = notesText;
        return info;
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static void callbackOnResult(Callback callback, UpdateInfo info) {
        AndroidUtilities.runOnUIThread(() -> callback.onResult(info));
    }

    private static void callbackOnError(Callback callback, String error) {
        AndroidUtilities.runOnUIThread(() -> callback.onError(TextUtils.isEmpty(error) ? LocaleController.getString(R.string.ErrorOccurred) : error));
    }
}

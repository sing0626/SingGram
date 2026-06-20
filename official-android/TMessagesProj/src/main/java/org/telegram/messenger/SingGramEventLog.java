package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SingGramEventLog {

    private static final String PREFS_NAME = "singgram";
    private static final String KEY_EVENT_LOG = "event_log";
    private static final int MAX_LINES = 120;

    private static SharedPreferences prefs() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void logDeletedMessages(long dialogId, ArrayList<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        append("delete", dialogId, "ids=" + TextUtils.join(",", messageIds));
    }

    public static void logEditedMessage(long dialogId, int messageId, String newText) {
        append("edit", dialogId, "id=" + messageId + " new=" + sanitize(newText));
    }

    public static String getLogText() {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_EVENT_LOG, "");
    }

    public static int getEventCount() {
        String log = getLogText();
        if (TextUtils.isEmpty(log)) {
            return 0;
        }
        int count = 0;
        for (String line : log.split("\\n")) {
            if (!TextUtils.isEmpty(line.trim())) {
                count++;
            }
        }
        return count;
    }

    public static void clear() {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().remove(KEY_EVENT_LOG).apply();
        }
    }

    private static void append(String type, long dialogId, String detail) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        ArrayList<String> lines = new ArrayList<>();
        String existing = preferences.getString(KEY_EVENT_LOG, "");
        if (!TextUtils.isEmpty(existing)) {
            for (String line : existing.split("\\n")) {
                if (!TextUtils.isEmpty(line.trim())) {
                    lines.add(line);
                }
            }
        }
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        lines.add(0, time + " | " + type + " | dialog=" + dialogId + " | " + detail);
        while (lines.size() > MAX_LINES) {
            lines.remove(lines.size() - 1);
        }
        preferences.edit().putString(KEY_EVENT_LOG, TextUtils.join("\n", lines)).apply();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('|', ' ').trim();
    }
}

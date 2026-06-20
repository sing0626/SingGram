package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SingGramChatNotesStore {

    private static final String PREFS_NAME = "singgram";
    private static final String KEY_NOTE = "chat_note_";
    private static final String KEY_TAGS = "chat_tags_";
    private static final String KEY_REMINDER = "chat_reminder_";

    private static SharedPreferences prefs() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getNote(long dialogId) {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_NOTE + dialogId, "");
    }

    public static void setNote(long dialogId, String value) {
        setString(KEY_NOTE + dialogId, value);
    }

    public static String getTags(long dialogId) {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_TAGS + dialogId, "");
    }

    public static void setTags(long dialogId, String value) {
        setString(KEY_TAGS + dialogId, normalizeTags(value));
    }

    public static String getReminder(long dialogId) {
        SharedPreferences preferences = prefs();
        return preferences == null ? "" : preferences.getString(KEY_REMINDER + dialogId, "");
    }

    public static void setReminder(long dialogId, String value) {
        setString(KEY_REMINDER + dialogId, value);
    }

    public static boolean hasNotes(long dialogId) {
        return !TextUtils.isEmpty(getNote(dialogId)) || !TextUtils.isEmpty(getTags(dialogId)) || !TextUtils.isEmpty(getReminder(dialogId));
    }

    public static String exportNote(long dialogId) {
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram chat notes\n");
        builder.append("dialog: ").append(dialogId).append('\n');
        builder.append("tags: ").append(getTags(dialogId)).append('\n');
        builder.append("reminder: ").append(getReminder(dialogId)).append('\n');
        builder.append("note:\n").append(getNote(dialogId));
        return builder.toString();
    }

    public static String exportAllNotesForBundle() {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof String) || TextUtils.isEmpty((String) value)) {
                continue;
            }
            String bundleKey = null;
            if (key.startsWith(KEY_NOTE)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_NOTE.length()) + ".note.b64";
            } else if (key.startsWith(KEY_TAGS)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_TAGS.length()) + ".tags.b64";
            } else if (key.startsWith(KEY_REMINDER)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_REMINDER.length()) + ".reminder.b64";
            }
            if (bundleKey == null) {
                continue;
            }
            builder.append(bundleKey).append(": ").append(encode((String) value)).append('\n');
        }
        return builder.toString();
    }

    public static boolean importBundleLine(String key, String value) {
        if (TextUtils.isEmpty(key) || !key.startsWith("sg.chat_note.")) {
            return false;
        }
        int start = "sg.chat_note.".length();
        int end = key.indexOf('.', start);
        if (end <= start) {
            return false;
        }
        String dialogId = key.substring(start, end);
        String field = key.substring(end + 1);
        String decoded = decode(value);
        if (decoded == null) {
            return false;
        }
        if ("note.b64".equals(field)) {
            setString(KEY_NOTE + dialogId, decoded);
            return true;
        } else if ("tags.b64".equals(field)) {
            setString(KEY_TAGS + dialogId, normalizeTags(decoded));
            return true;
        } else if ("reminder.b64".equals(field)) {
            setString(KEY_REMINDER + dialogId, decoded);
            return true;
        }
        return false;
    }

    public static void clear(long dialogId) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit()
                    .remove(KEY_NOTE + dialogId)
                    .remove(KEY_TAGS + dialogId)
                    .remove(KEY_REMINDER + dialogId)
                    .apply();
        }
    }

    private static void setString(String key, String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(key, value == null ? "" : value.trim()).apply();
        }
    }

    private static String normalizeTags(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value.replace('\n', ',').replace('\uFF0C', ',').trim();
    }

    private static String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String decode(String value) {
        try {
            return new String(Base64.decode(value, Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            return null;
        }
    }
}

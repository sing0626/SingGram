package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class SingGramChatNotesStore {

    private static final String PREFS_NAME = "singgram";
    private static final String KEY_NOTE = "chat_note_";
    private static final String KEY_TAGS = "chat_tags_";
    private static final String KEY_REMINDER = "chat_reminder_";
    private static final String KEY_FOLLOW_UP_DUE = "chat_follow_up_due_";
    private static final String KEY_FOLLOW_UP_COMPLETE = "chat_follow_up_complete_";
    private static final String KEY_FOLLOW_UP_ACCOUNT = "chat_follow_up_account_";

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

    public static long getFollowUpDueAt(long dialogId) {
        SharedPreferences preferences = prefs();
        return preferences == null ? 0 : preferences.getLong(KEY_FOLLOW_UP_DUE + dialogId, 0);
    }

    public static void setFollowUpDueAt(long dialogId, long dueAt) {
        setFollowUpDueAt(dialogId, getFollowUpAccount(dialogId), dueAt);
    }

    public static void setFollowUpDueAt(long dialogId, int account, long dueAt) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        int previousAccount = getFollowUpAccount(dialogId);
        SharedPreferences.Editor editor = preferences.edit();
        if (dueAt > 0) {
            editor.putLong(KEY_FOLLOW_UP_DUE + dialogId, dueAt);
            editor.putBoolean(KEY_FOLLOW_UP_COMPLETE + dialogId, false);
            editor.putInt(KEY_FOLLOW_UP_ACCOUNT + dialogId, account);
        } else {
            editor.remove(KEY_FOLLOW_UP_DUE + dialogId).remove(KEY_FOLLOW_UP_COMPLETE + dialogId).remove(KEY_FOLLOW_UP_ACCOUNT + dialogId);
        }
        editor.apply();
        if (dueAt > 0) {
            if (previousAccount != account) {
                SingGramFollowUpReceiver.cancel(ApplicationLoader.applicationContext, dialogId, previousAccount);
            }
            SingGramFollowUpReceiver.schedule(ApplicationLoader.applicationContext, dialogId, account, dueAt);
        } else {
            SingGramFollowUpReceiver.cancel(ApplicationLoader.applicationContext, dialogId, previousAccount);
        }
    }

    public static int getFollowUpAccount(long dialogId) {
        SharedPreferences preferences = prefs();
        int account = preferences == null ? UserConfig.selectedAccount : preferences.getInt(KEY_FOLLOW_UP_ACCOUNT + dialogId, UserConfig.selectedAccount);
        return account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT ? account : UserConfig.selectedAccount;
    }

    public static boolean isFollowUpComplete(long dialogId) {
        SharedPreferences preferences = prefs();
        return preferences != null && preferences.getBoolean(KEY_FOLLOW_UP_COMPLETE + dialogId, false);
    }

    public static void setFollowUpComplete(long dialogId, boolean complete) {
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_FOLLOW_UP_COMPLETE + dialogId, complete).apply();
        long dueAt = getFollowUpDueAt(dialogId);
        int account = getFollowUpAccount(dialogId);
        if (complete || dueAt <= 0) {
            SingGramFollowUpReceiver.cancel(ApplicationLoader.applicationContext, dialogId, account);
        } else {
            SingGramFollowUpReceiver.schedule(ApplicationLoader.applicationContext, dialogId, account, dueAt);
        }
    }

    public static void rescheduleFollowUps() {
        for (Long dialogId : getNotedDialogIds()) {
            long dueAt = getFollowUpDueAt(dialogId);
            if (dueAt > 0 && !isFollowUpComplete(dialogId)) {
                SingGramFollowUpReceiver.schedule(ApplicationLoader.applicationContext, dialogId, getFollowUpAccount(dialogId), dueAt);
            }
        }
    }

    public static boolean hasNotes(long dialogId) {
        return !TextUtils.isEmpty(getNote(dialogId)) || !TextUtils.isEmpty(getTags(dialogId)) || !TextUtils.isEmpty(getReminder(dialogId)) || getFollowUpDueAt(dialogId) > 0;
    }

    public static int getNotesCount() {
        return getNotedDialogIds().size();
    }

    public static ArrayList<Long> getNotedDialogIds() {
        SharedPreferences preferences = prefs();
        ArrayList<Long> dialogIds = new ArrayList<>();
        if (preferences == null) {
            return dialogIds;
        }
        for (String key : preferences.getAll().keySet()) {
            String id = null;
            if (key.startsWith(KEY_NOTE)) {
                id = key.substring(KEY_NOTE.length());
            } else if (key.startsWith(KEY_TAGS)) {
                id = key.substring(KEY_TAGS.length());
            } else if (key.startsWith(KEY_REMINDER)) {
                id = key.substring(KEY_REMINDER.length());
            } else if (key.startsWith(KEY_FOLLOW_UP_DUE)) {
                id = key.substring(KEY_FOLLOW_UP_DUE.length());
            } else if (key.startsWith(KEY_FOLLOW_UP_COMPLETE)) {
                id = key.substring(KEY_FOLLOW_UP_COMPLETE.length());
            } else if (key.startsWith(KEY_FOLLOW_UP_ACCOUNT)) {
                id = key.substring(KEY_FOLLOW_UP_ACCOUNT.length());
            }
            if (TextUtils.isEmpty(id)) {
                continue;
            }
            try {
                long dialogId = Long.parseLong(id);
                if (hasNotes(dialogId) && !dialogIds.contains(dialogId)) {
                    dialogIds.add(dialogId);
                }
            } catch (Exception ignore) {

            }
        }
        Collections.sort(dialogIds);
        return dialogIds;
    }

    public static String exportNote(long dialogId) {
        StringBuilder builder = new StringBuilder();
        builder.append("SingGram chat notes\n");
        builder.append("dialog: ").append(dialogId).append('\n');
        builder.append("tags: ").append(getTags(dialogId)).append('\n');
        builder.append("reminder: ").append(getReminder(dialogId)).append('\n');
        builder.append("follow_up_due: ").append(getFollowUpDueAt(dialogId)).append('\n');
        builder.append("follow_up_complete: ").append(isFollowUpComplete(dialogId)).append('\n');
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
            String bundleKey = null;
            if (key.startsWith(KEY_NOTE)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_NOTE.length()) + ".note.b64";
            } else if (key.startsWith(KEY_TAGS)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_TAGS.length()) + ".tags.b64";
            } else if (key.startsWith(KEY_REMINDER)) {
                bundleKey = "sg.chat_note." + key.substring(KEY_REMINDER.length()) + ".reminder.b64";
            }
            if (bundleKey != null && value instanceof String && !TextUtils.isEmpty((String) value)) {
                builder.append(bundleKey).append(": ").append(encode((String) value)).append('\n');
            } else if (key.startsWith(KEY_FOLLOW_UP_DUE) && value instanceof Long && (Long) value > 0) {
                builder.append("sg.chat_note.").append(key.substring(KEY_FOLLOW_UP_DUE.length())).append(".follow_up_due: ").append(value).append('\n');
            } else if (key.startsWith(KEY_FOLLOW_UP_COMPLETE) && value instanceof Boolean) {
                builder.append("sg.chat_note.").append(key.substring(KEY_FOLLOW_UP_COMPLETE.length())).append(".follow_up_complete: ").append(value).append('\n');
            } else if (key.startsWith(KEY_FOLLOW_UP_ACCOUNT) && value instanceof Integer) {
                builder.append("sg.chat_note.").append(key.substring(KEY_FOLLOW_UP_ACCOUNT.length())).append(".follow_up_account: ").append(value).append('\n');
            }
        }
        return builder.toString();
    }

    public static String exportAllNotesText() {
        StringBuilder builder = new StringBuilder("SingGram chat notes\n");
        ArrayList<Long> dialogIds = getNotedDialogIds();
        for (int i = 0; i < dialogIds.size(); i++) {
            long dialogId = dialogIds.get(i);
            if (i > 0) {
                builder.append("\n\n---\n");
            }
            builder.append(exportNote(dialogId));
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
        if ("note.b64".equals(field)) {
            String decoded = decode(value);
            if (decoded == null) return false;
            setString(KEY_NOTE + dialogId, decoded);
            return true;
        } else if ("tags.b64".equals(field)) {
            String decoded = decode(value);
            if (decoded == null) return false;
            setString(KEY_TAGS + dialogId, normalizeTags(decoded));
            return true;
        } else if ("reminder.b64".equals(field)) {
            String decoded = decode(value);
            if (decoded == null) return false;
            setString(KEY_REMINDER + dialogId, decoded);
            return true;
        } else if ("follow_up_due".equals(field)) {
            try {
                setFollowUpDueAt(Long.parseLong(dialogId), Long.parseLong(value));
                return true;
            } catch (Exception ignore) {
                return false;
            }
        } else if ("follow_up_complete".equals(field)) {
            try {
                setFollowUpComplete(Long.parseLong(dialogId), Boolean.parseBoolean(value));
                return true;
            } catch (Exception ignore) {
                return false;
            }
        } else if ("follow_up_account".equals(field)) {
            try {
                return setFollowUpAccount(Long.parseLong(dialogId), Integer.parseInt(value));
            } catch (Exception ignore) {
                return false;
            }
        }
        return false;
    }

    public static void clear(long dialogId) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            int account = getFollowUpAccount(dialogId);
            preferences.edit()
                    .remove(KEY_NOTE + dialogId)
                    .remove(KEY_TAGS + dialogId)
                    .remove(KEY_REMINDER + dialogId)
                    .remove(KEY_FOLLOW_UP_DUE + dialogId)
                    .remove(KEY_FOLLOW_UP_COMPLETE + dialogId)
                    .remove(KEY_FOLLOW_UP_ACCOUNT + dialogId)
                    .apply();
            SingGramFollowUpReceiver.cancel(ApplicationLoader.applicationContext, dialogId, account);
        }
    }

    private static void setString(String key, String value) {
        SharedPreferences preferences = prefs();
        if (preferences != null) {
            preferences.edit().putString(key, value == null ? "" : value.trim()).apply();
        }
    }

    private static boolean setFollowUpAccount(long dialogId, int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            return false;
        }
        SharedPreferences preferences = prefs();
        if (preferences == null) {
            return false;
        }
        int previousAccount = getFollowUpAccount(dialogId);
        preferences.edit().putInt(KEY_FOLLOW_UP_ACCOUNT + dialogId, account).apply();
        long dueAt = getFollowUpDueAt(dialogId);
        if (dueAt > 0 && !isFollowUpComplete(dialogId)) {
            if (previousAccount != account) {
                SingGramFollowUpReceiver.cancel(ApplicationLoader.applicationContext, dialogId, previousAccount);
            }
            SingGramFollowUpReceiver.schedule(ApplicationLoader.applicationContext, dialogId, account, dueAt);
        }
        return true;
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

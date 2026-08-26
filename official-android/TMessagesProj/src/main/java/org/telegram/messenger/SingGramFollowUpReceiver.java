package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.ui.LaunchActivity;

public class SingGramFollowUpReceiver extends BroadcastReceiver {

    private static final String ACTION_DUE = "org.telegram.messenger.SINGGRAM_FOLLOW_UP_DUE";
    private static final String EXTRA_DIALOG_ID = "dialog_id";
    private static final String EXTRA_ACCOUNT = "current_account";
    private static final int NOTIFICATION_ID_BASE = 73000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_DUE.equals(intent.getAction())) {
            return;
        }
        long dialogId = intent.getLongExtra(EXTRA_DIALOG_ID, 0);
        int account = intent.getIntExtra(EXTRA_ACCOUNT, SingGramChatNotesStore.getFollowUpAccount(dialogId));
        long dueAt = SingGramChatNotesStore.getFollowUpDueAt(dialogId);
        if (dialogId == 0 || dueAt == 0 || dueAt > System.currentTimeMillis() || SingGramChatNotesStore.isFollowUpComplete(dialogId)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        NotificationsController.checkOtherNotificationsChannel();
        Intent openIntent = new Intent(appContext, LaunchActivity.class)
                .setAction("com.tmessages.openchat.singgram_followup")
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        putDialogTarget(openIntent, dialogId);
        openIntent.putExtra("currentAccount", account);
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, requestCode(dialogId, account), openIntent, pendingIntentFlags());
        NotificationCompat.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new NotificationCompat.Builder(appContext, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL)
                : new NotificationCompat.Builder(appContext);
        builder.setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.getString(R.string.SingGramFollowUpNotificationTitle))
                .setContentText(LocaleController.getString(R.string.SingGramFollowUpNotificationText))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(LocaleController.getString(R.string.SingGramFollowUpNotificationText)))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(appContext).notify(notificationId(dialogId, account), builder.build());
        } catch (Throwable ignore) {
        }
    }

    public static void schedule(Context context, long dialogId, int account, long dueAt) {
        if (context == null || dialogId == 0) {
            return;
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pendingIntent = pendingIntent(context, dialogId, account);
        manager.cancel(pendingIntent);
        if (dueAt <= 0) {
            return;
        }
        long triggerAt = Math.max(dueAt, System.currentTimeMillis() + 1000L);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    public static void cancel(Context context, long dialogId, int account) {
        if (context == null || dialogId == 0) {
            return;
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            manager.cancel(pendingIntent(context, dialogId, account));
        }
    }

    private static PendingIntent pendingIntent(Context context, long dialogId, int account) {
        Intent intent = new Intent(context, SingGramFollowUpReceiver.class)
                .setAction(ACTION_DUE)
                .putExtra(EXTRA_DIALOG_ID, dialogId)
                .putExtra(EXTRA_ACCOUNT, account);
        return PendingIntent.getBroadcast(context, requestCode(dialogId, account), intent, pendingIntentFlags());
    }

    private static void putDialogTarget(Intent intent, long dialogId) {
        if (DialogObject.isEncryptedDialog(dialogId)) {
            intent.putExtra("encId", DialogObject.getEncryptedChatId(dialogId));
        } else if (DialogObject.isUserDialog(dialogId)) {
            intent.putExtra("userId", dialogId);
        } else {
            intent.putExtra("chatId", -dialogId);
        }
    }

    private static int requestCode(long dialogId, int account) {
        return 61000 + (31 * Long.hashCode(dialogId) + account & 0x1fff);
    }

    private static int notificationId(long dialogId, int account) {
        return NOTIFICATION_ID_BASE + (31 * Long.hashCode(dialogId) + account & 0x1fff);
    }

    private static int pendingIntentFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
    }
}

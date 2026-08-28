package org.telegram.messenger.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by grishka on 28.07.17.
 */

public class VoIPActionsReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// Android can deliver an old notification action after a newer call has arrived.
		// Never let that stale action decline or end the current call.
		if (intent.hasExtra("call_id") && !isMatchingCall(intent.getLongExtra("call_id", 0))) {
			return;
		}
		if (!intent.hasExtra("group_call_invite_msg_id") && VoIPService.getSharedInstance() != null) {
			VoIPService.getSharedInstance().handleNotificationAction(intent);
		} else {
			final String packageName = context.getPackageName();
			if ((packageName + ".END_CALL").equals(intent.getAction())) {
				if (intent.hasExtra("group_call_invite_msg_id")) {
					VoIPGroupNotification.decline(context, intent.getIntExtra("currentAccount", 0), intent.getIntExtra("group_call_invite_msg_id", 0));
				} else {
					VoIPPreNotificationService.decline(context, VoIPService.DISCARD_REASON_HANGUP);
				}
			} else if ((packageName + ".DECLINE_CALL").equals(intent.getAction())) {
				if (intent.hasExtra("group_call_invite_msg_id")) {
					VoIPGroupNotification.decline(context, intent.getIntExtra("currentAccount", 0), intent.getIntExtra("group_call_invite_msg_id", 0));
				} else {
					VoIPPreNotificationService.decline(context, VoIPService.DISCARD_REASON_LINE_BUSY);
				}
			} else if ((packageName + ".ANSWER_CALL").equals(intent.getAction())) {
				if (intent.hasExtra("group_call_invite_msg_id")) {
					VoIPGroupNotification.answer(context, intent.getIntExtra("currentAccount", 0), intent.getIntExtra("group_call_invite_msg_id", 0));
				} else {
					VoIPPreNotificationService.answer(context);
				}
			} else if ((packageName + ".HIDE_CALL").equals(intent.getAction())) {
				if (intent.hasExtra("group_call_invite_msg_id")) {
					VoIPGroupNotification.hide(context, intent.getIntExtra("currentAccount", 0), intent.getIntExtra("group_call_invite_msg_id", 0));
				} else {
					VoIPPreNotificationService.dismiss(context, false);
				}
			}
		}
	}

	private boolean isMatchingCall(long callId) {
		if (callId == 0) {
			return true;
		}
		VoIPService service = VoIPService.getSharedInstance();
		if (service != null) {
			return service.getCallID() == callId;
		}
		return VoIPPreNotificationService.pendingCall != null
				&& VoIPPreNotificationService.pendingCall.id == callId;
	}
}

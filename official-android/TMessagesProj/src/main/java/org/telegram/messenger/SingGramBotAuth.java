package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/** Logs a Bot API token into an unused native Telegram account slot through MTProto. */
public final class SingGramBotAuth {

    public interface Callback {
        void onSuccess(int account, TLRPC.User bot);

        void onError(String error);
    }

    private SingGramBotAuth() {
    }

    public static void login(String token, Callback callback) {
        String normalized = token == null ? "" : token.trim();
        if (!isValidToken(normalized)) {
            callbackError(callback, "BOT_TOKEN_INVALID");
            return;
        }
        int account = findFreeAccount();
        if (account < 0) {
            callbackError(callback, "NO_FREE_ACCOUNT_SLOT");
            return;
        }

        TLRPC.TL_auth_importBotAuthorization request = new TLRPC.TL_auth_importBotAuthorization();
        request.api_id = BuildVars.APP_ID;
        request.api_hash = BuildVars.APP_HASH;
        request.bot_auth_token = normalized;
        ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
            request.bot_auth_token = "";
            AndroidUtilities.runOnUIThread(() -> {
                if (error != null) {
                    callbackError(callback, TextUtils.isEmpty(error.text) ? "BOT_LOGIN_FAILED" : error.text);
                    return;
                }
                if (!(response instanceof TLRPC.TL_auth_authorization)) {
                    callbackError(callback, "BOT_LOGIN_UNEXPECTED_RESPONSE");
                    return;
                }
                TLRPC.TL_auth_authorization authorization = (TLRPC.TL_auth_authorization) response;
                if (authorization.user == null || !authorization.user.bot) {
                    callbackError(callback, "BOT_LOGIN_NOT_A_BOT");
                    return;
                }
                applyAuthorization(account, authorization);
                if (callback != null) {
                    callback.onSuccess(account, authorization.user);
                }
            });
        }, ConnectionsManager.RequestFlagFailOnServerErrors
                | ConnectionsManager.RequestFlagWithoutLogin
                | ConnectionsManager.RequestFlagTryDifferentDc
                | ConnectionsManager.RequestFlagEnableUnauthorized);
    }

    public static int findFreeAccount() {
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                return account;
            }
        }
        return -1;
    }

    public static boolean isBotAccount(int account) {
        TLRPC.User user = account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT ? UserConfig.getInstance(account).getCurrentUser() : null;
        return user != null && user.bot;
    }

    public static boolean isValidToken(String token) {
        int separator = token.indexOf(':');
        if (separator <= 0 || separator == token.length() - 1 || token.indexOf(':', separator + 1) >= 0) {
            return false;
        }
        for (int index = 0; index < separator; index++) {
            if (!Character.isDigit(token.charAt(index))) {
                return false;
            }
        }
        for (int index = separator + 1; index < token.length(); index++) {
            if (Character.isWhitespace(token.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static void applyAuthorization(int account, TLRPC.TL_auth_authorization authorization) {
        MessagesController.getInstance(account).cleanup();
        ConnectionsManager.getInstance(account).setUserId(authorization.user.id);
        UserConfig.getInstance(account).clearConfig();
        MessagesController.getInstance(account).cleanup();
        UserConfig.getInstance(account).syncContacts = false;
        UserConfig.getInstance(account).setCurrentUser(authorization.user);
        UserConfig.getInstance(account).saveConfig(true);
        MessagesStorage.getInstance(account).cleanup(true);
        ArrayList<TLRPC.User> users = new ArrayList<>();
        users.add(authorization.user);
        MessagesStorage.getInstance(account).putUsersAndChats(users, null, true, true);
        MessagesController.getInstance(account).putUser(authorization.user, false);
        ContactsController.getInstance(account).checkAppAccount();
        MessagesController.getInstance(account).checkPromoInfo(true);
        ConnectionsManager.getInstance(account).updateDcSettings();
        MessagesController.getInstance(account).loadAppConfig();
        MessagesController.getInstance(account).checkPeerColors(false);
        // Fetch the Bot inbox immediately after the account becomes authorized.
        MessagesController.getInstance(account).loadDialogs(0, 0, 50, false);
        if (authorization.future_auth_token != null) {
            AuthTokensHelper.saveLogInToken(authorization);
        }
        MediaDataController.getInstance(account).loadStickersByEmojiOrName(AndroidUtilities.STICKERS_PLACEHOLDER_PACK_NAME, false, true);
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.mainUserInfoChanged);
    }

    private static void callbackError(Callback callback, String error) {
        AndroidUtilities.runOnUIThread(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }
}

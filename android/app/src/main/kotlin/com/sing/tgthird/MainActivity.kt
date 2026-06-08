package com.sing.tgthird

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var configured = false
    private var authorizationState = "signedOut"
    private var loginPhoneNumber: String? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "engineStatus" -> result.success(engineStatus())
                    "configure" -> configure(call, result)
                    "startLogin" -> startLogin(call, result)
                    "submitCode" -> submitCode(call, result)
                    "submitPassword" -> submitPassword(call, result)
                    "logout" -> logout(result)
                    "listChats" -> listChats(call, result)
                    "listMessages" -> listMessages(call, result)
                    "sendText" -> sendText(call, result)
                    else -> result.notImplemented()
                }
            }
    }

    private fun engineStatus(): Map<String, Any?> {
        return mapOf(
            "nativeTdlibAvailable" to false,
            "configured" to configured,
            "authorizationState" to authorizationState,
            "tdlibVersion" to null,
            "statusMessage" to tdlibTodoMessage,
            "placeholder" to true,
        )
    }

    private fun configure(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val apiId = (args["apiId"] as? Number)?.toInt()
        val apiHash = args["apiHash"] as? String

        if (apiId == null || apiId <= 0) {
            result.invalidArgument("apiId must be a positive integer.")
            return
        }
        if (apiHash.isNullOrBlank()) {
            result.invalidArgument("apiHash is required.")
            return
        }

        // TODO(tdlib): Store these values in encrypted app storage only long
        // enough to pass them into TDLib's setTdlibParameters flow.
        configured = true
        authorizationState = "configured"
        result.success(
            actionResult(
                statusMessage = "Configuration accepted by placeholder bridge.",
            ),
        )
    }

    private fun startLogin(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val phoneNumber = args["phoneNumber"] as? String

        if (phoneNumber.isNullOrBlank()) {
            result.invalidArgument("phoneNumber is required.")
            return
        }
        if (!configured) {
            result.success(
                actionResult(
                    ok = false,
                    statusMessage = "Bridge must be configured before login.",
                ),
            )
            return
        }

        // TODO(tdlib): Send setAuthenticationPhoneNumber and surface real
        // authorization state updates through an EventChannel.
        loginPhoneNumber = phoneNumber
        authorizationState = "waitingCode"
        result.success(
            actionResult(
                statusMessage = "Placeholder login started for $phoneNumber.",
            ),
        )
    }

    private fun submitCode(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val code = args["code"] as? String

        if (code.isNullOrBlank()) {
            result.invalidArgument("code is required.")
            return
        }

        // TODO(tdlib): Call checkAuthenticationCode and keep the returned
        // state instead of assuming success.
        authorizationState = "ready"
        result.success(
            actionResult(
                statusMessage = "Placeholder code accepted.",
            ),
        )
    }

    private fun submitPassword(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val password = args["password"] as? String

        if (password.isNullOrBlank()) {
            result.invalidArgument("password is required.")
            return
        }

        // TODO(tdlib): Call checkAuthenticationPassword and keep the returned
        // state instead of assuming success.
        authorizationState = "ready"
        result.success(
            actionResult(
                statusMessage = "Placeholder password accepted.",
            ),
        )
    }

    private fun logout(result: MethodChannel.Result) {
        // TODO(tdlib): Call logOut, close the client, and clear TDLib-owned
        // directories only through explicit product flows.
        authorizationState = "signedOut"
        loginPhoneNumber = null
        result.success(
            actionResult(
                statusMessage = "Placeholder logout complete.",
            ),
        )
    }

    private fun listChats(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val limit = ((args["limit"] as? Number)?.toInt() ?: defaultLimit)
            .coerceIn(1, maxLimit)
        val now = System.currentTimeMillis()

        // TODO(tdlib): Replace with getChats plus chat update handling.
        val chats = listOf(
            mapOf(
                "id" to "placeholder-saved",
                "title" to "Saved Messages",
                "type" to "private",
                "unreadCount" to 0,
                "lastMessagePreview" to "TDLib bridge placeholder is wired.",
                "updatedAtEpochMs" to now - 5 * minuteMillis,
            ),
            mapOf(
                "id" to "placeholder-build",
                "title" to "TDLib Bridge",
                "type" to "basicGroup",
                "unreadCount" to 1,
                "lastMessagePreview" to tdlibTodoMessage,
                "updatedAtEpochMs" to now - 15 * minuteMillis,
            ),
        ).take(limit)

        result.success(
            mapOf(
                "chats" to chats,
                "nextOffset" to null,
                "placeholder" to true,
            ),
        )
    }

    private fun listMessages(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val chatId = args["chatId"] as? String
        val limit = ((args["limit"] as? Number)?.toInt() ?: defaultLimit)
            .coerceIn(1, maxLimit)
        val now = System.currentTimeMillis()

        if (chatId.isNullOrBlank()) {
            result.invalidArgument("chatId is required.")
            return
        }

        // TODO(tdlib): Replace with getChatHistory and TDLib message models.
        val messages = listOf(
            mapOf(
                "id" to "$chatId-placeholder-1",
                "chatId" to chatId,
                "senderId" to "tdlib-placeholder",
                "senderName" to "TDLib placeholder",
                "text" to "Messages will come from TDLib after native libraries are linked.",
                "sentAtEpochMs" to now - 3 * minuteMillis,
                "outgoing" to false,
                "pending" to false,
            ),
        ).take(limit)

        result.success(
            mapOf(
                "messages" to messages,
                "nextFromMessageId" to null,
                "placeholder" to true,
            ),
        )
    }

    private fun sendText(call: MethodCall, result: MethodChannel.Result) {
        val args = call.argumentsMap()
        val chatId = args["chatId"] as? String
        val text = args["text"] as? String
        val now = System.currentTimeMillis()

        if (chatId.isNullOrBlank()) {
            result.invalidArgument("chatId is required.")
            return
        }
        if (text.isNullOrBlank()) {
            result.invalidArgument("text is required.")
            return
        }

        // TODO(tdlib): Replace with sendMessage/inputMessageText and return
        // the TDLib message id once available.
        result.success(
            mapOf(
                "ok" to true,
                "authorizationState" to authorizationState,
                "statusMessage" to "Placeholder text queued.",
                "placeholder" to true,
                "sentMessage" to mapOf(
                    "id" to "$chatId-local-placeholder-$now",
                    "chatId" to chatId,
                    "senderId" to loginPhoneNumber,
                    "senderName" to null,
                    "text" to text.trim(),
                    "sentAtEpochMs" to now,
                    "outgoing" to true,
                    "pending" to true,
                ),
            ),
        )
    }

    private fun actionResult(
        ok: Boolean = true,
        statusMessage: String,
    ): Map<String, Any?> {
        return mapOf(
            "ok" to ok,
            "authorizationState" to authorizationState,
            "statusMessage" to statusMessage,
            "placeholder" to true,
        )
    }

    private fun MethodCall.argumentsMap(): Map<*, *> {
        return arguments as? Map<*, *> ?: emptyMap<String, Any?>()
    }

    private fun MethodChannel.Result.invalidArgument(message: String) {
        error("invalid_argument", message, null)
    }

    companion object {
        private const val channelName = "tgthird/telegram"
        private const val defaultLimit = 50
        private const val maxLimit = 100
        private const val minuteMillis = 60_000L
        private const val tdlibTodoMessage =
            "Native TDLib is not linked yet; Android is returning placeholder data."
    }
}

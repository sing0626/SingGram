package org.telegram.messenger;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SingGramAiClient {

    public static final int ACTION_SUMMARIZE = 1;
    public static final int ACTION_TRANSLATE_ZH_HANT = 2;
    public static final int ACTION_REWRITE_YUE = 3;
    public static final int ACTION_REPLY_SUGGESTIONS = 4;
    public static final int ACTION_SHORTEN = 5;
    public static final int ACTION_EXPLAIN = 6;
    public static final int ACTION_CLEAN_COPY = 7;
    public static final int ACTION_EXTRACT_TASKS = 8;
    public static final int ACTION_TRANSLATE_YUE = 9;
    public static final int ACTION_TEST_CONNECTION = 10;
    public static final int ACTION_CHAT_APP = 11;

    public interface Callback {
        void onResult(String text);
        void onError(String error);
    }

    public interface ModelsCallback {
        void onResult(ArrayList<String> models);
        void onError(String error);
    }

    public static String getActionTitle(int action) {
        switch (action) {
            case ACTION_SUMMARIZE:
                return LocaleController.getString(R.string.SingGramAISummarize);
            case ACTION_TRANSLATE_ZH_HANT:
                return LocaleController.getString(R.string.SingGramAITranslate);
            case ACTION_REWRITE_YUE:
                return LocaleController.getString(R.string.SingGramAIRewriteCantonese);
            case ACTION_REPLY_SUGGESTIONS:
                return LocaleController.getString(R.string.SingGramAIReplyIdeas);
            case ACTION_SHORTEN:
                return LocaleController.getString(R.string.SingGramAIShorten);
            case ACTION_EXPLAIN:
                return LocaleController.getString(R.string.SingGramAIExplain);
            case ACTION_CLEAN_COPY:
                return LocaleController.getString(R.string.SingGramAICleanCopy);
            case ACTION_EXTRACT_TASKS:
                return LocaleController.getString(R.string.SingGramAIExtractTasks);
            case ACTION_TRANSLATE_YUE:
                return LocaleController.getString(R.string.SingGramAITranslateCantonese);
            case ACTION_TEST_CONNECTION:
                return LocaleController.getString(R.string.SingGramAITestConnection);
            case ACTION_CHAT_APP:
                return LocaleController.getString(R.string.SingGramAIChatApp);
            default:
                return LocaleController.getString(R.string.SingGramAI);
        }
    }

    public static void testConnection(Callback callback) {
        runTextAction(ACTION_TEST_CONNECTION, "Reply with OK only.", callback);
    }

    public static void fetchModels(ModelsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!SingGramConfig.isAiEnabled()) {
            callback.onError(LocaleController.getString(R.string.SingGramAIDisabledError));
            return;
        }
        if (!SingGramConfig.isAiConfigured()) {
            callback.onError(LocaleController.getString(R.string.SingGramAIConfigureError));
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(buildModelsEndpoint(SingGramConfig.getAiBaseUrl())).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Accept", "application/json");
                String apiKey = SingGramConfig.getAiApiKey();
                if (!TextUtils.isEmpty(apiKey)) {
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readStream(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    String error = parseError(response);
                    callbackOnError(callback, TextUtils.isEmpty(error) ? "HTTP " + responseCode : error);
                    return;
                }
                ArrayList<String> models = parseModels(response);
                if (models.isEmpty()) {
                    callbackOnError(callback, LocaleController.getString(R.string.SingGramAIModelsEmpty));
                } else {
                    callbackOnResult(callback, models);
                }
            } catch (Exception e) {
                FileLog.e(e);
                callbackOnError(callback, e.getMessage());
            }
        });
    }

    public static void runTextAction(int action, String input, Callback callback) {
        if (callback == null) {
            return;
        }
        if (!SingGramConfig.isAiEnabled()) {
            callback.onError(LocaleController.getString(R.string.SingGramAIDisabledError));
            return;
        }
        if (!SingGramConfig.isAiConfigured()) {
            callback.onError(LocaleController.getString(R.string.SingGramAIConfigureError));
            return;
        }
        if (TextUtils.isEmpty(input)) {
            callback.onError(LocaleController.getString(R.string.SingGramAIEmptyInput));
            return;
        }
        final String text = input.trim();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("model", SingGramConfig.getAiModel());
                body.put("temperature", action == ACTION_REPLY_SUGGESTIONS || action == ACTION_CHAT_APP ? 0.7 : action == ACTION_TEST_CONNECTION ? 0.0 : 0.35);
                body.put("max_tokens", action == ACTION_TEST_CONNECTION ? 64 : action == ACTION_REPLY_SUGGESTIONS || action == ACTION_CHAT_APP ? 900 : 1200);

                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", buildSystemPrompt(action));
                messages.put(system);

                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", text);
                messages.put(user);
                body.put("messages", messages);

                HttpURLConnection connection = (HttpURLConnection) new URL(buildEndpoint(SingGramConfig.getAiBaseUrl())).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                String apiKey = SingGramConfig.getAiApiKey();
                if (!TextUtils.isEmpty(apiKey)) {
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }

                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readStream(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    String error = parseError(response);
                    callbackOnError(callback, TextUtils.isEmpty(error) ? "HTTP " + responseCode : error);
                    return;
                }
                String result = parseResult(response);
                if (TextUtils.isEmpty(result)) {
                    callbackOnError(callback, LocaleController.getString(R.string.SingGramAIEmptyResponse));
                } else {
                    callbackOnResult(callback, result.trim());
                }
            } catch (Exception e) {
                FileLog.e(e);
                callbackOnError(callback, e.getMessage());
            }
        });
    }

    private static String buildSystemPrompt(int action) {
        String custom = SingGramConfig.getAiSystemPrompt();
        String base = TextUtils.isEmpty(custom)
                ? "You are SingGram AI. Be concise, practical, and keep formatting easy to paste into chat."
                : custom;
        base += SingGramConfig.shouldAiPreferCantonese()
                ? "\nDefault to natural written Cantonese for Hong Kong unless the user asks for another language."
                : "\nDefault to Traditional Chinese for Hong Kong unless the user asks for another language.";
        switch (action) {
            case ACTION_SUMMARIZE:
                return base + "\nSummarize the user's message. Use bullet points when useful.";
            case ACTION_TRANSLATE_ZH_HANT:
                return base + "\nTranslate the user's message into fluent Traditional Chinese for Hong Kong. Preserve names, links, code, and numbers.";
            case ACTION_REWRITE_YUE:
                return base + "\nRewrite the user's message in natural written Cantonese used in Hong Kong. Keep the meaning and tone.";
            case ACTION_REPLY_SUGGESTIONS:
                return base + "\nSuggest 3 concise replies. Number them 1 to 3.";
            case ACTION_SHORTEN:
                return base + "\nRewrite the user's message to be shorter and clearer.";
            case ACTION_EXPLAIN:
                return base + "\nExplain the user's message plainly. Clarify implied meaning, context, and any important caveats. Keep it concise.";
            case ACTION_CLEAN_COPY:
                return base + "\nClean up the user's message for direct copying. Remove repeated whitespace, forwarding clutter, and noisy formatting while preserving meaning, links, code, numbers, and names.";
            case ACTION_EXTRACT_TASKS:
                return base + "\nExtract actionable tasks, dates, names, amounts, and decisions from the user's message. If there are no tasks, say so briefly.";
            case ACTION_TRANSLATE_YUE:
                return base + "\nTranslate the user's message into natural written Cantonese used in Hong Kong. Preserve names, links, code, and numbers.";
            case ACTION_TEST_CONNECTION:
                return base + "\nThis is a connectivity test. Reply with a short OK message only.";
            case ACTION_CHAT_APP:
                return base + "\nYou are a chat app inside SingGram. Reply naturally to the user's draft or question, and keep the result ready to send in chat.";
            default:
                return base;
        }
    }

    private static String buildEndpoint(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/chat/completions") || url.endsWith("/responses")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }

    private static String buildModelsEndpoint(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/v1/models") || url.endsWith("/models")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/models";
        }
        return url + "/v1/models";
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String parseError(String response) {
        try {
            JSONObject object = new JSONObject(response);
            JSONObject error = object.optJSONObject("error");
            if (error != null) {
                return error.optString("message", error.toString());
            }
        } catch (Exception ignore) {

        }
        return response;
    }

    private static String parseResult(String response) throws Exception {
        JSONObject object = new JSONObject(response);
        if (!TextUtils.isEmpty(object.optString("output_text"))) {
            return object.optString("output_text");
        }
        JSONArray choices = object.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) {
            return "";
        }
        if (!TextUtils.isEmpty(choice.optString("text"))) {
            return choice.optString("text");
        }
        JSONObject message = choice.optJSONObject("message");
        if (message == null) {
            return "";
        }
        Object content = message.opt("content");
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            StringBuilder builder = new StringBuilder();
            JSONArray array = (JSONArray) content;
            for (int i = 0; i < array.length(); i++) {
                JSONObject part = array.optJSONObject(i);
                if (part == null) {
                    continue;
                }
                String text = part.optString("text", part.optString("content", ""));
                if (!TextUtils.isEmpty(text)) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        return "";
    }

    private static ArrayList<String> parseModels(String response) throws Exception {
        ArrayList<String> models = new ArrayList<>();
        JSONObject object = new JSONObject(response);
        JSONArray data = object.optJSONArray("data");
        if (data == null) {
            return models;
        }
        for (int i = 0; i < data.length(); i++) {
            JSONObject model = data.optJSONObject(i);
            if (model == null) {
                continue;
            }
            String id = model.optString("id", "");
            if (!TextUtils.isEmpty(id) && !models.contains(id)) {
                models.add(id);
            }
        }
        return models;
    }

    private static void callbackOnResult(Callback callback, String text) {
        AndroidUtilities.runOnUIThread(() -> callback.onResult(text));
    }

    private static void callbackOnError(Callback callback, String error) {
        AndroidUtilities.runOnUIThread(() -> callback.onError(TextUtils.isEmpty(error) ? LocaleController.getString(R.string.ErrorOccurred) : error));
    }

    private static void callbackOnResult(ModelsCallback callback, ArrayList<String> models) {
        AndroidUtilities.runOnUIThread(() -> callback.onResult(models));
    }

    private static void callbackOnError(ModelsCallback callback, String error) {
        AndroidUtilities.runOnUIThread(() -> callback.onError(TextUtils.isEmpty(error) ? LocaleController.getString(R.string.ErrorOccurred) : error));
    }
}

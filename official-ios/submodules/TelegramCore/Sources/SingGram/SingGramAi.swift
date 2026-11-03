import Foundation

public enum SingGramAiAction: Int {
    case summarize = 1
    case translateTraditional = 2
    case rewriteCantonese = 3
    case replyIdeas = 4
    case shorten = 5
    case explain = 6
    case cleanCopy = 7
    case extractTasks = 8
    case translateCantonese = 9
    case testConnection = 10
    case followUp = 16

    public var title: String {
        switch self {
        case .summarize:
            return "摘要"
        case .translateTraditional:
            return "翻譯成繁體中文"
        case .rewriteCantonese:
            return "改寫成粵語"
        case .replyIdeas:
            return "回覆建議"
        case .shorten:
            return "縮短"
        case .explain:
            return "解釋"
        case .cleanCopy:
            return "整理可複製文字"
        case .extractTasks:
            return "抽出待辦"
        case .translateCantonese:
            return "翻譯成粵語"
        case .testConnection:
            return "測試連線"
        case .followUp:
            return "跟進摘要"
        }
    }
}

public struct SingGramAiFailure: Error {
    public let message: String

    public init(_ message: String) {
        self.message = message
    }
}

public enum SingGramAi {
    public static func run(action: SingGramAiAction, input: String, completion: @escaping (Result<String, SingGramAiFailure>) -> Void) {
        let text = input.trimmingCharacters(in: .whitespacesAndNewlines)
        if !SingGramSettings.isAiActive {
            completion(.failure(SingGramAiFailure("AI 未啟用，或者閃退安全模式開咗。")))
            return
        }
        if SingGramSettings.aiBaseUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            completion(.failure(SingGramAiFailure("未設定 API 位址。")))
            return
        }
        if text.isEmpty {
            completion(.failure(SingGramAiFailure("冇文字可以處理。")))
            return
        }
        guard let url = URL(string: endpoint(SingGramSettings.aiBaseUrl)) else {
            completion(.failure(SingGramAiFailure("API 位址唔正確。")))
            return
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        let apiKey = SingGramSettings.aiApiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        if !apiKey.isEmpty {
            request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        }
        let body: [String: Any] = [
            "model": SingGramSettings.aiModel,
            "temperature": action == .testConnection ? 0.0 : (action == .replyIdeas ? 0.7 : 0.35),
            "max_tokens": action == .testConnection ? 64 : 1200,
            "messages": [
                ["role": "system", "content": systemPrompt(action)],
                ["role": "user", "content": text]
            ]
        ]
        guard let payload = try? JSONSerialization.data(withJSONObject: body) else {
            completion(.failure(SingGramAiFailure("組唔到請求。")))
            return
        }
        request.httpBody = payload
        URLSession.shared.dataTask(with: request) { data, response, error in
            let result: Result<String, SingGramAiFailure>
            if let error = error {
                result = .failure(SingGramAiFailure(error.localizedDescription))
            } else if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                result = .failure(SingGramAiFailure(errorMessage(data) ?? "HTTP \(http.statusCode)"))
            } else if let data = data, let text = responseText(data), !text.isEmpty {
                result = .success(text.trimmingCharacters(in: .whitespacesAndNewlines))
            } else {
                result = .failure(SingGramAiFailure("AI 冇返回內容。"))
            }
            DispatchQueue.main.async {
                completion(result)
            }
        }.resume()
    }

    private static func systemPrompt(_ action: SingGramAiAction) -> String {
        var prompt = "You are SingGram AI. Be concise, practical, and keep formatting easy to paste into chat."
        prompt += SingGramSettings.shouldAiPreferCantonese
            ? "\nDefault to natural written Cantonese for Hong Kong unless the user asks for another language."
            : "\nDefault to Traditional Chinese for Hong Kong unless the user asks for another language."
        switch action {
        case .summarize:
            prompt += "\nSummarize the user's message. Use bullet points when useful."
        case .translateTraditional:
            prompt += "\nTranslate the user's message into fluent Traditional Chinese for Hong Kong. Preserve names, links, code, and numbers."
        case .rewriteCantonese:
            prompt += "\nRewrite the user's message in natural written Cantonese used in Hong Kong. Keep the meaning and tone."
        case .replyIdeas:
            prompt += "\nSuggest 3 concise replies. Number them 1 to 3."
        case .shorten:
            prompt += "\nRewrite the user's message to be shorter and clearer."
        case .explain:
            prompt += "\nExplain the user's message plainly. Clarify implied meaning, context, and any important caveats. Keep it concise."
        case .cleanCopy:
            prompt += "\nClean up the user's message for direct copying. Remove repeated whitespace and noisy formatting while preserving meaning, links, code, numbers, and names."
        case .extractTasks:
            prompt += "\nExtract actionable tasks, dates, names, amounts, and decisions from the user's message. If there are no tasks, say so briefly."
        case .translateCantonese:
            prompt += "\nTranslate the user's message into natural written Cantonese used in Hong Kong. Preserve names, links, code, and numbers."
        case .testConnection:
            prompt += "\nThis is a connectivity test. Reply with a short OK message only."
        case .followUp:
            prompt += "\nTurn the message into a concise follow-up brief with exactly these headings: Next action, Owner, Due, Context. If no deadline or owner is stated, say Not specified."
        }
        return prompt
    }

    private static func endpoint(_ baseUrl: String) -> String {
        var url = baseUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        while url.hasSuffix("/") {
            url.removeLast()
        }
        if url.hasSuffix("/chat/completions") || url.hasSuffix("/responses") {
            return url
        }
        if url.hasSuffix("/v1") {
            return url + "/chat/completions"
        }
        return url + "/v1/chat/completions"
    }

    private static func responseText(_ data: Data) -> String? {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        if let output = object["output_text"] as? String, !output.isEmpty {
            return output
        }
        if let choices = object["choices"] as? [[String: Any]], let message = choices.first?["message"] as? [String: Any] {
            if let content = message["content"] as? String {
                return content
            }
        }
        return nil
    }

    private static func errorMessage(_ data: Data?) -> String? {
        guard let data = data, let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        if let error = object["error"] as? [String: Any], let message = error["message"] as? String, !message.isEmpty {
            return message
        }
        return String(data: data, encoding: .utf8)
    }
}

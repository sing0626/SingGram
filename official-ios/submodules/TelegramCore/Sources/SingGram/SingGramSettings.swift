import Foundation
import Postbox

public enum SingGramSettings {
    private static let defaults = UserDefaults(suiteName: "singgram") ?? .standard

    private enum Key {
        static let ghostMode = "ghost_mode"
        static let ghostSelectedChatsOnly = "ghost_selected_chats_only"
        static let ghostDialogIds = "ghost_dialog_ids"
        static let disableReadReceipts = "disable_read_receipts"
        static let readReceiptsAllowedDialogIds = "read_receipts_allowed_dialog_ids"
        static let hideTypingStatus = "hide_typing_status"
        static let hidePhoneInSettings = "hide_phone_in_settings"
        static let keepDeletedMessages = "keep_deleted_messages"
        static let keepOriginalEdits = "keep_original_edits"
        static let aiContextMenu = "ai_context_menu"
        static let aiTranslateAction = "ai_translate_action"
        static let quickReplyIdeas = "quick_reply_ideas"
        static let aiInsertResult = "ai_insert_result"
        static let aiPreferCantonese = "ai_prefer_cantonese"
        static let aiEnabled = "ai_enabled"
        static let aiBaseUrl = "ai_base_url"
        static let aiApiKey = "ai_api_key"
        static let aiModel = "ai_model"
        static let liquidGlassLevel = "liquid_glass_level"
        static let showDiagnostics = "show_diagnostics"
        static let crashSafeMode = "crash_safe_mode"
    }

    public static var isGhostModeEnabled: Bool {
        get { defaults.bool(forKey: Key.ghostMode) }
        set { defaults.set(newValue, forKey: Key.ghostMode) }
    }

    public static var isGhostSelectedChatsOnly: Bool {
        get { defaults.bool(forKey: Key.ghostSelectedChatsOnly) }
        set { defaults.set(newValue, forKey: Key.ghostSelectedChatsOnly) }
    }

    public static var isDisableReadReceiptsEnabled: Bool {
        get { defaults.bool(forKey: Key.disableReadReceipts) }
        set { defaults.set(newValue, forKey: Key.disableReadReceipts) }
    }

    public static var isHideTypingStatusEnabled: Bool {
        get { defaults.bool(forKey: Key.hideTypingStatus) }
        set { defaults.set(newValue, forKey: Key.hideTypingStatus) }
    }

    public static var shouldHidePhoneInSettings: Bool {
        get { defaults.bool(forKey: Key.hidePhoneInSettings) }
        set { defaults.set(newValue, forKey: Key.hidePhoneInSettings) }
    }

    public static var shouldKeepDeletedMessages: Bool {
        get { defaults.bool(forKey: Key.keepDeletedMessages) }
        set { defaults.set(newValue, forKey: Key.keepDeletedMessages) }
    }

    public static var shouldKeepOriginalEdits: Bool {
        get { defaults.bool(forKey: Key.keepOriginalEdits) }
        set { defaults.set(newValue, forKey: Key.keepOriginalEdits) }
    }

    public static var isAiContextMenuEnabled: Bool {
        get { defaults.bool(forKey: Key.aiContextMenu) }
        set { defaults.set(newValue, forKey: Key.aiContextMenu) }
    }

    public static var isAiTranslateActionEnabled: Bool {
        get { defaults.bool(forKey: Key.aiTranslateAction) }
        set { defaults.set(newValue, forKey: Key.aiTranslateAction) }
    }

    public static var isQuickReplyIdeasEnabled: Bool {
        get { defaults.bool(forKey: Key.quickReplyIdeas) }
        set { defaults.set(newValue, forKey: Key.quickReplyIdeas) }
    }

    public static var isAiInsertResultEnabled: Bool {
        get { defaults.bool(forKey: Key.aiInsertResult) }
        set { defaults.set(newValue, forKey: Key.aiInsertResult) }
    }

    public static var shouldAiPreferCantonese: Bool {
        get { defaults.bool(forKey: Key.aiPreferCantonese) }
        set { defaults.set(newValue, forKey: Key.aiPreferCantonese) }
    }

    public static var isAiEnabled: Bool {
        get { defaults.bool(forKey: Key.aiEnabled) }
        set { defaults.set(newValue, forKey: Key.aiEnabled) }
    }

    public static var aiBaseUrl: String {
        get { defaults.string(forKey: Key.aiBaseUrl) ?? "" }
        set { defaults.set(newValue, forKey: Key.aiBaseUrl) }
    }

    public static var aiApiKey: String {
        get { defaults.string(forKey: Key.aiApiKey) ?? "" }
        set { defaults.set(newValue, forKey: Key.aiApiKey) }
    }

    public static var aiModel: String {
        get {
            let value = defaults.string(forKey: Key.aiModel) ?? ""
            return value.isEmpty ? "gpt-4o-mini" : value
        }
        set { defaults.set(newValue, forKey: Key.aiModel) }
    }

    public static var liquidGlassLevel: Int {
        get {
            if defaults.object(forKey: Key.liquidGlassLevel) == nil {
                return 1
            }
            return min(max(defaults.integer(forKey: Key.liquidGlassLevel), 0), 2)
        }
        set {
            defaults.set(min(max(newValue, 0), 2), forKey: Key.liquidGlassLevel)
            NotificationCenter.default.post(name: Notification.Name("SingGramLiquidGlassChanged"), object: nil)
        }
    }

    public static var isDiagnosticsEnabled: Bool {
        get { defaults.bool(forKey: Key.showDiagnostics) }
        set { defaults.set(newValue, forKey: Key.showDiagnostics) }
    }

    public static var isCrashSafeModeEnabled: Bool {
        get { defaults.bool(forKey: Key.crashSafeMode) }
        set {
            defaults.set(newValue, forKey: Key.crashSafeMode)
            NotificationCenter.default.post(name: Notification.Name("SingGramLiquidGlassChanged"), object: nil)
        }
    }

    public static var isAiActive: Bool {
        return isAiEnabled && !isCrashSafeModeEnabled
    }

    public static var effectiveLiquidGlassLevel: Int {
        return isCrashSafeModeEnabled ? 0 : liquidGlassLevel
    }

    public static var liquidGlassLevelName: String {
        switch effectiveLiquidGlassLevel {
        case 1:
            return "標準"
        case 2:
            return "強"
        default:
            return "關閉"
        }
    }

    public static var ghostChatCount: Int {
        return dialogIds(Key.ghostDialogIds).count
    }

    public static func isGhostChat(_ peerId: PeerId) -> Bool {
        return dialogIds(Key.ghostDialogIds).contains(peerId.toInt64())
    }

    public static func setGhostChat(_ peerId: PeerId, enabled: Bool) {
        updateDialogIds(Key.ghostDialogIds, peerId: peerId, included: enabled)
    }

    public static func clearGhostChats() {
        defaults.removeObject(forKey: Key.ghostDialogIds)
    }

    public static func allowsReadReceipts(_ peerId: PeerId) -> Bool {
        return dialogIds(Key.readReceiptsAllowedDialogIds).contains(peerId.toInt64())
    }

    public static func setReadReceiptsAllowed(_ peerId: PeerId, allowed: Bool) {
        updateDialogIds(Key.readReceiptsAllowedDialogIds, peerId: peerId, included: allowed)
    }

    public static func enableGhostChat(_ peerId: PeerId) {
        isGhostModeEnabled = true
        isGhostSelectedChatsOnly = true
        setGhostChat(peerId, enabled: true)
        setReadReceiptsAllowed(peerId, allowed: false)
    }

    public static func applyCrashRecovery() {
        isCrashSafeModeEnabled = true
        isAiEnabled = false
        liquidGlassLevel = 0
    }

    public static func diagnosticsText() -> String {
        return [
            "隱身模式: \(isGhostModeEnabled)",
            "只限指定聊天: \(isGhostSelectedChatsOnly)",
            "指定聊天: \(ghostChatCount)",
            "阻止已讀: \(isDisableReadReceiptsEnabled)",
            "隱藏輸入: \(isHideTypingStatusEnabled)",
            "隱藏電話: \(shouldHidePhoneInSettings)",
            "保留已刪訊息: \(shouldKeepDeletedMessages)",
            "保留編輯前內容: \(shouldKeepOriginalEdits)",
            "AI: \(isAiActive)",
            "模型: \(aiModel)",
            "API: \(aiBaseUrl.isEmpty ? "未設定" : aiBaseUrl)",
            "粵語: \(shouldAiPreferCantonese)",
            "玻璃: \(liquidGlassLevelName)",
            "診斷: \(isDiagnosticsEnabled)",
            "閃退安全模式: \(isCrashSafeModeEnabled)"
        ].joined(separator: "\n")
    }

    public static func shouldDisableReadReceipts(peerId: PeerId) -> Bool {
        return isGhostModeEnabled
            && isDisableReadReceiptsEnabled
            && isGhostModeAllowed(peerId: peerId)
            && !isReadReceiptAllowed(peerId: peerId)
    }

    public static func shouldHideTypingStatus(peerId: PeerId) -> Bool {
        return isGhostModeEnabled
            && isHideTypingStatusEnabled
            && isGhostModeAllowed(peerId: peerId)
    }

    private static func isGhostModeAllowed(peerId: PeerId) -> Bool {
        if !isGhostSelectedChatsOnly {
            return true
        }
        return dialogIds(Key.ghostDialogIds).contains(peerId.toInt64())
    }

    private static func isReadReceiptAllowed(peerId: PeerId) -> Bool {
        return dialogIds(Key.readReceiptsAllowedDialogIds).contains(peerId.toInt64())
    }

    private static func dialogIds(_ key: String) -> Set<Int64> {
        let values = defaults.stringArray(forKey: key) ?? []
        return Set(values.compactMap(Int64.init))
    }

    private static func updateDialogIds(_ key: String, peerId: PeerId, included: Bool) {
        var ids = dialogIds(key)
        if included {
            ids.insert(peerId.toInt64())
        } else {
            ids.remove(peerId.toInt64())
        }
        defaults.set(ids.map { String($0) }, forKey: key)
    }
}

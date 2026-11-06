import SingGramAppearance
import TelegramUIPreferences
import Foundation
import UIKit
import SwiftSignalKit
import Display
import AccountContext
import ItemListUI
import TelegramPresentationData
import TelegramCore
import PresentationDataUtils

private enum SingGramSettingsPage: String {
    case overview = "SingGram"
    case appearance = "外觀"
    case privacy = "隱私與訊息保護"
    case ai = "AI 與聊天工具"
    case diagnostics = "診斷與安全模式"
}

private enum SingGramSettingsEntry: ItemListNodeEntry {
    case homeAppearance(PresentationTheme)
    case homePrivacy(PresentationTheme)
    case homeAi(PresentationTheme)
    case homeDiagnostics(PresentationTheme)
    case homeInfo(PresentationTheme)
    case themeColor(PresentationTheme, String)
    case bubbleRadius(PresentationTheme, String)
    case listDensity(PresentationTheme, String)
    case systemFont(PresentationTheme, Bool)
    case compactComposer(PresentationTheme, Bool)
    case floatingNavigation(PresentationTheme, Bool)
    case appearanceInfo(PresentationTheme)
    case aiInfo(PresentationTheme)
    case diagnosticsInfo(PresentationTheme)
    case ghostHeader(PresentationTheme)
    case ghostEnabled(PresentationTheme, Bool)
    case ghostSelectedChats(PresentationTheme, Bool)
    case disableReadReceipts(PresentationTheme, Bool)
    case hideTyping(PresentationTheme, Bool)
    case hidePhone(PresentationTheme, Bool)
    case addGhostChat(PresentationTheme, Int)
    case clearGhostChats(PresentationTheme)
    case ghostInfo(PresentationTheme)

    case protectionHeader(PresentationTheme)
    case keepDeleted(PresentationTheme, Bool)
    case keepEdits(PresentationTheme, Bool)
    case protectionInfo(PresentationTheme)

    case toolsHeader(PresentationTheme)
    case aiContextMenu(PresentationTheme, Bool)
    case aiTranslate(PresentationTheme, Bool)
    case replyIdeas(PresentationTheme, Bool)
    case insertResult(PresentationTheme, Bool)

    case aiHeader(PresentationTheme)
    case aiEnabled(PresentationTheme, Bool)
    case preferCantonese(PresentationTheme, Bool)
    case aiModel(PresentationTheme, String)
    case aiBaseUrl(PresentationTheme, String)
    case aiApiKey(PresentationTheme, Bool)
    case testAi(PresentationTheme)

    case appearanceHeader(PresentationTheme)
    case liquidGlass(PresentationTheme, String)

    case diagnosticsHeader(PresentationTheme)
    case diagnosticsVisible(PresentationTheme, Bool)
    case crashSafeMode(PresentationTheme, Bool)
    case diagnosticsReport(PresentationTheme)

    var section: ItemListSectionId {
        switch self {
        case .homeAppearance, .homePrivacy, .homeAi, .homeDiagnostics, .homeInfo:
            return 0
        case .themeColor, .bubbleRadius, .listDensity, .systemFont, .compactComposer, .floatingNavigation, .appearanceInfo:
            return 4
        case .aiInfo:
            return 3
        case .diagnosticsInfo:
            return 5
        case .ghostHeader, .ghostEnabled, .ghostSelectedChats, .disableReadReceipts, .hideTyping, .hidePhone, .addGhostChat, .clearGhostChats, .ghostInfo:
            return 0
        case .protectionHeader, .keepDeleted, .keepEdits, .protectionInfo:
            return 1
        case .toolsHeader, .aiContextMenu, .aiTranslate, .replyIdeas, .insertResult:
            return 2
        case .aiHeader, .aiEnabled, .preferCantonese, .aiModel, .aiBaseUrl, .aiApiKey, .testAi:
            return 3
        case .appearanceHeader, .liquidGlass:
            return 4
        case .diagnosticsHeader, .diagnosticsVisible, .crashSafeMode, .diagnosticsReport:
            return 5
        }
    }

    var stableId: Int {
        switch self {
        case .homeAppearance: return -5
        case .homePrivacy: return -4
        case .homeAi: return -3
        case .homeDiagnostics: return -2
        case .homeInfo: return -1
        case .themeColor: return 31
        case .bubbleRadius: return 32
        case .listDensity: return 33
        case .systemFont: return 34
        case .compactComposer: return 35
        case .floatingNavigation: return 36
        case .appearanceInfo: return 37
        case .aiInfo: return 38
        case .diagnosticsInfo: return 39
        case .ghostHeader: return 0
        case .ghostEnabled: return 1
        case .ghostSelectedChats: return 2
        case .disableReadReceipts: return 3
        case .hideTyping: return 4
        case .hidePhone: return 5
        case .addGhostChat: return 6
        case .clearGhostChats: return 7
        case .ghostInfo: return 8
        case .protectionHeader: return 9
        case .keepDeleted: return 10
        case .keepEdits: return 11
        case .protectionInfo: return 12
        case .toolsHeader: return 13
        case .aiContextMenu: return 14
        case .aiTranslate: return 15
        case .replyIdeas: return 16
        case .insertResult: return 17
        case .aiHeader: return 18
        case .aiEnabled: return 19
        case .preferCantonese: return 20
        case .aiModel: return 21
        case .aiBaseUrl: return 22
        case .aiApiKey: return 23
        case .testAi: return 24
        case .appearanceHeader: return 25
        case .liquidGlass: return 26
        case .diagnosticsHeader: return 27
        case .diagnosticsVisible: return 28
        case .crashSafeMode: return 29
        case .diagnosticsReport: return 30
        }
    }

    static func < (lhs: SingGramSettingsEntry, rhs: SingGramSettingsEntry) -> Bool {
        return lhs.stableId < rhs.stableId
    }

    func item(presentationData: ItemListPresentationData, arguments: Any) -> ListViewItem {
        let actions = arguments as? SingGramSettingsActions
        switch self {
        case .homeAppearance:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "外觀", label: "主題・玻璃・氣泡", sectionId: self.section, style: .blocks, action: { actions?.open(.appearance) })
        case .homePrivacy:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "隱私與訊息保護", label: "隱身・已讀・保留訊息", sectionId: self.section, style: .blocks, action: { actions?.open(.privacy) })
        case .homeAi:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "AI 與聊天工具", label: SingGramSettings.isAiActive ? "已開啟" : "未開啟", sectionId: self.section, style: .blocks, action: { actions?.open(.ai) })
        case .homeDiagnostics:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "診斷與安全模式", label: "連線・疑難排解", sectionId: self.section, style: .blocks, action: { actions?.open(.diagnostics) })
        case .homeInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("將 SingGram 調整成你習慣嘅樣。外觀設定會保留，隨時都可以再改。"), sectionId: self.section)
        case let .themeColor(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "主題色", label: value, sectionId: self.section, style: .blocks, action: { actions?.chooseAccent() })
        case let .bubbleRadius(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "訊息氣泡", label: value, sectionId: self.section, style: .blocks, action: { actions?.chooseBubbles() })
        case let .listDensity(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "聊天列表疏密", label: value, sectionId: self.section, style: .blocks, action: { actions?.chooseDensity() })
        case let .systemFont(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "跟隨系統文字大小", value: value, sectionId: self.section, style: .blocks, updated: { actions?.setSystemFont($0) })
        case let .compactComposer(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "簡潔輸入列", value: value, sectionId: self.section, style: .blocks, updated: { SingGramAppearance.compactComposer = $0; actions?.changed() })
        case let .floatingNavigation(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "加闊導航兩側留白", value: value, sectionId: self.section, style: .blocks, updated: { SingGramAppearance.floatingNavigation = $0; actions?.changed() })
        case .appearanceInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("長按輸入列嘅 ＋ 可開啟 AI 工具。玻璃效果會配合淺色／深色模式，並尊重系統嘅「減少透明度」同「減少動態效果」。"), sectionId: self.section)
        case .aiInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("設定好 AI 服務後，可喺訊息選單或長按輸入列 ＋ 使用。只有你選用 AI 時，相關文字先會送去你設定嘅服務。"), sectionId: self.section)
        case .diagnosticsInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("遇到畫面或 AI 問題，可以先開安全模式。呢個模式會暫停玻璃效果同 AI，唔會刪除聊天。"), sectionId: self.section)
        case .ghostHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "隱身模式", sectionId: self.section)
        case let .ghostEnabled(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "啟用隱身模式", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setGhost(updated)
            })
        case let .ghostSelectedChats(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "只在指定聊天隱身", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setSelectedChats(updated)
            })
        case let .disableReadReceipts(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "阻止自動已讀", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setReadReceipts(updated)
            })
        case let .hideTyping(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "隱藏輸入與錄音狀態", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setTyping(updated)
            })
        case let .hidePhone(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "在設定隱藏電話號碼", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setHidePhone(updated)
            })
        case let .addGhostChat(_, count):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "加入指定聊天", label: "\(count)", sectionId: self.section, style: .blocks, action: {
                actions?.addGhostChat()
            })
        case .clearGhostChats:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "清除指定聊天", label: "", sectionId: self.section, style: .blocks, action: {
                actions?.clearGhostChats()
            })
        case .ghostInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("開咗隱身模式之後，先會停已讀回條同輸入狀態。可以喺度加入指定聊天，或者喺聊天長按選單入面對呢個聊天隱身。"), sectionId: self.section)
        case .protectionHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "訊息保護", sectionId: self.section)
        case let .keepDeleted(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "本機保留已刪訊息", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setKeepDeleted(updated)
            })
        case let .keepEdits(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "保留編輯前內容", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setKeepEdits(updated)
            })
        case .protectionInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("對方刪訊息或者改訊息時，部機會留低原本嗰版。"), sectionId: self.section)
        case .toolsHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "聊天工具", sectionId: self.section)
        case let .aiContextMenu(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "AI 訊息選單", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setAiMenu(updated)
            })
        case let .aiTranslate(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "快速翻譯按鈕", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setAiTranslate(updated)
            })
        case let .replyIdeas(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "回覆建議", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setReplyIdeas(updated)
            })
        case let .insertResult(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "插入 AI 結果", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setInsertResult(updated)
            })
        case .aiHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "AI", sectionId: self.section)
        case let .aiEnabled(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "啟用 AI", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setAiEnabled(updated)
            })
        case let .preferCantonese(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "AI 優先用粵語", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setCantonese(updated)
            })
        case let .aiModel(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "選擇模型", label: value, sectionId: self.section, style: .blocks, action: {
                actions?.editText(title: "選擇模型", value: SingGramSettings.aiModel) { SingGramSettings.aiModel = $0 }
            })
        case let .aiBaseUrl(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "API 位址", label: value.isEmpty ? "未設定" : value, sectionId: self.section, style: .blocks, action: {
                actions?.editText(title: "API 位址", value: SingGramSettings.aiBaseUrl) { SingGramSettings.aiBaseUrl = $0 }
            })
        case let .aiApiKey(_, configured):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "API 金鑰", label: configured ? "已設定" : "未設定", sectionId: self.section, style: .blocks, action: {
                actions?.editText(title: "API 金鑰", value: SingGramSettings.aiApiKey) { SingGramSettings.aiApiKey = $0 }
            })
        case .testAi:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "測試連線", label: "", sectionId: self.section, style: .blocks, action: {
                actions?.testAi()
            })
        case .appearanceHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "外觀", sectionId: self.section)
        case let .liquidGlass(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "Liquid Glass", label: value, sectionId: self.section, style: .blocks, action: {
                actions?.cycleLiquidGlass()
            })
        case .diagnosticsHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "診斷", sectionId: self.section)
        case let .diagnosticsVisible(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "顯示診斷工具", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setDiagnostics(updated)
            })
        case let .crashSafeMode(_, value):
            return ItemListSwitchItem(presentationData: presentationData, systemStyle: .glass, title: "閃退安全模式", value: value, sectionId: self.section, style: .blocks, updated: { updated in
                actions?.setCrashSafe(updated)
            })
        case .diagnosticsReport:
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "查看診斷", label: "", sectionId: self.section, style: .blocks, action: {
                actions?.showDiagnostics()
            })
        }
    }
}

private func singGramSettingsEntries(theme: PresentationTheme, page: SingGramSettingsPage, themeSettings: PresentationThemeSettings) -> [SingGramSettingsEntry] {
    if page == .overview {
        return [.homeAppearance(theme), .homePrivacy(theme), .homeAi(theme), .homeDiagnostics(theme), .homeInfo(theme)]
    }
    var entries: [SingGramSettingsEntry] = [
        .ghostHeader(theme),
        .ghostEnabled(theme, SingGramSettings.isGhostModeEnabled),
        .ghostSelectedChats(theme, SingGramSettings.isGhostSelectedChatsOnly),
        .disableReadReceipts(theme, SingGramSettings.isDisableReadReceiptsEnabled),
        .hideTyping(theme, SingGramSettings.isHideTypingStatusEnabled),
        .hidePhone(theme, SingGramSettings.shouldHidePhoneInSettings),
        .addGhostChat(theme, SingGramSettings.ghostChatCount)
    ]
    if SingGramSettings.ghostChatCount > 0 {
        entries.append(.clearGhostChats(theme))
    }
    entries.append(contentsOf: [
        .ghostInfo(theme),
        .protectionHeader(theme),
        .keepDeleted(theme, SingGramSettings.shouldKeepDeletedMessages),
        .keepEdits(theme, SingGramSettings.shouldKeepOriginalEdits),
        .protectionInfo(theme),
        .toolsHeader(theme),
        .aiContextMenu(theme, SingGramSettings.isAiContextMenuEnabled),
        .aiTranslate(theme, SingGramSettings.isAiTranslateActionEnabled),
        .replyIdeas(theme, SingGramSettings.isQuickReplyIdeasEnabled),
        .insertResult(theme, SingGramSettings.isAiInsertResultEnabled),
        .aiHeader(theme),
        .aiEnabled(theme, SingGramSettings.isAiEnabled),
        .preferCantonese(theme, SingGramSettings.shouldAiPreferCantonese),
        .aiModel(theme, SingGramSettings.aiModel),
        .aiBaseUrl(theme, SingGramSettings.aiBaseUrl),
        .aiApiKey(theme, !SingGramSettings.aiApiKey.isEmpty),
        .testAi(theme),
        .appearanceHeader(theme),
        .liquidGlass(theme, SingGramSettings.liquidGlassLevelName),
        .themeColor(theme, SingGramAppearance.accent.title),
        .bubbleRadius(theme, "\(themeSettings.chatBubbleSettings.mainRadius) pt"),
        .listDensity(theme, SingGramAppearance.listDensity.title),
        .systemFont(theme, themeSettings.useSystemFont),
        .compactComposer(theme, SingGramAppearance.compactComposer),
        .floatingNavigation(theme, SingGramAppearance.floatingNavigation),
        .appearanceInfo(theme),
        .aiInfo(theme),
        .diagnosticsInfo(theme),
        .diagnosticsHeader(theme),
        .diagnosticsVisible(theme, SingGramSettings.isDiagnosticsEnabled),
        .crashSafeMode(theme, SingGramSettings.isCrashSafeModeEnabled)
    ])
    if SingGramSettings.isDiagnosticsEnabled {
        entries.append(.diagnosticsReport(theme))
    }
    return entries.filter { entry in
        switch page {
        case .overview: return false
        case .privacy: return entry.section == 0 || entry.section == 1
        case .ai: return entry.section == 2 || entry.section == 3
        case .appearance: return entry.section == 4
        case .diagnostics: return entry.section == 5
        }
    }.sorted()
}

private final class SingGramSettingsActions {
    weak var controller: ViewController?
    let context: AccountContext
    let changed: () -> Void

    init(context: AccountContext, changed: @escaping () -> Void) {
        self.context = context
        self.changed = changed
    }

    func open(_ page: SingGramSettingsPage) {
        (self.controller?.navigationController as? NavigationController)?.pushViewController(singGramSettingsPageController(context: self.context, page: page))
    }

    func choose(title: String, options: [(String, () -> Void)]) {
        let sheet = UIAlertController(title: title, message: nil, preferredStyle: .actionSheet)
        for (title, action) in options {
            sheet.addAction(UIAlertAction(title: title, style: .default, handler: { _ in action() }))
        }
        sheet.addAction(UIAlertAction(title: "取消", style: .cancel))
        if let view = self.controller?.view {
            sheet.popoverPresentationController?.sourceView = view
            sheet.popoverPresentationController?.sourceRect = CGRect(x: view.bounds.midX, y: view.bounds.midY, width: 1, height: 1)
        }
        self.controller?.view.window?.rootViewController?.present(sheet, animated: true)
    }

    func chooseAccent() {
        self.choose(title: "主題色", options: SingGramAccent.allCases.map { choice in
            (choice.title, { [weak self] in
                guard let self else { return }
                SingGramAppearance.accent = choice
                let colors: [PresentationThemeBaseColor] = [.blue, .green, .purple, .pink, .orange]
                let color = PresentationThemeAccentColor(baseColor: colors[choice.rawValue])
                let _ = updatePresentationThemeSettingsInteractively(accountManager: self.context.sharedContext.accountManager, { current in
                    var colors = current.themeSpecificAccentColors
                    let themes: [PresentationBuiltinThemeReference] = [.dayClassic, .day, .night, .nightAccent]
                    for theme in themes {
                        colors[PresentationThemeReference.builtin(theme).index] = color
                    }
                    var theme = current.theme
                    if case let .cloud(info) = theme, let settings = info.theme.settings?.first {
                        theme = .builtin(PresentationBuiltinThemeReference(baseTheme: settings.baseTheme))
                    }
                    var automatic = current.automaticThemeSwitchSetting
                    if case let .cloud(info) = automatic.theme, let settings = info.theme.settings?.first {
                        automatic.theme = .builtin(PresentationBuiltinThemeReference(baseTheme: settings.baseTheme))
                    }
                    colors[theme.index] = color
                    return current.withUpdatedTheme(theme).withUpdatedAutomaticThemeSwitchSetting(automatic).withUpdatedThemeSpecificAccentColors(colors)
                }).start(completed: { [weak self] in self?.changed() })
            })
        })
    }

    func chooseBubbles() {
        self.choose(title: "訊息氣泡", options: [("俐落", Int32(8)), ("柔和", Int32(16)), ("圓潤", Int32(22))].map { title, radius in
            (title, { [weak self] in
                guard let self else { return }
                let _ = updatePresentationThemeSettingsInteractively(accountManager: self.context.sharedContext.accountManager, { current in
                    return current.withUpdatedChatBubbleSettings(PresentationChatBubbleSettings(mainRadius: radius, auxiliaryRadius: max(4, radius / 2), mergeBubbleCorners: current.chatBubbleSettings.mergeBubbleCorners))
                }).start(completed: { [weak self] in self?.changed() })
            })
        })
    }

    func chooseDensity() {
        self.choose(title: "聊天列表疏密", options: SingGramListDensity.allCases.map { density in
            (density.title, { [weak self] in
                guard let self else { return }
                SingGramAppearance.listDensity = density
                self.changed()
            })
        })
    }

    func setSystemFont(_ value: Bool) {
        let _ = updatePresentationThemeSettingsInteractively(accountManager: self.context.sharedContext.accountManager, { current in
            return current.withUpdatedUseSystemFont(value)
        }).start(completed: { [weak self] in self?.changed() })
    }

    func setGhost(_ value: Bool) { SingGramSettings.isGhostModeEnabled = value; self.changed() }
    func setSelectedChats(_ value: Bool) { SingGramSettings.isGhostSelectedChatsOnly = value; self.changed() }
    func setReadReceipts(_ value: Bool) { SingGramSettings.isDisableReadReceiptsEnabled = value; self.changed() }
    func setTyping(_ value: Bool) { SingGramSettings.isHideTypingStatusEnabled = value; self.changed() }
    func setHidePhone(_ value: Bool) { SingGramSettings.shouldHidePhoneInSettings = value; self.changed() }
    func setKeepDeleted(_ value: Bool) { SingGramSettings.shouldKeepDeletedMessages = value; self.changed() }
    func setKeepEdits(_ value: Bool) { SingGramSettings.shouldKeepOriginalEdits = value; self.changed() }
    func setAiMenu(_ value: Bool) { SingGramSettings.isAiContextMenuEnabled = value; self.changed() }
    func setAiTranslate(_ value: Bool) { SingGramSettings.isAiTranslateActionEnabled = value; self.changed() }
    func setReplyIdeas(_ value: Bool) { SingGramSettings.isQuickReplyIdeasEnabled = value; self.changed() }
    func setInsertResult(_ value: Bool) { SingGramSettings.isAiInsertResultEnabled = value; self.changed() }
    func setAiEnabled(_ value: Bool) { SingGramSettings.isAiEnabled = value; self.changed() }
    func setCantonese(_ value: Bool) { SingGramSettings.shouldAiPreferCantonese = value; self.changed() }
    func setDiagnostics(_ value: Bool) { SingGramSettings.isDiagnosticsEnabled = value; self.changed() }
    func setCrashSafe(_ value: Bool) {
        if value {
            SingGramSettings.applyCrashRecovery()
        } else {
            SingGramSettings.isCrashSafeModeEnabled = false
        }
        self.changed()
    }

    func cycleLiquidGlass() {
        let next = (SingGramSettings.liquidGlassLevel + 1) % 3
        SingGramSettings.liquidGlassLevel = next
        if next > 0 {
            SingGramSettings.isCrashSafeModeEnabled = false
        }
        self.changed()
    }

    func addGhostChat() {
        let picker = self.context.sharedContext.makePeerSelectionController(PeerSelectionControllerParams(context: self.context, filter: [.excludeSavedMessages, .doNotSearchMessages], title: "指定聊天", hasCreation: false))
        picker.peerSelected = { [weak self] peer, _ in
            guard let self, peer.id != self.context.account.peerId else {
                return
            }
            SingGramSettings.enableGhostChat(peer.id)
            self.changed()
            if let navigationController = picker.navigationController as? NavigationController {
                _ = navigationController.popViewController(animated: true)
            }
        }
        (self.controller?.navigationController as? NavigationController)?.pushViewController(picker)
    }

    func clearGhostChats() {
        SingGramSettings.clearGhostChats()
        self.changed()
    }

    func testAi() {
        SingGramAi.run(action: .testConnection, input: "Reply with OK only.") { [weak self] result in
            let text: String
            switch result {
            case let .success(value):
                text = value
            case let .failure(error):
                text = error.message
            }
            guard let self, let controller = self.controller else {
                return
            }
            controller.present(textAlertController(context: self.context, title: "測試連線", text: text, actions: [TextAlertAction(type: .defaultAction, title: "確定", action: {})]), in: .window(.root))
        }
    }

    func showDiagnostics() {
        guard let controller = self.controller else {
            return
        }
        controller.present(textAlertController(context: self.context, title: "診斷", text: SingGramSettings.diagnosticsText(), actions: [TextAlertAction(type: .defaultAction, title: "確定", action: {})], actionLayout: .vertical), in: .window(.root))
    }

    func editText(title: String, value: String, save: @escaping (String) -> Void) {
        let alert = UIAlertController(title: title, message: nil, preferredStyle: .alert)
        alert.addTextField { textField in
            textField.text = value
            textField.clearButtonMode = .whileEditing
            textField.isSecureTextEntry = title == "API 金鑰"
            textField.autocapitalizationType = .none
            textField.autocorrectionType = .no
            textField.spellCheckingType = .no
        }
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        alert.addAction(UIAlertAction(title: "儲存", style: .default, handler: { _ in
            save(alert.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "")
            self.changed()
        }))
        self.controller?.view.window?.rootViewController?.present(alert, animated: true)
    }
}

func singGramSettingsController(context: AccountContext) -> ViewController {
    return singGramSettingsPageController(context: context, page: .overview)
}

private func singGramSettingsPageController(context: AccountContext, page: SingGramSettingsPage) -> ViewController {
    let version = ValuePromise<Int>(0, ignoreRepeated: true)
    var currentVersion = 0
    let actions = SingGramSettingsActions(context: context, changed: {
        currentVersion += 1
        version.set(currentVersion)
    })
    let signal = combineLatest(context.sharedContext.presentationData, version.get(), context.sharedContext.accountManager.sharedData(keys: [ApplicationSpecificSharedDataKeys.presentationThemeSettings]))
    |> map { presentationData, _, sharedData -> (ItemListControllerState, (ItemListNodeState, Any)) in
        let themeSettings = sharedData.entries[ApplicationSpecificSharedDataKeys.presentationThemeSettings]?.get(PresentationThemeSettings.self) ?? PresentationThemeSettings.defaultSettings
        let entries = singGramSettingsEntries(theme: presentationData.theme, page: page, themeSettings: themeSettings)
        let state = ItemListControllerState(presentationData: ItemListPresentationData(presentationData), title: .text(page.rawValue), leftNavigationButton: nil, rightNavigationButton: nil, backNavigationButton: ItemListBackButton(title: presentationData.strings.Common_Back))
        return (state, (ItemListNodeState(presentationData: ItemListPresentationData(presentationData), entries: entries, style: .blocks), actions))
    }
    let controller = ItemListController(context: context, state: signal)
    actions.controller = controller
    return controller
}

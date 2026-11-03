import Foundation
import UIKit
import SwiftSignalKit
import Display
import AccountContext
import ItemListUI
import TelegramPresentationData
import TelegramCore
import PresentationDataUtils

private enum SingGramSettingsEntry: ItemListNodeEntry {
    case ghostHeader(PresentationTheme)
    case ghostEnabled(PresentationTheme, Bool)
    case ghostSelectedChats(PresentationTheme, Bool)
    case disableReadReceipts(PresentationTheme, Bool)
    case hideTyping(PresentationTheme, Bool)
    case hidePhone(PresentationTheme, Bool)
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

    case appearanceHeader(PresentationTheme)
    case liquidGlass(PresentationTheme, String)

    case diagnosticsHeader(PresentationTheme)
    case diagnosticsVisible(PresentationTheme, Bool)
    case crashSafeMode(PresentationTheme, Bool)

    var section: ItemListSectionId {
        switch self {
        case .ghostHeader, .ghostEnabled, .ghostSelectedChats, .disableReadReceipts, .hideTyping, .hidePhone, .ghostInfo:
            return 0
        case .protectionHeader, .keepDeleted, .keepEdits, .protectionInfo:
            return 1
        case .toolsHeader, .aiContextMenu, .aiTranslate, .replyIdeas, .insertResult:
            return 2
        case .aiHeader, .aiEnabled, .preferCantonese, .aiModel, .aiBaseUrl, .aiApiKey:
            return 3
        case .appearanceHeader, .liquidGlass:
            return 4
        case .diagnosticsHeader, .diagnosticsVisible, .crashSafeMode:
            return 5
        }
    }

    var stableId: Int {
        switch self {
        case .ghostHeader: return 0
        case .ghostEnabled: return 1
        case .ghostSelectedChats: return 2
        case .disableReadReceipts: return 3
        case .hideTyping: return 4
        case .hidePhone: return 5
        case .ghostInfo: return 6
        case .protectionHeader: return 7
        case .keepDeleted: return 8
        case .keepEdits: return 9
        case .protectionInfo: return 10
        case .toolsHeader: return 11
        case .aiContextMenu: return 12
        case .aiTranslate: return 13
        case .replyIdeas: return 14
        case .insertResult: return 15
        case .aiHeader: return 16
        case .aiEnabled: return 17
        case .preferCantonese: return 18
        case .aiModel: return 19
        case .aiBaseUrl: return 20
        case .aiApiKey: return 21
        case .appearanceHeader: return 22
        case .liquidGlass: return 23
        case .diagnosticsHeader: return 24
        case .diagnosticsVisible: return 25
        case .crashSafeMode: return 26
        }
    }

    static func < (lhs: SingGramSettingsEntry, rhs: SingGramSettingsEntry) -> Bool {
        return lhs.stableId < rhs.stableId
    }

    func item(presentationData: ItemListPresentationData, arguments: Any) -> ListViewItem {
        let actions = arguments as? SingGramSettingsActions
        switch self {
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
        case .ghostInfo:
            return ItemListTextItem(presentationData: presentationData, text: .plain("開咗隱身模式之後，先會停已讀回條同輸入狀態。指定聊天選擇器暫時未有，開啟「只在指定聊天隱身」後唔會套用到任何聊天。"), sectionId: self.section)
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
            return ItemListTextItem(presentationData: presentationData, text: .plain("對方刪訊息時，部機會繼續留低原訊息。編輯前內容暫時只會記住開關。"), sectionId: self.section)
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
        case .appearanceHeader:
            return ItemListSectionHeaderItem(presentationData: presentationData, text: "外觀", sectionId: self.section)
        case let .liquidGlass(_, value):
            return ItemListDisclosureItem(presentationData: presentationData, systemStyle: .glass, title: "玻璃模式", label: value, sectionId: self.section, style: .blocks, action: {
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
        }
    }
}

private func singGramSettingsEntries(theme: PresentationTheme) -> [SingGramSettingsEntry] {
    return [
        .ghostHeader(theme),
        .ghostEnabled(theme, SingGramSettings.isGhostModeEnabled),
        .ghostSelectedChats(theme, SingGramSettings.isGhostSelectedChatsOnly),
        .disableReadReceipts(theme, SingGramSettings.isDisableReadReceiptsEnabled),
        .hideTyping(theme, SingGramSettings.isHideTypingStatusEnabled),
        .hidePhone(theme, SingGramSettings.shouldHidePhoneInSettings),
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
        .appearanceHeader(theme),
        .liquidGlass(theme, SingGramSettings.liquidGlassLevelName),
        .diagnosticsHeader(theme),
        .diagnosticsVisible(theme, SingGramSettings.isDiagnosticsEnabled),
        .crashSafeMode(theme, SingGramSettings.isCrashSafeModeEnabled)
    ]
}

private final class SingGramSettingsActions {
    weak var controller: ViewController?
    let changed: () -> Void

    init(changed: @escaping () -> Void) {
        self.changed = changed
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
    func setCrashSafe(_ value: Bool) { SingGramSettings.isCrashSafeModeEnabled = value; self.changed() }

    func cycleLiquidGlass() {
        SingGramSettings.liquidGlassLevel = (SingGramSettings.liquidGlassLevel + 1) % 3
        self.changed()
    }

    func editText(title: String, value: String, save: @escaping (String) -> Void) {
        let alert = UIAlertController(title: title, message: nil, preferredStyle: .alert)
        alert.addTextField { textField in
            textField.text = value
            textField.clearButtonMode = .whileEditing
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
    let version = ValuePromise<Int>(0, ignoreRepeated: true)
    var currentVersion = 0
    let actions = SingGramSettingsActions(changed: {
        currentVersion += 1
        version.set(currentVersion)
    })
    let signal = combineLatest(context.sharedContext.presentationData, version.get())
    |> map { presentationData, _ -> (ItemListControllerState, (ItemListNodeState, Any)) in
        let entries = singGramSettingsEntries(theme: presentationData.theme)
        let state = ItemListControllerState(presentationData: ItemListPresentationData(presentationData), title: .text("SingGram"), leftNavigationButton: nil, rightNavigationButton: nil, backNavigationButton: ItemListBackButton(title: presentationData.strings.Common_Back))
        return (state, (ItemListNodeState(presentationData: ItemListPresentationData(presentationData), entries: entries, style: .blocks), actions))
    }
    let controller = ItemListController(context: context, state: signal)
    actions.controller = controller
    return controller
}

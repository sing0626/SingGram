import Foundation
import UIKit
import Display
import TelegramCore
import AccountContext
import ContextUI
import SwiftSignalKit
import TelegramPresentationData
import ChatControllerInteraction
import ChatPresentationInterfaceState
import OverlayStatusController
import UndoUI
import AppBundle
import PresentationDataUtils

func appendSingGramMessageActions(actions: inout [ContextMenuItem], messages: [Message], context: AccountContext, controllerInteraction: ChatControllerInteraction, interfaceInteraction: ChatPanelInterfaceInteraction) {
    guard let message = messages.first, messages.count == 1 else {
        return
    }
    let peerId = message.id.peerId
    if peerId != context.account.peerId && peerId.namespace != Namespaces.Peer.SecretChat {
        let ghosted = SingGramSettings.isGhostChat(peerId)
        actions.append(.action(ContextMenuActionItem(text: ghosted ? "取消呢個聊天隱身" : "對呢個聊天隱身", icon: { theme in
            return generateTintedImage(image: UIImage(bundleImageName: "Chat/Context Menu/Info"), color: theme.actionSheet.primaryTextColor)
        }, action: { _, f in
            f(.dismissWithoutContent)
            if ghosted {
                SingGramSettings.setGhostChat(peerId, enabled: false)
            } else {
                SingGramSettings.enableGhostChat(peerId)
            }
            controllerInteraction.displayUndo(.info(title: nil, text: ghosted ? "呢個聊天已取消隱身" : "呢個聊天已開隱身", timeout: nil, customUndoText: nil))
        })))
        if SingGramSettings.shouldDisableReadReceipts(peerId: peerId) {
            actions.append(.action(ContextMenuActionItem(text: "呢個聊天照樣已讀", icon: { theme in
                return generateTintedImage(image: UIImage(bundleImageName: "Chat/Context Menu/Read"), color: theme.actionSheet.primaryTextColor)
            }, action: { _, f in
                f(.dismissWithoutContent)
                SingGramSettings.setReadReceiptsAllowed(peerId, allowed: true)
                controllerInteraction.displayUndo(.info(title: nil, text: "呢個聊天會照樣送已讀", timeout: nil, customUndoText: nil))
            })))
        } else if SingGramSettings.allowsReadReceipts(peerId) {
            actions.append(.action(ContextMenuActionItem(text: "恢復阻止已讀", icon: { theme in
                return generateTintedImage(image: UIImage(bundleImageName: "Chat/Context Menu/Read"), color: theme.actionSheet.primaryTextColor)
            }, action: { _, f in
                f(.dismissWithoutContent)
                SingGramSettings.setReadReceiptsAllowed(peerId, allowed: false)
                controllerInteraction.displayUndo(.info(title: nil, text: "呢個聊天已恢復阻止已讀", timeout: nil, customUndoText: nil))
            })))
        }
    }

    let source = message.text.trimmingCharacters(in: .whitespacesAndNewlines)
    guard SingGramSettings.isAiActive, SingGramSettings.isAiContextMenuEnabled, !source.isEmpty else {
        return
    }
    actions.append(.action(ContextMenuActionItem(text: "SingGram AI", icon: { theme in
        return generateTintedImage(image: UIImage(bundleImageName: "Chat/Context Menu/Edit"), color: theme.actionSheet.primaryTextColor)
    }, action: { _, f in
        f(.dismissWithoutContent)
        Queue.mainQueue().after(0.2) {
            presentSingGramAiActions(source: source, context: context, controllerInteraction: controllerInteraction, interfaceInteraction: interfaceInteraction)
        }
    })))
}

private func presentSingGramAiActions(source: String, context: AccountContext, controllerInteraction: ChatControllerInteraction, interfaceInteraction: ChatPanelInterfaceInteraction) {
    var actions: [SingGramAiAction] = [.summarize, .rewriteCantonese, .translateCantonese, .shorten, .explain, .cleanCopy, .extractTasks, .followUp]
    if SingGramSettings.isAiTranslateActionEnabled {
        actions.insert(.translateTraditional, at: 1)
    }
    if SingGramSettings.isQuickReplyIdeasEnabled {
        actions.append(.replyIdeas)
    }
    let presentationData = context.sharedContext.currentPresentationData.with { $0 }
    let sheet = ActionSheetController(presentationData: presentationData)
    var items: [ActionSheetItem] = actions.map { action in
        ActionSheetButtonItem(title: action.title, action: { [weak sheet] in
            sheet?.dismissAnimated()
            runSingGramAi(action: action, source: source, context: context, controllerInteraction: controllerInteraction, interfaceInteraction: interfaceInteraction)
        })
    }
    items.insert(ActionSheetTextItem(title: "SingGram AI"), at: 0)
    sheet.setItemGroups([
        ActionSheetItemGroup(items: items),
        ActionSheetItemGroup(items: [
            ActionSheetButtonItem(title: presentationData.strings.Common_Cancel, color: .accent, font: .bold, action: { [weak sheet] in
                sheet?.dismissAnimated()
            })
        ])
    ])
    controllerInteraction.presentController(sheet, nil)
}

private func runSingGramAi(action: SingGramAiAction, source: String, context: AccountContext, controllerInteraction: ChatControllerInteraction, interfaceInteraction: ChatPanelInterfaceInteraction) {
    let presentationData = context.sharedContext.currentPresentationData.with { $0 }
    let status = OverlayStatusController(theme: presentationData.theme, type: .loading(cancelled: nil))
    controllerInteraction.presentController(status, nil)
    SingGramAi.run(action: action, input: action == .testConnection ? "Reply with OK only." : source) { result in
        status.dismiss()
        switch result {
        case let .success(text):
            var alertActions = [
                TextAlertAction(type: .defaultAction, title: "複製", action: {
                    UIPasteboard.general.string = text
                })
            ]
            if SingGramSettings.isAiInsertResultEnabled {
                alertActions.append(TextAlertAction(type: .defaultAction, title: "插入", action: {
                    let attributed = NSAttributedString(string: text)
                    interfaceInteraction.updateTextInputStateAndMode { _, mode in
                        return (ChatTextInputState(inputText: attributed), mode)
                    }
                }))
            }
            alertActions.append(TextAlertAction(type: .genericAction, title: presentationData.strings.Common_OK, action: {}))
            controllerInteraction.presentController(textAlertController(context: context, title: action.title, text: text, actions: alertActions, actionLayout: .vertical), nil)
        case let .failure(error):
            controllerInteraction.displayUndo(.info(title: nil, text: error.message, timeout: nil, customUndoText: nil))
        }
    }
}

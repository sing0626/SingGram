import UIKit
import SwiftUI

@available(iOS 26.0, *)
private final class SingGramGlassModel: ObservableObject {
    @Published var isEnabled = false
    @Published var isCapsule = true
    @Published var cornerRadius: CGFloat = 0
    @Published var isDark = false
    @Published var level = 1
}

@available(iOS 26.0, *)
private struct SingGramGlassPlate: View {
    @ObservedObject var model: SingGramGlassModel

    var body: some View {
        let plate = Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity)
        if self.model.isEnabled {
            if self.model.isCapsule {
                plate.glassEffect(.regular.tint((self.model.isDark ? Color.black : Color.white).opacity(self.model.level == 2 ? 0.12 : 0.04)).interactive(), in: Capsule(style: .continuous))
            } else {
                plate.glassEffect(.regular.tint((self.model.isDark ? Color.black : Color.white).opacity(self.model.level == 2 ? 0.12 : 0.04)).interactive(), in: RoundedRectangle(cornerRadius: self.model.cornerRadius, style: .continuous))
            }
        } else {
            plate
        }
    }
}

@available(iOS 26.0, *)
private final class SingGramGlassController: UIHostingController<SingGramGlassPlate> {
    override var canBecomeFirstResponder: Bool {
        return false
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        self.view.backgroundColor = .clear
        self.view.isOpaque = false
    }
}

@available(iOS 26.0, *)
final class SingGramSwiftUIGlassHost {
    private let model: SingGramGlassModel
    private let controller: UIHostingController<SingGramGlassPlate>

    init() {
        let model = SingGramGlassModel()
        self.model = model
        let controller = SingGramGlassController(rootView: SingGramGlassPlate(model: model))
        self.controller = controller
        controller.view.backgroundColor = .clear
        controller.view.isOpaque = false
        controller.view.isUserInteractionEnabled = false
        controller.view.clipsToBounds = true
        controller.sizingOptions = []
        controller.safeAreaRegions = []
        controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    var view: UIView {
        return self.controller.view
    }

    func update(isEnabled: Bool, isCapsule: Bool, cornerRadius: CGFloat, animated: Bool, isDark: Bool, level: Int) {
        let apply = {
            self.model.isEnabled = isEnabled
            self.model.isCapsule = isCapsule
            self.model.cornerRadius = cornerRadius
            self.model.isDark = isDark
            self.model.level = level
        }
        if animated {
            withAnimation(.smooth(duration: 0.5), apply)
        } else {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction, apply)
        }
    }
}

import UIKit
import SwiftUI

@available(iOS 26.0, *)
private final class SingGramGlassModel: ObservableObject {
    @Published var isEnabled = false
    @Published var isCapsule = true
    @Published var cornerRadius: CGFloat = 0
}

@available(iOS 26.0, *)
private struct SingGramGlassPlate: View {
    @ObservedObject var model: SingGramGlassModel

    var body: some View {
        let plate = Color.clear.frame(maxWidth: .infinity, maxHeight: .infinity)
        if self.model.isEnabled {
            if self.model.isCapsule {
                plate.glassEffect(.regular.interactive(), in: Capsule(style: .continuous))
            } else {
                plate.glassEffect(.regular.interactive(), in: RoundedRectangle(cornerRadius: self.model.cornerRadius, style: .continuous))
            }
        } else {
            plate
        }
    }
}

@available(iOS 26.0, *)
final class SingGramSwiftUIGlassHost {
    private let model: SingGramGlassModel
    private let controller: UIHostingController<SingGramGlassPlate>

    init() {
        let model = SingGramGlassModel()
        self.model = model
        self.controller = UIHostingController(rootView: SingGramGlassPlate(model: model))
        self.controller.view.backgroundColor = .clear
        self.controller.view.isUserInteractionEnabled = false
        self.controller.sizingOptions = []
        self.controller.safeAreaRegions = []
        self.controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    var view: UIView {
        return self.controller.view
    }

    func update(isEnabled: Bool, isCapsule: Bool, cornerRadius: CGFloat, animated: Bool) {
        let apply = {
            self.model.isEnabled = isEnabled
            self.model.isCapsule = isCapsule
            self.model.cornerRadius = cornerRadius
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

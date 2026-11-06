import Foundation

func check(_ condition: Bool, _ message: String) {
    precondition(condition, message)
}
let suite = "singgram-appearance-tests-\(UUID().uuidString)"
let defaults = UserDefaults(suiteName: suite)!
defer { defaults.removePersistentDomain(forName: suite) }
defaults.set(true, forKey: "ghost_mode")
defaults.set("existing-model", forKey: "ai_model")
let store = SingGramAppearancePreferences(defaults: defaults)
check(store.listDensity == .balanced, "Fresh installs must use balanced spacing")
check(store.floatingNavigation && store.compactComposer, "Fresh UI defaults")
store.listDensity = .spacious
store.accent = .purple
store.floatingNavigation = false
store.compactComposer = false
let restored = SingGramAppearancePreferences(defaults: UserDefaults(suiteName: suite)!)
check(restored.listDensity == .spacious && restored.accent == .purple, "Persist selected appearance")
check(!restored.floatingNavigation && !restored.compactComposer, "Persist explicit false values")
defaults.set(999, forKey: "list_density")
defaults.set(-100, forKey: "accent_palette")
check(store.listDensity == .balanced && store.accent == .blue, "Invalid stored values must fall back safely")
let densities = SingGramListDensity.allCases
let padding = densities.map { $0.verticalPadding }
check(padding.allSatisfy { $0 >= 0 } && zip(padding, padding.dropFirst()).allSatisfy { $0 < $1 }, "Density spacing must increase without negative insets")
check(defaults.bool(forKey: "ghost_mode") && defaults.string(forKey: "ai_model") == "existing-model", "Appearance changes must preserve privacy and AI settings")
check(densities.map { $0.avatarDiameter }.allSatisfy { (44...60).contains($0) }, "Avatar geometry must remain bounded")
check(Set(SingGramAccent.allCases.map { $0.title }).count == SingGramAccent.allCases.count, "Distinct palette labels")

check(SingGramBubbleStyle(rawValue: 18) == nil, "A custom native radius must not be mislabeled as a preset")
check(SingGramBubbleStyle(rawValue: -1) == nil, "Invalid native radius must remain custom")

print("SingGramAppearance: defaults, persistence, invalid values and layout bounds passed")

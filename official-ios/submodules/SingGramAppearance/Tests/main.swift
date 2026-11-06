import Foundation

func check(_ condition: Bool, _ message: String) {
    precondition(condition, message)
}
let suite = "singgram-appearance-tests-\(UUID().uuidString)"
let defaults = UserDefaults(suiteName: suite)!
defer { defaults.removePersistentDomain(forName: suite) }
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
check(densities.map { $0.verticalPadding } == [0, 6, 12], "Row spacing order")
check(densities.map { $0.avatarDiameter }.allSatisfy { (44...60).contains($0) }, "Avatar geometry must remain bounded")
check(Set(SingGramAccent.allCases.map { $0.title }).count == SingGramAccent.allCases.count, "Distinct palette labels")
print("SingGramAppearance: defaults, persistence, invalid values and layout bounds passed")

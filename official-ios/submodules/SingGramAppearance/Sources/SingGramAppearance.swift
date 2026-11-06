import Foundation

public enum SingGramListDensity: Int, CaseIterable {
    case compact, balanced, spacious

    public var title: String {
        switch self {
        case .compact: return "緊湊"
        case .balanced: return "標準"
        case .spacious: return "寬鬆"
        }
    }
    public var verticalPadding: Double {
        switch self {
        case .compact: return 0
        case .balanced: return 6
        case .spacious: return 12
        }
    }
    public var avatarDiameter: Double {
        switch self {
        case .compact: return 52
        case .balanced: return 56
        case .spacious: return 60
        }
    }
}

public enum SingGramBubbleStyle: Int32, CaseIterable {
    case crisp = 8
    case soft = 16
    case rounded = 22

    public var title: String {
        switch self {
        case .crisp: return "俐落"
        case .soft: return "柔和"
        case .rounded: return "圓潤"
        }
    }
}

public enum SingGramAccent: Int, CaseIterable {
    case blue, green, purple, pink, orange
    public var title: String {
        switch self {
        case .blue: return "海藍"
        case .green: return "薄荷綠"
        case .purple: return "紫羅蘭"
        case .pink: return "玫瑰粉"
        case .orange: return "暖橙"
        }
    }
}

// UserDefaults serializes access; UI invalidations are delivered on the main queue.
public final class SingGramAppearancePreferences: @unchecked Sendable {
    private let defaults: UserDefaults

    public init(defaults: UserDefaults) { self.defaults = defaults }

    public var listDensity: SingGramListDensity {
        get {
            guard defaults.object(forKey: "list_density") != nil else { return .balanced }
            return SingGramListDensity(rawValue: defaults.integer(forKey: "list_density")) ?? .balanced
        }
        set { defaults.set(newValue.rawValue, forKey: "list_density"); notify() }
    }
    public var accent: SingGramAccent {
        get { SingGramAccent(rawValue: defaults.integer(forKey: "accent_palette")) ?? .blue }
        set { defaults.set(newValue.rawValue, forKey: "accent_palette"); notify() }
    }
    public var floatingNavigation: Bool {
        get { defaults.object(forKey: "floating_navigation") == nil || defaults.bool(forKey: "floating_navigation") }
        set { defaults.set(newValue, forKey: "floating_navigation"); notify() }
    }
    public var compactComposer: Bool {
        get { defaults.object(forKey: "compact_composer") == nil || defaults.bool(forKey: "compact_composer") }
        set { defaults.set(newValue, forKey: "compact_composer"); notify() }
    }
    private func notify() {
        if Thread.isMainThread {
            NotificationCenter.default.post(name: SingGramAppearance.changed, object: nil)
        } else {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: SingGramAppearance.changed, object: nil)
            }
        }
    }
}

public enum SingGramAppearance {
    public static let changed = Notification.Name("SingGramAppearanceChanged")
    private static let store = SingGramAppearancePreferences(defaults: UserDefaults(suiteName: "singgram") ?? .standard)

    public static var listDensity: SingGramListDensity {
        get { store.listDensity }
        set { store.listDensity = newValue }
    }
    public static var accent: SingGramAccent {
        get { store.accent }
        set { store.accent = newValue }
    }
    public static var floatingNavigation: Bool {
        get { store.floatingNavigation }
        set { store.floatingNavigation = newValue }
    }
    public static var compactComposer: Bool {
        get { store.compactComposer }
        set { store.compactComposer = newValue }
    }
}

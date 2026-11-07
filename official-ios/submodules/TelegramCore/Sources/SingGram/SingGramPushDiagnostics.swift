import Foundation

/// Observations from this process only. Never stores a device token, payload,
/// phone number, API credential, or server error description.
public final class SingGramPushDiagnostics: @unchecked Sendable {
    public enum AppleRegistration: Equatable, Sendable {
        case notObserved
        case registered
        case failed(code: Int)
    }

    public enum TelegramRegistration: Equatable, Sendable {
        case notObserved
        case registering
        case accepted
        case rejected
        case invalidated
        case failed(code: Int)
    }

    public struct Snapshot: Sendable {
        public let apple: AppleRegistration
        public let telegram: TelegramRegistration
    }

    public static let shared = SingGramPushDiagnostics()
    public static let localTestIdentifier = "singgram.notification-test"

    private let lock = NSLock()
    private var apple: AppleRegistration = .notObserved
    private var telegramByAccount: [Int64: TelegramRegistration] = [:]

    public init() {
    }

    public func recordApple(_ value: AppleRegistration) {
        self.lock.lock()
        defer { self.lock.unlock() }
        self.apple = value
    }

    public func recordTelegram(accountId: Int64, _ value: TelegramRegistration) {
        self.lock.lock()
        defer { self.lock.unlock() }
        self.telegramByAccount[accountId] = value
    }

    public func snapshot(accountId: Int64) -> Snapshot {
        self.lock.lock()
        defer { self.lock.unlock() }
        return Snapshot(apple: self.apple, telegram: self.telegramByAccount[accountId] ?? .notObserved)
    }
}

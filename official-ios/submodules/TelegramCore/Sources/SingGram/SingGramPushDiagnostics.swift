import Foundation

/// Observations from this process only. Never stores a device token, payload,
/// phone number, API hash/key, or raw server error description.
public final class SingGramPushDiagnostics: @unchecked Sendable {
    /// Only recognized machine-readable errors may reach a copied report.
    public enum FailureReason: String, Equatable, Sendable {
        case tokenInvalid = "TOKEN_INVALID"
        case tokenTypeInvalid = "TOKEN_TYPE_INVALID"
        case deviceTokenInvalid = "DEVICE_TOKEN_INVALID"
        case pushTokenInvalid = "PUSH_TOKEN_INVALID"
        case appIdInvalid = "APP_ID_INVALID"
        case apiIdInvalid = "API_ID_INVALID"
        case apiIdPublishedFlood = "API_ID_PUBLISHED_FLOOD"
        case secretInvalid = "SECRET_INVALID"
        case userIdInvalid = "USER_ID_INVALID"
        case inputConstructorInvalid = "INPUT_CONSTRUCTOR_INVALID"
        case inputLayerInvalid = "INPUT_LAYER_INVALID"
        case authKeyInvalid = "AUTH_KEY_INVALID"
        case authKeyUnregistered = "AUTH_KEY_UNREGISTERED"
        case authKeyPermEmpty = "AUTH_KEY_PERM_EMPTY"
        case sessionRevoked = "SESSION_REVOKED"
        case floodWait = "FLOOD_WAIT"
        case unclassified = "UNCLASSIFIED"

        public init(serverDescription: String?) {
            guard let value = serverDescription, value.utf8.count <= 80 else {
                self = .unclassified
                return
            }
            if let known = Self(rawValue: value) {
                self = known
            } else if value.hasPrefix("FLOOD_WAIT_"), UInt32(value.dropFirst("FLOOD_WAIT_".count)) != nil {
                self = .floodWait
            } else {
                self = .unclassified
            }
        }
    }

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
        case failed(code: Int, reason: FailureReason = .unclassified)
    }

    public struct Snapshot: Sendable {
        public let apple: AppleRegistration
        public let telegram: TelegramRegistration
        public let apiId: Int32?
    }

    public static let shared = SingGramPushDiagnostics()
    public static let localTestIdentifier = "singgram.notification-test"
    public static let retryRegistrationRequested = Notification.Name("SingGramPushDiagnostics.retryRegistration")

    private let lock = NSLock()
    private var apple: AppleRegistration = .notObserved
    private var telegramByAccount: [Int64: TelegramRegistration] = [:]
    private var apiId: Int32?

    public init() {
    }

    public func recordApple(_ value: AppleRegistration) {
        self.lock.lock()
        defer { self.lock.unlock() }
        self.apple = value
    }

    public func recordApiId(_ value: Int32) {
        self.lock.lock()
        defer { self.lock.unlock() }
        self.apiId = value
    }

    public func recordTelegram(accountId: Int64, _ value: TelegramRegistration) {
        self.lock.lock()
        defer { self.lock.unlock() }
        self.telegramByAccount[accountId] = value
    }

    public func snapshot(accountId: Int64) -> Snapshot {
        self.lock.lock()
        defer { self.lock.unlock() }
        return Snapshot(apple: self.apple, telegram: self.telegramByAccount[accountId] ?? .notObserved, apiId: self.apiId)
    }
}

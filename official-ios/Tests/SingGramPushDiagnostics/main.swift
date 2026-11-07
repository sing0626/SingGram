import Foundation
import Dispatch

func check(_ condition: Bool, _ message: String) {
    precondition(condition, message)
}

func runTests() {
    let state = SingGramPushDiagnostics()
    check(state.snapshot(accountId: 1).apple == .notObserved, "Fresh process must not claim an Apple token")
    check(state.snapshot(accountId: 1).telegram == .notObserved, "Fresh process must not claim Telegram delivery")
    check(state.snapshot(accountId: 1).apiId == nil, "Do not invent a Telegram API application id")
    state.recordApiId(123456)
    check(state.snapshot(accountId: 1).apiId == 123456, "Report the configured public API id without an API hash")
    state.recordApple(.failed(code: 3000))
    check(state.snapshot(accountId: 1).apple == .failed(code: 3000), "Preserve the registration failure code")
    state.recordApple(.registered)
    state.recordTelegram(accountId: 1, .accepted)
    state.recordTelegram(accountId: 2, .failed(code: 400))
    check(state.snapshot(accountId: 1).apple == .registered, "A later Apple callback supersedes the failure")
    check(state.snapshot(accountId: 1).telegram == .accepted, "Keep account 1's own server observation")
    check(state.snapshot(accountId: 2).telegram == .failed(code: 400), "A second account must not overwrite the first")
    check(state.snapshot(accountId: 3).telegram == .notObserved, "Do not attribute another account's success to an unobserved account")
    let captured = state.snapshot(accountId: 1)
    state.recordTelegram(accountId: 1, .invalidated)
    check(captured.telegram == .accepted && state.snapshot(accountId: 1).telegram == .invalidated, "Snapshots must remain immutable")

    DispatchQueue.concurrentPerform(iterations: 200) { index in
        let accountId = Int64(1000 + index)
        state.recordTelegram(accountId: accountId, .registering)
        state.recordTelegram(accountId: accountId, .rejected)
        check(state.snapshot(accountId: accountId).telegram == .rejected, "Concurrent callbacks must keep account observations isolated")
    }
    check(state.snapshot(accountId: 2).telegram == .failed(code: 400), "Concurrent updates must preserve existing observations")
    check(SingGramPushDiagnostics().snapshot(accountId: 1).telegram == .notObserved, "Do not persist an old process's success into a fresh launch")

    check(SingGramPushDiagnostics.FailureReason(serverDescription: "TOKEN_INVALID") == .tokenInvalid, "Preserve a recognized registration error")
    check(SingGramPushDiagnostics.FailureReason(serverDescription: "API_ID_INVALID") == .apiIdInvalid, "Distinguish app configuration from token rejection")
    check(SingGramPushDiagnostics.FailureReason(serverDescription: "FLOOD_WAIT_60") == .floodWait, "Normalize a flood-wait identifier without retaining its description")
    for description in [nil, "", "unknown private server text", "TOKEN_INVALID secret=not-for-a-report", "TOKEN_INVALID\nprivate payload", String(repeating: "A1", count: 32), String(repeating: "X", count: 1000)] as [String?] {
        check(SingGramPushDiagnostics.FailureReason(serverDescription: description) == .unclassified, "Never expose an unrecognized/raw server description")
    }
    state.recordTelegram(accountId: 2, .failed(code: 400, reason: .tokenInvalid))
    check(state.snapshot(accountId: 2).telegram == .failed(code: 400, reason: .tokenInvalid), "Keep a sanitized reason alongside the numeric error")
    check(state.snapshot(accountId: 1).telegram == .invalidated, "Recording a reason must preserve another account's result")
    print("SingGram push diagnostics: account isolation, concurrency and error-description redaction passed")
}

runTests()

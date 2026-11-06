import Foundation

func profile(_ contents: [String: Any]) throws -> Data {
    let plist = try PropertyListSerialization.data(fromPropertyList: contents, format: .xml, options: 0)
    return Data([0x30, 0x82, 0xff, 0x00]) + plist + Data([0x00, 0xff])
}

func check(_ value: Bool, _ message: String) {
    if !value {
        fatalError(message)
    }
}

check(!singGramProvisioningIncludesPush(nil), "Missing profile must not enable PushKit")
check(!singGramProvisioningIncludesPush(Data()), "Empty profile must not enable PushKit")
check(!singGramProvisioningIncludesPush(Data("aps-environment".utf8)), "A substring is not an entitlement")
check(!singGramProvisioningIncludesPush(try profile(["Name": "aps-environment", "Entitlements": [:]])), "A profile name must not enable PushKit")
check(!singGramProvisioningIncludesPush(try profile(["Entitlements": ["aps-environment": false]])), "Wrong entitlement type")
check(!singGramProvisioningIncludesPush(try profile(["Entitlements": ["aps-environment": "unknown"]])), "Unknown environment")
for environment in ["development", "production"] {
    check(singGramProvisioningIncludesPush(try profile(["Entitlements": ["aps-environment": environment]])), "Valid push profile")
}
let valid = try profile(["Entitlements": ["aps-environment": "production"]])
for index in 0..<valid.count {
    let prefix = valid.prefix(index)
    if prefix.range(of: Data("</plist>".utf8)) == nil {
        check(!singGramProvisioningIncludesPush(Data(prefix)), "Truncated profile")
    }
}
print("LaunchConfiguration: missing, malformed, unrelated, truncated and valid profile checks passed")

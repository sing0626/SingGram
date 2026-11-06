import Foundation

// A provisioning profile describes allowed capabilities, not necessarily the
// entitlements of the installed executable. Missing/invalid profiles fail closed.
func singGramProvisioningIncludesPush(_ data: Data?) -> Bool {
    guard let data else {
        return false
    }
    let plistData: Data
    if let start = data.range(of: Data("<plist".utf8)),
       let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex) {
        plistData = data.subdata(in: start.lowerBound..<end.upperBound)
    } else {
        return false
    }
    guard let profile = (try? PropertyListSerialization.propertyList(from: plistData, options: [], format: nil)) as? [String: Any],
          let entitlements = profile["Entitlements"] as? [String: Any],
          let environment = entitlements["aps-environment"] as? String else {
        return false
    }
    return environment == "development" || environment == "production"
}

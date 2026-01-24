# Sentinel Journal 🛡️

## 2025-02-18 - VpnService Allow-List Fail-Open Risk
**Vulnerability:** Android `VpnService.Builder` defaults to "Allow All" if `addAllowedApplication` is never successfully called (or if the list is empty and we don't call it).
**Learning:** Trying to "Allow None" (block all traffic via sinkhole, or allow only specific apps) requires at least one valid app to be added. Adding a non-existent app throws an exception. If caught and ignored, the builder remains in "Allow All" mode, routing ALL device traffic to the sinkhole (Internet Blackout).
**Prevention:** Always verify `addAllowedApplication` succeeds for at least one package. If the list is empty, add the app's own package (self) as a safe dummy. If that fails, abort VPN startup.

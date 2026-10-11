# NetIsolate

An Android app that cuts selected apps off from the internet, without root.
Pick apps from a list, turn on one switch, and those apps lose network access
while every other app connects as normal.

## How it works

NetIsolate uses Android's `VpnService` as a local **sinkhole**:

1. Quarantined apps are added to the VPN's allow-list with
   `addAllowedApplication`, so only their traffic enters the VPN.
2. The VPN interface takes the default IPv4 and IPv6 routes (`0.0.0.0/0` and
   `::/0`) but never reads or forwards packets, so that traffic is dropped.
3. All other apps are outside the allow-list. They bypass the VPN and use the
   normal network.

No traffic leaves the device through the VPN, and nothing is logged or sent
anywhere.

**Fail-safe for an empty list.** If `VpnService.Builder` gets no allowed apps,
it routes *every* app into the VPN, which would cut off the whole device. When
the quarantine list is empty, NetIsolate adds its own package as a harmless
placeholder. If even that fails, the service stops instead of starting.

## Features

- **Registry screen:** lists installed user-facing apps with icons. You can
  search by name or package, and quarantine an app with one tap.
- **Quarantine screen:** has a master switch (`CONNECTED` / `ISOLATED`), the
  list of blocked apps, and a `[RESTORE]` button for each one.
- **Live updates:** changing the list while the switch is on rebuilds the VPN
  at once. You don't need to restart it.
- **Persistence:** the quarantine list and switch state are saved in
  `SharedPreferences`.
- **Foreground service:** a persistent notification keeps the blocker running
  in the background.
- **UI:** Jetpack Compose and Material 3, with a dark theme. Swipe between the
  two screens or use the bottom navigation bar.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3, `HorizontalPager` |
| Concurrency | Kotlin coroutines (`Dispatchers.IO`) |
| Networking | `android.net.VpnService` |
| Build | Gradle (Kotlin DSL), AGP 8.13, version catalog |
| SDK | min 26 (Android 8.0), target / compile 36 |

## Project structure

```
app/src/main/java/com/example/netisolate/
├── MainActivity.kt            # VPN permission flow, pager, service start/stop/update
├── data/
│   ├── AppInfo.kt             # app model (name, package, icon, uid, quarantined)
│   └── AppRepository.kt       # installed-app query, quarantine list in SharedPreferences
├── service/
│   └── NetGuardService.kt     # foreground VpnService that sinkholes quarantined apps
└── ui/
    ├── RegistryScreen.kt      # searchable app list
    ├── QuarantineScreen.kt    # master switch and blocked list
    └── theme/                 # colours, typography
```

## Build and run

Requirements: Android Studio (or JDK 11+ with the Android SDK), and a device or
emulator on Android 8.0 or newer.

```powershell
./gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it
with `adb install app/build/outputs/apk/debug/app-debug.apk`, or open the
project in Android Studio and press Run.

On first use, Android asks you to allow the VPN connection. Accept it to turn on
isolation.

### Prebuilt APK (unofficial)

`NetIsolate-v1.0-unofficial-debug.apk` in the project root is an **unofficial
debug build** for quick testing. It is not a release:

- It is signed with the Android debug key, so Android will warn that it comes
  from an unknown source.
- It was built **before the IPv6 fix**, so it blocks only IPv4 traffic. Build
  from source to get the current behaviour.

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed apps in the Registry |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Keep the VPN service running |
| `BIND_VPN_SERVICE` (on the service) | Required by Android for any `VpnService` |

## Limitations

- Only one VPN can be active on Android at a time, so NetIsolate cannot run
  alongside another VPN app.
- Apps without a launcher entry are hidden from the Registry, unless they are
  user-installed.

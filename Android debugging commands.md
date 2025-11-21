### Common Commands
 - ~/Library/Android/sdk/platform-tools/adb
 - emulator

```bash
# Make sure no emulator is running
emulator -list-avds
# emulator -avd Medium_Phone_API_36.0 -netdelay none -netspeed full
adb devices       # optional, just to see what’s up
adb emu kill
emulator -avd Medium_Phone_API_36.0 -wipe-data -no-snapshot-load  -no-snapshot-save -no-boot-anim & 
adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done; echo "booted"'
# Reverse proxy for local API testing
adb reverse tcp:5173 tcp:5173 


adb logcat -t 100
adb shell am force-stop edu.aiims.surveylauncher && sleep 1 
adb shell am start -n edu.aiims.surveylauncher/.MainActivity && sleep 2 

#  AppDrawer’s discovery logs, showing the last ~15 lines where the drawer lists found launcher apps or allowed apps. 
# Handy for verifying the allow-list filtering after policy sync or debugging why an app isn’t appearing in the drawer without wading through full logs.
adb logcat -d | grep "AppDrawer.*Found\|AppDrawer.*  -" | tail -15


# 5. Install launcher APK and set DO again:
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner edu.aiims.surveylauncher/.admin.LauncherAdminReceiver
adb shell cmd package set-home-activity edu.aiims.surveylauncher/.MainActivity  # set/reset HOME

# Check if device is currently in lock task mode (and which level). 
# Use it to confirm if kiosk mode is active right now.
adb shell dumpsys activity activities | grep mLockTaskModeState 

# Verify whitelisted packages and locked state.
# dumps the Device Policy service and prints the LockTaskPolicy section (about 8
#     lines). Use it to see which packages are whitelisted for lock task/kiosk and what policy flags are active.
adb shell dumpsys device_policy | sed -n '/LockTaskPolicy/,+8p'
# similar, but just greps the lock task section; quicker skim of the whitelist and setttings
adb shell dumpsys device_policy | grep -A8 -i locktask
adb shell dumpsys device_policy | sed -n 's/.*mPackages= //p' | head -1

adb shell  dumpsys activity locktask
## HOME APP Check You can see the current and candidate HOME activities with cmd package:
# CUrrent Default (what will launch on HOME):
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

# All apps that declare a HOME/LAUNCHER intent:
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME

# Preferred/default setting (if you want the “why”):
# adb shell dumpsys package preferred-activities | grep -i home -A2



#!/bin/sh
set -eu
cd "$(dirname "$0")"
test -f GoogleService-Info.plist || { echo 'Letakkan GoogleService-Info.plist dari Firebase di iosApp terlebih dahulu.'; exit 1; }
bundle_id=$(/usr/libexec/PlistBuddy -c 'Print :BUNDLE_ID' GoogleService-Info.plist)
test "$bundle_id" = 'com.logikawan.finflow' || { echo 'Bundle ID Firebase harus com.logikawan.finflow'; exit 1; }
project_id=$(/usr/libexec/PlistBuddy -c 'Print :PROJECT_ID' GoogleService-Info.plist)
test "$project_id" = 'finflow-logikawan' || { echo 'Project Firebase harus finflow-logikawan'; exit 1; }
reversed_client_id=$(/usr/libexec/PlistBuddy -c 'Print :REVERSED_CLIENT_ID' GoogleService-Info.plist)
printf 'GOOGLE_REVERSED_CLIENT_ID = %s\n' "$reversed_client_id" > Config.xcconfig
echo 'Konfigurasi iOS disiapkan. Jalankan xcodegen generate lalu pod install.'

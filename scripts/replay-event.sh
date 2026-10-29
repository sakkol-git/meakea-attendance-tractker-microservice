#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd -P)
jar="$root/services/notification/target/notification-0.1.0-SNAPSHOT.jar"
[[ -f "$jar" ]] || { printf 'Build the notification executable jar first.\n' >&2; exit 1; }
exec java -Dloader.main=com.hrflow.events.DeadLetterReplay -cp "$jar" org.springframework.boot.loader.launch.PropertiesLauncher "$@"

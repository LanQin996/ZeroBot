#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
exec java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar ZeroBot.jar "$@"

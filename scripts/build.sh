#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
rm -rf out
mkdir -p out
find src/main/java -name '*.java' > .sources.list
javac -encoding UTF-8 -d out @.sources.list
rm -f .sources.list
if [ -d src/main/resources ]; then
  cp -R src/main/resources/. out/
fi
printf 'Build ok. Classes in %s/out\n' "$ROOT"

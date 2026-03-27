#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/build.sh
rm -rf out-test
mkdir -p out-test
find src/test/java -name '*.java' > .test-sources.list
javac -encoding UTF-8 -cp out -d out-test @.test-sources.list
rm -f .test-sources.list
java -cp out:out-test com.example.dbcompare.tests.AllTests

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./scripts/build.sh
java -cp out com.example.dbcompare.app.CompareApplication examples/demo/demo.properties

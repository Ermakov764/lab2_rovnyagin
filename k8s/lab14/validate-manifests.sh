#!/usr/bin/env bash
# Локальная проверка YAML (без доступа к кластеру).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
python3 <<PY
import glob, sys, yaml
errors = []
for path in sorted(glob.glob("$ROOT" + "/*.yaml")):
    with open(path) as f:
        try:
            docs = [d for d in yaml.safe_load_all(f) if d]
        except Exception as e:
            errors.append((path, str(e)))
            continue
    for i, d in enumerate(docs):
        if "kind" not in d or "apiVersion" not in d:
            errors.append((path, f"doc {i}: missing kind/apiVersion"))
print("Checked:", len(glob.glob("$ROOT" + "/*.yaml")), "files")
if errors:
    for p, m in errors:
        print("ERROR", p, m, file=sys.stderr)
    sys.exit(1)
print("OK: all YAML documents parse and have kind/apiVersion")
PY

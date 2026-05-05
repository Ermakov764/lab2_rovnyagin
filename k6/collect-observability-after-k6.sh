#!/usr/bin/env bash
set -euo pipefail

# Saves /api/observability snapshots for both services (pretty-printed JSON).
# Also writes short human-readable summaries (*.txt) for reports.
#
# Example:
#   MAIN_BASE_URL=http://10.60.3.33:8080 \
#   ADDITIONAL_BASE_URL=http://10.60.3.33:8081 \
#   OUT_DIR=results/cpu-0.5 \
#   STAMP=cpu-0.5-ratio-50-50 \
#   bash k6/collect-observability-after-k6.sh

MAIN_BASE_URL="${MAIN_BASE_URL:?set MAIN_BASE_URL, e.g. http://10.60.3.33:8080}"
ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:?set ADDITIONAL_BASE_URL, e.g. http://10.60.3.33:8081}"
OUT_DIR="${OUT_DIR:-results}"
STAMP="${STAMP:-$(date -u +%Y%m%dT%H%M%SZ)}"

mkdir -p "$OUT_DIR"

RAW_MAIN="${OUT_DIR}/observability-main-${STAMP}.json"
RAW_ADD="${OUT_DIR}/observability-additional-${STAMP}.json"

curl -fsS "${MAIN_BASE_URL}/api/observability" -o "$RAW_MAIN"
curl -fsS "${ADDITIONAL_BASE_URL}/api/observability" -o "$RAW_ADD"

if command -v python3 >/dev/null 2>&1; then
  python3 - <<'PY' "$RAW_MAIN" "$RAW_ADD"
import json, sys

for path in sys.argv[1:]:
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
PY

  python3 - <<'PY' "$RAW_MAIN" "$RAW_ADD" "$OUT_DIR" "$STAMP"
import json, sys, os

main_path, add_path, out_dir, stamp = sys.argv[1:]
for src, prefix in [(main_path, "main"), (add_path, "additional")]:
    out_txt = os.path.join(out_dir, f"observability-{prefix}-{stamp}.txt")
    with open(src, encoding="utf-8") as f:
        data = json.load(f)
    with open(out_txt, "w", encoding="utf-8") as w:
        windows = list(data.keys())
        for win in ("10s", "30s", "1m"):
            if win not in windows:
                continue
            w.write(f"[{win}]\n")
            ops = data.get(win) or {}
            if not ops:
                w.write("  (no events)\n\n")
                continue
            for op, st in sorted(ops.items()):
                w.write(
                    f"  {op}: n={st.get('count')} err={st.get('errors')} "
                    f"avg={float(st.get('avgMs', 0)):.2f} "
                    f"p95={float(st.get('p95Ms', 0)):.2f} "
                    f"p99={float(st.get('p99Ms', 0)):.2f} "
                    f"rps={float(st.get('rps', 0)):.2f}\n"
                )
            w.write("\n")
        for win in sorted(w for w in windows if w not in ("10s", "30s", "1m")):
            w.write(f"[{win}]\n")
            ops = data.get(win) or {}
            for op, st in sorted(ops.items()):
                w.write(
                    f"  {op}: n={st.get('count')} err={st.get('errors')} "
                    f"avg={float(st.get('avgMs', 0)):.2f} "
                    f"p95={float(st.get('p95Ms', 0)):.2f} "
                    f"p99={float(st.get('p99Ms', 0)):.2f} "
                    f"rps={float(st.get('rps', 0)):.2f}\n"
                )
            w.write("\n")
PY
else
  echo "WARN: python3 not found; JSON left minified. Install python3 for pretty-print + .txt summaries." >&2
fi

echo "Saved:"
echo "  ${RAW_MAIN}"
echo "  ${RAW_ADD}"
if command -v python3 >/dev/null 2>&1; then
  echo "  ${OUT_DIR}/observability-main-${STAMP}.txt"
  echo "  ${OUT_DIR}/observability-additional-${STAMP}.txt"
fi

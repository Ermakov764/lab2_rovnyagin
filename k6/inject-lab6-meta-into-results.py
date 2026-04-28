#!/usr/bin/env python3
"""
Добавляет lab6_meta в старые k6 summary JSON (прогоны до handleSummary в cinema-lab6-constant.js).

TARGET_VUS и post_share выводятся из имени файла lab6-summary-*.json;
duration, base_url, k6_route — из аргументов (как было в реальном прогоне).

Пример:
  python3 k6/inject-lab6-meta-into-results.py ./results \\
    --base-url http://192.168.1.242:8080 \\
    --duration 90s \\
    --k6-route server-to-server

Затем: python3 k6/plot_lab6_from_results.py ./results -o ./png_k6

Предпочтительно переснять прогоны актуальным run-lab6-ratio-sweep.sh — тогда meta совпадёт с фактом.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


def die(msg: str) -> None:
    print(msg, file=sys.stderr)
    raise SystemExit(1)


def post_share_from_stem(stem: str) -> float:
    if re.search(r"post05-get95", stem, re.I):
        return 0.05
    if re.search(r"post50-get50", stem, re.I):
        return 0.5
    if re.search(r"post95-get05", stem, re.I):
        return 0.95
    die(f"{stem}: не распознана смесь в имени (ожидаются post05-get95 / post50-get50 / post95-get05)")


def vus_from_stem(stem: str) -> int:
    m = re.search(r"-vus-(\d+)", stem, re.I)
    if not m:
        die(f"{stem}: нет суффикса -vus-<число> в имени файла")
    return int(m.group(1))


def main() -> None:
    p = argparse.ArgumentParser(description="Вставить lab6_meta в старые lab6-summary JSON")
    p.add_argument("results", type=Path, help="Каталог с cpu-*")
    p.add_argument("--base-url", required=True, help="BASE_URL того прогона")
    p.add_argument("--duration", required=True, help="DURATION (например 90s)")
    p.add_argument(
        "--k6-route",
        required=True,
        choices=("pc-to-server", "server-to-server"),
        help="K6_ROUTE",
    )
    p.add_argument("--film-id", default="1", help="FILM_ID (по умолчанию 1)")
    p.add_argument(
        "--force",
        action="store_true",
        help="Перезаписать уже существующий lab6_meta",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Только показать, какие файлы изменились бы",
    )
    args = p.parse_args()
    base = args.results.resolve()
    if not base.is_dir():
        die(f"Нет каталога: {base}")

    bu = str(args.base_url).strip().rstrip("/")
    if not bu:
        die("--base-url не должен быть пустым")

    changed = 0
    for folder in sorted(base.iterdir()):
        if not folder.is_dir() or not folder.name.lower().startswith("cpu-"):
            continue
        for path in sorted(folder.glob("lab6-summary-*.json")):
            with path.open(encoding="utf-8") as f:
                data = json.load(f)
            if "lab6_meta" in data and isinstance(data["lab6_meta"], dict) and not args.force:
                continue
            stem = path.stem
            meta = {
                "target_vus": vus_from_stem(stem),
                "duration": str(args.duration).strip(),
                "base_url": bu,
                "k6_route": args.k6_route,
                "post_share": post_share_from_stem(stem),
                "film_id": str(args.film_id),
                "scenario": "cinema-lab6-constant",
            }
            if args.dry_run:
                print(f"would update: {path}")
                changed += 1
                continue
            data["lab6_meta"] = meta
            with path.open("w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
                f.write("\n")
            print(f"OK: {path}")
            changed += 1

    if changed == 0:
        print(
            "Нет файлов без lab6_meta (или нет lab6-summary-*.json). "
            "Уже заполнено — для перезаписи: --force",
            file=sys.stderr,
        )


if __name__ == "__main__":
    main()

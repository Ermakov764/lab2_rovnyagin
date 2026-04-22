#!/usr/bin/env python3
"""
LAB4: строит график среднего времени отклика (avg http_req_duration, мс) vs целевых VU
по JSON-файлам, сохранённым k6 с флагом --summary-export.
"""
import argparse
import json
import re
import sys
from pathlib import Path
from typing import List, Optional, Tuple


def extract_avg_ms(summary: dict) -> Optional[float]:
    metrics = summary.get("metrics") or {}
    trend = metrics.get("http_req_duration") or {}
    values = trend.get("values") or {}
    avg = values.get("avg")
    return float(avg) if avg is not None else None


def collect_points(reports_dir: Path) -> List[Tuple[int, float]]:
    pat = re.compile(r"^summary-vus-(\d+)\.json$")
    out: List[Tuple[int, float]] = []
    for path in sorted(reports_dir.glob("summary-vus-*.json")):
        m = pat.match(path.name)
        if not m:
            continue
        vus = int(m.group(1))
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        avg = extract_avg_ms(data)
        if avg is None:
            print(f"Предупреждение: нет metrics.http_req_duration.values.avg в {path}", file=sys.stderr)
            continue
        out.append((vus, avg))
    out.sort(key=lambda t: t[0])
    return out


def main() -> None:
    p = argparse.ArgumentParser(description="График avg времени отклика k6 vs VU")
    p.add_argument(
        "reports_dir",
        nargs="?",
        default="k6/reports",
        type=Path,
        help="Каталог с summary-vus-*.json (по умолчанию k6/reports)",
    )
    p.add_argument(
        "-o",
        "--output",
        type=Path,
        default=None,
        help="PNG-файл (по умолчанию <reports_dir>/avg_vs_vus.png)",
    )
    args = p.parse_args()
    reports_dir = args.reports_dir.resolve()
    if not reports_dir.is_dir():
        print(f"Каталог не найден: {reports_dir}", file=sys.stderr)
        sys.exit(1)

    points = collect_points(reports_dir)
    if len(points) < 2:
        print("Нужно минимум 2 файла summary-vus-*.json с валидными avg.", file=sys.stderr)
        sys.exit(1)

    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print("Установите matplotlib: pip install matplotlib", file=sys.stderr)
        sys.exit(1)

    vus_list, avg_list = zip(*points)
    out_path = args.output or (reports_dir / "avg_vs_vus.png")

    plt.figure(figsize=(8, 5))
    plt.plot(vus_list, avg_list, "o-", linewidth=2, markersize=8)
    plt.xlabel("Целевые VU (TARGET_VUS)")
    plt.ylabel("Среднее время отклика http_req_duration (мс)")
    plt.title("k6: средняя задержка vs нагрузка (POST /api/films + GET analytics)")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(out_path, dpi=150)
    print(f"Сохранено: {out_path}")


if __name__ == "__main__":
    main()

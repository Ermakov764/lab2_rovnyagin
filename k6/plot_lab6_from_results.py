#!/usr/bin/env python3
"""
LAB6: по папкам с отчётами k6 (cpu-0.5-..., cpu-1-..., …) строит PNG: средние
POST/GET (Trend) vs соотношение 5/95, 50/50, 95/5. Один график на папку CPU.

Структура:
  <results>/
    cpu-0.5-20250424/
      lab6-summary-post05-get95-vus-30.json
      lab6-summary-post50-get50-vus-30.json
      lab6-summary-post95-get05-vus-30.json
    cpu-1.0-20250424/ ...

Выход: <results>/plots/lab6-cpu-0.5.png (и т.д.)

Зависимость: pip install "matplotlib>=3.7"

Запуск с ПК (после scp results/):
  python3 k6/plot_lab6_from_results.py /путь/к/ermakov_k6/results
На k6:
  python3 plot_lab6_from_results.py ~/ermakov_k6/results
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import List, Optional, Tuple

METRIC_POST = "k6_post_film_ms"
METRIC_GET = "k6_get_analytics_ms"

# Имя файла k6: ...-post05-get95-... → доля POST 0.05; post50 → 0.5; post95 → 0.95
MIX_PATTERNS: List[Tuple[re.Pattern, float, str]] = [
    (re.compile(r"post05-get95"), 0.05, "5% POST / 95% GET"),
    (re.compile(r"post50-get50"), 0.5, "50% / 50%"),
    (re.compile(r"post95-get05"), 0.95, "95% POST / 5% GET"),
]


def extract_avg_from_trend(trend: dict) -> Optional[float]:
    if not trend:
        return None
    values = trend.get("values")
    if isinstance(values, dict) and "avg" in values:
        return float(values["avg"])
    a = trend.get("avg")
    return float(a) if a is not None else None


def extract_metric(summary: dict, name: str) -> Optional[float]:
    return extract_avg_from_trend((summary.get("metrics") or {}).get(name) or {})


def mix_info_from_stem(name: str) -> Optional[Tuple[float, str]]:
    for pat, post_share, label in MIX_PATTERNS:
        if pat.search(name):
            return (post_share, label)
    return None


def cpu_key_from_dir(dirname: str) -> str:
    m = re.match(r"^cpu-([\d.]+)", dirname, re.I)
    if m:
        return m.group(1)
    return re.sub(r"[^\w.-]+", "_", dirname)


def collect_one_cpu_folder(
    folder: Path,
) -> Optional[Tuple[List[Tuple[str, float, float]], str]]:
    """
    Возвращает (rows, cpu_key) — rows: (x_label, post_ms, get_ms) по возрастанию post_share;
    cpu_key — из имени папки cpu-0.5-...
    """
    rows: List[Tuple[float, str, Optional[float], Optional[float], str]] = []
    for path in sorted(folder.glob("lab6-summary-*.json")):
        info = mix_info_from_stem(path.stem)
        if not info:
            continue
        post_share, x_label = info
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        p = extract_metric(data, METRIC_POST)
        g = extract_metric(data, METRIC_GET)
        if p is None and g is None:
            print(f"Предупреждение: нет {METRIC_POST}/{METRIC_GET} в {path}", file=sys.stderr)
        rows.append((post_share, x_label, p, g, str(path.name)))

    if not rows:
        return None
    rows.sort(key=lambda t: t[0])
    out: List[Tuple[str, float, float]] = []
    for _ps, x_label, p, g, _fn in rows:
        if p is not None and g is not None:
            out.append((x_label, p, g))
        else:
            print(
                f"Пропуск точки {x_label} в {folder.name}: post={p} get={g}",
                file=sys.stderr,
            )

    if not out:
        return None
    cpu = cpu_key_from_dir(folder.name)
    return (out, cpu)


def plot_folder(out_dir: Path, series: List[Tuple[str, float, float]], cpu: str) -> None:
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print('Установите: pip install "matplotlib>=3.7"', file=sys.stderr)
        sys.exit(1)

    labels = [r[0] for r in series]
    y_post = [r[1] for r in series]
    y_get = [r[2] for r in series]

    fig, ax = plt.subplots(figsize=(9, 5.5))
    x = range(len(labels))
    ax.plot(
        x,
        y_post,
        "o-",
        label="POST /api/films (avg мс)",
        color="#1f77b4",
        linewidth=2,
        markersize=8,
    )
    ax.plot(
        x,
        y_get,
        "s-",
        label="GET analytics (avg мс)",
        color="#ff7f0e",
        linewidth=2,
        markersize=7,
    )
    ax.set_xticks(list(x), labels, rotation=15, ha="right")
    ax.set_ylabel("Среднее время отклика (мс)")
    ax.set_xlabel("Соотношение POST/GET (constant VU)")
    ax.set_title(f"Лаб. 6, APP_CPU (лимит) ≈ {cpu} vCPU; TARGET_VUS / DURATION из JSON")
    ax.legend(loc="upper left", framealpha=0.9)
    ax.grid(True, alpha=0.3)
    fig.tight_layout()

    out_dir.mkdir(parents=True, exist_ok=True)
    safe = re.sub(r"[^\w.+-]+", "-", cpu)
    path = out_dir / f"lab6-cpu-{safe}.png"
    fig.savefig(path, dpi=150)
    plt.close(fig)
    print(f"OK: {path}")


def main() -> None:
    p = argparse.ArgumentParser(
        description="PNG-графики lab6: POST/GET vs микс (5/95, 50/50, 95/5) на каждую папку cpu-*"
    )
    p.add_argument(
        "results",
        type=Path,
        nargs="?",
        default=Path("results"),
        help="Каталог с подпапками cpu-0.5-..., cpu-1-... (по умолчанию ./results)",
    )
    p.add_argument(
        "-o",
        "--out-dir",
        type=Path,
        default=None,
        help="Куда писать PNG (по умолчанию <results>/plots)",
    )
    args = p.parse_args()
    base = args.results.resolve()
    if not base.is_dir():
        print(f"Нет каталога: {base}", file=sys.stderr)
        sys.exit(1)

    out = (args.out_dir or (base / "plots")).resolve()

    subdirs = [d for d in sorted(base.iterdir()) if d.is_dir() and d.name.lower().startswith("cpu-")]
    if not subdirs:
        print(f"Нет подкаталогов cpu-* в {base}", file=sys.stderr)
        sys.exit(1)

    for d in subdirs:
        res = collect_one_cpu_folder(d)
        if not res:
            print(f"Пропуск (нет lab6-*.json): {d.name}", file=sys.stderr)
            continue
        series, cpu = res
        plot_folder(out, series, cpu)

    if not any((out.glob("lab6-cpu-*.png"))):
        print("Ни одного PNG не сгенерировано", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()

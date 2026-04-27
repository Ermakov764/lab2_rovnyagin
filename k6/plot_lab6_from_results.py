#!/usr/bin/env python3
"""
LAB6 (п. 10 ТЗ): три графика — по одному на каждое соотношение вставка/чтение
(5/95, 50/50, 95/5). На каждом: ось X — лимит CPU контейнера app (шаг 0.5 vCPU:
0.5, 1.0, 1.5, 2), ось Y — среднее время отклика (мс) при постоянных VU.

Структура входа:
  <results>/
    cpu-0.5/   (или cpu-0.5-20250424/)
      lab6-summary-post05-get95-vus-30.json
      lab6-summary-post50-get50-vus-30.json
      lab6-summary-post95-get05-vus-30.json
    cpu-1.0/ …

Выход (по умолчанию <results>/plots/):
  lab6-vs-cpu-mix-5-95.png
  lab6-vs-cpu-mix-50-50.png
  lab6-vs-cpu-mix-95-5.png

Зависимость: pip install "matplotlib>=3.7"
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

# паттерн в имени файла, подпись для заголовка, суффикс PNG
MIX_SPECS: List[Tuple[re.Pattern, str, str]] = [
    (re.compile(r"post05-get95"), "5% POST / 95% GET (вставка/чтение 5/95)", "mix-5-95"),
    (re.compile(r"post50-get50"), "50% POST / 50% GET (50/50)", "mix-50-50"),
    (re.compile(r"post95-get05"), "95% POST / 5% GET (95/5)", "mix-95-5"),
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


def cpu_float_from_dir(dirname: str) -> Optional[float]:
    m = re.match(r"^cpu-([\d.]+)", dirname, re.I)
    if not m:
        return None
    return float(m.group(1))


def extract_vus_from_stem(stem: str) -> Optional[int]:
    m = re.search(r"-vus-(\d+)", stem, re.I)
    return int(m.group(1)) if m else None


def find_mix_json(folder: Path, mix_pat: re.Pattern) -> Optional[Path]:
    for path in sorted(folder.glob("lab6-summary-*.json")):
        if mix_pat.search(path.stem):
            return path
    return None


def collect_series_for_mix(
    base: Path, mix_pat: re.Pattern
) -> Tuple[List[Tuple[float, float, float]], Optional[int]]:
    """
    Собирает точки (cpu_limit, post_ms, get_ms), отсортированные по cpu.
    Второе значение — TARGET_VUS из имён файлов (если одинаковый).
    """
    rows: List[Tuple[float, float, float, Optional[int]]] = []
    for folder in sorted(base.iterdir()):
        if not folder.is_dir() or not folder.name.lower().startswith("cpu-"):
            continue
        cpu_v = cpu_float_from_dir(folder.name)
        if cpu_v is None:
            continue
        path = find_mix_json(folder, mix_pat)
        if not path:
            print(
                f"Предупреждение: в {folder.name} нет lab6-summary-* для этого микса",
                file=sys.stderr,
            )
            continue
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        p = extract_metric(data, METRIC_POST)
        g = extract_metric(data, METRIC_GET)
        vus = extract_vus_from_stem(path.stem)
        if p is None or g is None:
            print(
                f"Пропуск {path.name}: нет {METRIC_POST}/{METRIC_GET} (post={p} get={g})",
                file=sys.stderr,
            )
            continue
        rows.append((cpu_v, p, g, vus))

    rows.sort(key=lambda t: t[0])
    if not rows:
        return [], None

    vus_vals = {r[3] for r in rows if r[3] is not None}
    vus_one = next(iter(vus_vals)) if len(vus_vals) == 1 else None
    if len(vus_vals) > 1:
        print(
            f"Предупреждение: разные TARGET_VUS в точках: {vus_vals}; в заголовке VU не указан",
            file=sys.stderr,
        )

    out = [(r[0], r[1], r[2]) for r in rows]
    return out, vus_one


def plot_mix(
    out_dir: Path,
    series: List[Tuple[float, float, float]],
    mix_label: str,
    file_slug: str,
    vus: Optional[int],
) -> None:
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print('Установите: pip install "matplotlib>=3.7"', file=sys.stderr)
        sys.exit(1)

    x_cpu = [r[0] for r in series]
    y_post = [r[1] for r in series]
    y_get = [r[2] for r in series]

    fig, ax = plt.subplots(figsize=(9, 5.5))
    ax.plot(
        x_cpu,
        y_post,
        "o-",
        label="POST /api/films (среднее, мс)",
        color="#1f77b4",
        linewidth=2,
        markersize=8,
    )
    ax.plot(
        x_cpu,
        y_get,
        "s-",
        label="GET analytics (среднее, мс)",
        color="#ff7f0e",
        linewidth=2,
        markersize=7,
    )
    ax.set_xlabel("Лимит CPU контейнера app (vCPU), шаг 0.5")
    ax.set_ylabel("Среднее время отклика (мс)")
    ax.set_xticks(x_cpu)
    ax.grid(True, alpha=0.3)
    ax.legend(loc="upper right", framealpha=0.9)

    vu_part = f"constant VU = {vus}; " if vus is not None else "constant VU (см. JSON); "
    title = (
        f"Лаб. 6: время отклика vs CPU\n{vu_part}{mix_label}"
    )
    ax.set_title(title, fontsize=11)

    fig.tight_layout()
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"lab6-vs-cpu-{file_slug}.png"
    fig.savefig(path, dpi=150)
    plt.close(fig)
    print(f"OK: {path}")


def main() -> None:
    p = argparse.ArgumentParser(
        description="Лаб. 6 ТЗ п.10: 3 PNG — время отклика vs лимит CPU для смесей 5/95, 50/50, 95/5"
    )
    p.add_argument(
        "results",
        type=Path,
        nargs="?",
        default=Path("results"),
        help="Каталог с подпапками cpu-0.5, cpu-1.0, …",
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

    subdirs = [
        d for d in base.iterdir() if d.is_dir() and d.name.lower().startswith("cpu-")
    ]
    if not subdirs:
        print(f"Нет подкаталогов cpu-* в {base}", file=sys.stderr)
        sys.exit(1)

    generated = 0
    for mix_pat, mix_label, file_slug in MIX_SPECS:
        series, vus = collect_series_for_mix(base, mix_pat)
        if len(series) < 1:
            print(f"Пропуск графика {file_slug}: нет ни одной точки", file=sys.stderr)
            continue
        plot_mix(out, series, mix_label, file_slug, vus)
        generated += 1

    if generated == 0 or not any(out.glob("lab6-vs-cpu-*.png")):
        print("Ни одного PNG не сгенерировано (нужны cpu-* с lab6-summary-*.json)", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()

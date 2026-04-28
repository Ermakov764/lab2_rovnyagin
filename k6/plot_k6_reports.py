#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LAB4 / LAB6: графики по summary JSON k6 (формат как у одногрупниц: zil/k6/plot_k6_reports.py).

LAB4 (по умолчанию)
  K6_REPORTS_DIR или ./k6/reports — summary-vus-*.json
  → avg_vs_vus.png (оси: VU, метрики post_ms / get_ms или legacy k6_*)

LAB6 (--lab6)
  Папка: k6/reports-lab6-pc или k6/reports-lab6-s2s
  Имена: *cpu10_mix50.json (cpu05→0.5 vCPU, mix05|50|95 → 5/95, 50/50, 95/5)
  → lab6_latency_vs_cpu.png — три панели по смесям, ось X = CPU.

Зависимость: pip install matplotlib
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

from matplotlib.lines import Line2D

try:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except ImportError:
    print("Python:", sys.executable, file=sys.stderr)
    print('Установите: pip install "matplotlib>=3.7"', file=sys.stderr)
    raise SystemExit(1)

_LAB6_CPU_MIX = re.compile(r"cpu(\d+)_mix(\d+)\.json$", re.IGNORECASE)
MIX_TICK = {"05": "5/95", "50": "50/50", "95": "95/5"}
CPU_STEPS: tuple[float, ...] = (0.5, 1.0, 1.5, 2.0)


def get_avg(m: dict | None) -> float | None:
    if not m:
        return None
    val = m.get("values")
    if isinstance(val, dict) and val.get("avg") is not None:
        return float(val["avg"])
    if m.get("avg") is not None:
        return float(m["avg"])
    return None


def post_get_avgs(metrics: dict) -> tuple[float | None, float | None]:
    """post_ms/get_ms (как в cinema-lab6) или старые k6_*."""
    pa = get_avg(metrics.get("post_ms") or {})
    ga = get_avg(metrics.get("get_ms") or {})
    if pa is None:
        pa = get_avg(metrics.get("k6_post_film_ms") or {})
    if ga is None:
        ga = get_avg(metrics.get("k6_get_analytics_ms") or {})
    return pa, ga


def cpu_code_to_float(c: str) -> float:
    return int(c, 10) / 10.0


def _load_lab6_matrix(root: Path) -> dict[float, dict[str, tuple[float, float]]]:
    by_cpu: dict[float, dict[str, tuple[float, float]]] = {}
    for f in root.glob("*.json"):
        m = _LAB6_CPU_MIX.search(f.name)
        if not m:
            continue
        cpu_key, mix_key = m.group(1), m.group(2)
        if mix_key not in ("05", "50", "95"):
            continue
        data = json.loads(f.read_text(encoding="utf-8"))
        met = data.get("metrics") or {}
        pa, ga = post_get_avgs(met)
        if pa is None or ga is None:
            print(
                "Нет post_ms/get_ms (или legacy k6_*) в",
                f,
                file=sys.stderr,
            )
            continue
        cpu = cpu_code_to_float(cpu_key)
        by_cpu.setdefault(cpu, {})[mix_key] = (pa, ga)
    return by_cpu


def _ensure_lab6_complete(by_cpu: dict[float, dict[str, tuple[float, float]]], root: Path) -> None:
    if len(by_cpu) < 1:
        print("LAB6: нет файлов вида *cpu10_mix50.json в", root, file=sys.stderr)
        raise SystemExit(1)
    missing: list[tuple[float, str]] = []
    for cpu in CPU_STEPS:
        mixes = by_cpu.get(cpu, {})
        for mix in ("05", "50", "95"):
            if mix not in mixes:
                missing.append((cpu, mix))
    if missing:
        print("LAB6: не хватает файлов для панелей:", file=sys.stderr)
        for cpu, mix in missing:
            print(f"  CPU {cpu:g}, mix {mix}", file=sys.stderr)
        print(
            "Ожидаются все комбинации cpu05|10|15|20 × mix05|50|95 в",
            root,
            file=sys.stderr,
        )
        raise SystemExit(1)


def plot_lab6_cpu_axis(root: Path, title_suffix: str = "") -> None:
    by_cpu = _load_lab6_matrix(root)
    _ensure_lab6_complete(by_cpu, root)

    mix_order = ("05", "50", "95")
    subtitles = (
        "POST/GET = 5/95",
        "POST/GET = 50/50",
        "POST/GET = 95/5",
    )
    xs = list(CPU_STEPS)

    fig, axes = plt.subplots(1, 3, figsize=(14, 5), sharey=True)
    supt = "Среднее время отклика (мс) vs CPU (лаб. 6)"
    if title_suffix:
        supt += f" — {title_suffix}"
    fig.suptitle(supt, fontsize=12, y=1.02)

    for ax, mix, sub in zip(axes, mix_order, subtitles, strict=True):
        posts = [by_cpu[c][mix][0] for c in CPU_STEPS]
        gets = [by_cpu[c][mix][1] for c in CPU_STEPS]
        ax.plot(xs, posts, "o-", color="#1f77b4", linewidth=2, markersize=7)
        ax.plot(xs, gets, "o-", color="#ff7f0e", linewidth=2, markersize=7)
        for x, p in zip(xs, posts, strict=True):
            ax.annotate(
                f"{p:.1f}",
                (x, p),
                textcoords="offset points",
                xytext=(0, 8),
                ha="center",
                fontsize=8,
            )
        for x, g in zip(xs, gets, strict=True):
            ax.annotate(
                f"{g:.1f}",
                (x, g),
                textcoords="offset points",
                xytext=(0, -14),
                ha="center",
                fontsize=8,
                color="#555555",
            )
        ax.set_xticks(xs)
        ax.set_xlabel("Лимит CPU (vCPU)")
        ax.set_title(sub, fontsize=10)
        ax.grid(True, linestyle=":", alpha=0.55)

    axes[0].set_ylabel("Среднее время отклика (мс)")
    legend_elem = [
        Line2D(
            [0],
            [0],
            color="#1f77b4",
            marker="o",
            linestyle="-",
            linewidth=2,
            label="POST /api/films",
        ),
        Line2D(
            [0],
            [0],
            color="#ff7f0e",
            marker="o",
            linestyle="-",
            linewidth=2,
            label="GET /api/tickets/analytics/max-viewers",
        ),
    ]
    fig.legend(
        handles=legend_elem,
        loc="upper center",
        ncol=2,
        bbox_to_anchor=(0.5, 1.08),
        frameon=False,
    )
    fig.tight_layout()
    out = root / "lab6_latency_vs_cpu.png"
    plt.savefig(out, dpi=150, bbox_inches="tight")
    plt.close()
    print("OK:", out)


def main() -> None:
    here = Path(__file__).resolve().parent
    ap = argparse.ArgumentParser(description="Графики k6: LAB4 (VU) или LAB6 (CPU, три панели).")
    ap.add_argument(
        "--lab6",
        action="store_true",
        help="LAB6: ось X = CPU, три панели → lab6_latency_vs_cpu.png",
    )
    ap.add_argument(
        "--title-suffix",
        default="",
        help='Подпись (напр. "ПК → сервер")',
    )
    ap.add_argument(
        "dir",
        nargs="?",
        default="",
        help="Папка с JSON (LAB6: reports-lab6-pc; LAB4: reports)",
    )
    args = ap.parse_args()

    if args.lab6:
        default_l6 = here / "reports-lab6-pc"
        root = Path(args.dir) if args.dir else Path(
            os.environ.get("K6_LAB6_DIR", str(default_l6))
        )
        if not root.is_dir():
            print("Нет папки:", root, file=sys.stderr)
            raise SystemExit(1)
        suffix = args.title_suffix.strip()
        if not suffix:
            if "reports-lab6-pc" in root.parts:
                suffix = "ПК → сервер (pc-to-server)"
            elif "reports-lab6-s2s" in root.parts:
                suffix = "сервер → сервер (server-to-server)"
        plot_lab6_cpu_axis(root, title_suffix=suffix)
        return

    default_dir = here / "reports"
    root = Path(args.dir) if args.dir else Path(
        os.environ.get("K6_REPORTS_DIR", str(default_dir))
    )
    name_pat = re.compile(r"^summary-vus-(\d+)\.json$")
    post_pts: list[tuple[int, float]] = []
    get_pts: list[tuple[int, float]] = []

    for f in sorted(root.glob("summary-vus-*.json")):
        m = name_pat.match(f.name)
        if not m:
            continue
        vus = int(m.group(1))
        data = json.loads(f.read_text(encoding="utf-8"))
        met = data.get("metrics") or {}
        a, b = post_get_avgs(met)
        if a is not None:
            post_pts.append((vus, a))
        if b is not None:
            get_pts.append((vus, b))

    if len(post_pts) < 1 or len(get_pts) < 1:
        print(
            "Нужны post_ms и get_ms (или k6_*) в summary-vus-*.json.",
            file=sys.stderr,
        )
        raise SystemExit(1)

    post_pts.sort(key=lambda t: t[0])
    get_pts.sort(key=lambda t: t[0])
    vp = [a[0] for a in post_pts]
    yp = [a[1] for a in post_pts]
    vg = [a[0] for a in get_pts]
    yg = [a[1] for a in get_pts]

    plt.figure(figsize=(9, 5.5))
    plt.plot(
        vp, yp, "o-", label="POST /api/films", color="#1f77b4", linewidth=2, markersize=7
    )
    plt.plot(
        vg,
        yg,
        "s-",
        label="GET /analytics/max-viewers",
        color="#ff7f0e",
        linewidth=2,
        markersize=7,
    )
    plt.legend()
    plt.xlabel("TARGET_VUS")
    plt.ylabel("Среднее время (мс)")
    plt.title("k6: задержка vs TARGET_VUS")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    out = root / "avg_vs_vus.png"
    plt.savefig(out, dpi=150)
    plt.close()
    print("OK:", out)


if __name__ == "__main__":
    main()

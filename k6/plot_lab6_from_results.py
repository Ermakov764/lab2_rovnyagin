#!/usr/bin/env python3
"""
Графики «время отклика vs лимит CPU» по подпапкам cpu-* (лаб. 6 или лаб. 8).
Три PNG — смеси 5/95, 50/50, 95/5; ось X — vCPU, ось Y — среднее POST/GET (мс).

Каждый summary JSON должен содержать объект lab6_meta (пишет k6 при handleSummary:
лаб. 8 — cinema-lab8-constant.js, см. run-lab8-ratio-sweep.sh). Без него скрипт завершится с ошибкой.

Одна серия прогонов: одинаковые TARGET_VUS в именах файлов и одинаковое ядро
lab6_meta между всеми k6/cpu-runs/cpu-* и всеми смесиями — иначе ошибка.

Структура входа:
  k6/cpu-runs/
    cpu-0.5/
      lab6-summary-post05-get95-vus-30.json
      ...
    cpu-1.0/ ...

Выход (по умолчанию <out-dir> или <каталог>/plots):
  lab6-vs-cpu-mix-5-95.png
  lab6-vs-cpu-mix-50-50.png
  lab6-vs-cpu-mix-95-5.png

Если в одной папке cpu-* лежат и *-vus-30.json и *-vus-400.json — укажите --vus 400
(иначе будет ошибка «разный TARGET_VUS»).

Зависимость: pip install "matplotlib>=3.7"
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import List, Optional, Tuple

METRIC_POST = "post_ms"
METRIC_GET = "get_ms"
METRIC_POST_LEGACY = "k6_post_film_ms"
METRIC_GET_LEGACY = "k6_get_analytics_ms"

MIX_SPECS: List[Tuple[re.Pattern, str, str]] = [
    (re.compile(r"post05-get95"), "5% POST / 95% GET (5/95)", "mix-5-95"),
    (re.compile(r"post50-get50"), "50% POST / 50% GET (50/50)", "mix-50-50"),
    (re.compile(r"post95-get05"), "95% POST / 5% GET (95/5)", "mix-95-5"),
]


def die(msg: str, code: int = 1) -> None:
    print(msg, file=sys.stderr)
    raise SystemExit(code)


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


def extract_post_get_avg(summary: dict) -> tuple[Optional[float], Optional[float]]:
    m = summary.get("metrics") or {}
    p = extract_metric(summary, METRIC_POST) or extract_metric(summary, METRIC_POST_LEGACY)
    g = extract_metric(summary, METRIC_GET) or extract_metric(summary, METRIC_GET_LEGACY)
    return p, g


def cpu_float_from_dir(dirname: str) -> Optional[float]:
    m = re.match(r"^cpu-([\d.]+)", dirname, re.I)
    if not m:
        return None
    return float(m.group(1))


def extract_vus_from_stem(stem: str) -> Optional[int]:
    m = re.search(r"-vus-(\d+)", stem, re.I)
    return int(m.group(1)) if m else None


def find_mix_json(
    folder: Path,
    mix_pat: re.Pattern,
    vus_filter: Optional[int] = None,
    summary_prefix: str = "lab6-summary",
) -> Optional[Path]:
    paths = sorted(
        p
        for p in folder.glob(f"{summary_prefix}-*.json")
        if mix_pat.search(p.stem)
    )
    if not paths:
        return None
    vus_per_path: List[Tuple[Path, Optional[int]]] = [
        (p, extract_vus_from_stem(p.stem)) for p in paths
    ]
    if vus_filter is not None:
        matched = [p for p, v in vus_per_path if v == vus_filter]
        return matched[0] if matched else None
    vus_set = {v for _, v in vus_per_path if v is not None}
    if len(vus_set) > 1:
        die(
            f"{folder.name}: в одном миксе лежат JSON с разным TARGET_VUS: {sorted(vus_set)}. "
            f"Удалите лишние {summary_prefix}-*-vus-*.json или укажите: "
            f"python3 k6/plot_lab6_from_results.py … --vus 400"
        )
    if not vus_set:
        return paths[0]
    only_v = next(iter(vus_set))
    matched = [p for p, v in vus_per_path if v == only_v]
    return sorted(matched)[0]


def missing_mix_issue(
    folder: Path,
    mix_pat: re.Pattern,
    vus_filter: Optional[int],
    summary_prefix: str = "lab6-summary",
) -> str:
    candidates = [
        p
        for p in folder.glob(f"{summary_prefix}-*.json")
        if mix_pat.search(p.stem)
    ]
    if not candidates:
        return (
            f"В {folder.name} нет {summary_prefix}-* для микса «{mix_pat.pattern}»"
        )
    if vus_filter is not None:
        vus_found = sorted(
            {
                extract_vus_from_stem(p.stem)
                for p in candidates
                if extract_vus_from_stem(p.stem) is not None
            }
        )
        return (
            f"В {folder.name} нет *-vus-{vus_filter}.json для микса «{mix_pat.pattern}» "
            f"(в папке только VU: {vus_found}). Нужен прогон: TARGET_VUS={vus_filter} "
            f"и RESULT_CPU для {folder.name}."
        )
    return f"В {folder.name} нет {summary_prefix}-* для микса «{mix_pat.pattern}»"


def parse_lab6_meta(data: dict, path: Path) -> dict:
    raw = data.get("lab6_meta")
    if not isinstance(raw, dict):
        die(
            f"{path}: нет объекта lab6_meta — переснимите прогоны "
            f"(например k6/run-lab8-ratio-sweep.sh с актуальным cinema-lab8-constant.js), "
            f"чтобы summary содержал поле lab6_meta."
        )
    try:
        tv = int(raw["target_vus"])
    except (KeyError, TypeError, ValueError):
        die(f"{path}: lab6_meta.target_vus отсутствует или не целое число")
    if tv < 1:
        die(f"{path}: lab6_meta.target_vus должно быть >= 1, сейчас {tv}")
    for key in ("duration", "base_url", "k6_route"):
        if key not in raw or not str(raw.get(key, "")).strip():
            die(f"{path}: lab6_meta.{key} обязательно и не должно быть пустым")
    return raw


def meta_core_tuple(meta: dict) -> Tuple:
    bu = str(meta.get("base_url", "")).rstrip("/")
    return (
        int(meta["target_vus"]),
        str(meta["duration"]),
        bu,
        str(meta["k6_route"]),
    )


def route_human(route: str) -> str:
    r = route.strip().lower().replace("_", "-")
    if r in ("pc-to-server", "pctoserver", "from-pc"):
        return "сценарий: ПК → сервер"
    if r in ("server-to-server", "servertoserver", "s2s"):
        return "сценарий: сервер → сервер"
    return f"сценарий: {route}"


def shorten(s: str, max_len: int = 52) -> str:
    s = s.strip()
    if len(s) <= max_len:
        return s
    return s[: max_len - 3] + "..."


def build_subtitle(meta: dict) -> str:
    vu = int(meta["target_vus"])
    parts = [
        f"TARGET_VUS={vu}",
        f"DURATION={meta['duration']}",
        f"BASE_URL={shorten(str(meta['base_url']))}",
        route_human(str(meta["k6_route"])),
    ]
    return "; ".join(parts)


def collect_series_for_mix(
    base: Path,
    mix_pat: re.Pattern,
    vus_filter: Optional[int] = None,
    summary_prefix: str = "lab6-summary",
) -> Tuple[List[Tuple[float, float, float]], int, dict, List[str]]:
    rows: List[Tuple[float, float, float, int, dict]] = []
    issues: List[str] = []
    meta_signatures: set = set()

    for folder in sorted(base.iterdir()):
        if not folder.is_dir() or not folder.name.lower().startswith("cpu-"):
            continue
        cpu_v = cpu_float_from_dir(folder.name)
        if cpu_v is None:
            continue
        path = find_mix_json(folder, mix_pat, vus_filter, summary_prefix)
        if not path:
            issues.append(missing_mix_issue(folder, mix_pat, vus_filter, summary_prefix))
            continue
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        meta = parse_lab6_meta(data, path)
        sig = meta_core_tuple(meta)
        meta_signatures.add(sig)

        p, g = extract_post_get_avg(data)
        vus = extract_vus_from_stem(path.stem)
        if vus is None:
            issues.append(f"{path.name}: в имени файла нет суффикса -vus-<число>")
            continue
        mv = int(meta["target_vus"])
        if mv != vus:
            issues.append(
                f"{path.name}: lab6_meta.target_vus ({mv}) ≠ суффикс в имени ({vus})"
            )

        if p is None or g is None:
            issues.append(
                f"{path.name}: нет метрик post_ms/get_ms или k6_* (post={p} get={g})"
            )
            continue
        rows.append((cpu_v, p, g, vus, meta))

    rows.sort(key=lambda t: t[0])
    if not rows:
        return [], 0, {}, issues

    vus_vals = {r[3] for r in rows}
    if len(vus_vals) > 1:
        issues.append(
            f"Микс «{mix_pat.pattern}»: разные TARGET_VUS в именах файлов: {sorted(vus_vals)}"
        )
    vus_one = next(iter(vus_vals))

    if len(meta_signatures) > 1:
        issues.append(
            f"Микс «{mix_pat.pattern}»: разный lab6_meta между папками cpu-*: {meta_signatures}"
        )

    meta_canonical = rows[0][4]
    out = [(r[0], r[1], r[2]) for r in rows]
    return out, vus_one, meta_canonical, issues


def plot_mix(
    out_dir: Path,
    series: List[Tuple[float, float, float]],
    mix_label: str,
    file_slug: str,
    meta: dict,
    title_tag: str = "Лаб. 6: время отклика vs CPU",
    png_prefix: str = "lab6-vs-cpu",
    post_legend: str = "POST /api/films (среднее, мс)",
    get_legend: str = "GET analytics (среднее, мс)",
) -> None:
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        die('Установите: pip install "matplotlib>=3.7"')

    x_cpu = [r[0] for r in series]
    y_post = [r[1] for r in series]
    y_get = [r[2] for r in series]

    fig, ax = plt.subplots(figsize=(9, 6.2))
    ax.plot(
        x_cpu,
        y_post,
        "o-",
        label=post_legend,
        color="#1f77b4",
        linewidth=2,
        markersize=8,
    )
    ax.plot(
        x_cpu,
        y_get,
        "s-",
        label=get_legend,
        color="#ff7f0e",
        linewidth=2,
        markersize=7,
    )
    ax.set_xlabel("Лимит CPU контейнера app (vCPU), шаг 0.5")
    ax.set_ylabel("Среднее время отклика (мс)")
    ax.set_xticks(x_cpu)
    ax.grid(True, alpha=0.3)
    ax.legend(loc="upper right", framealpha=0.9)

    line1 = title_tag
    line2 = build_subtitle(meta)
    ax.set_title(f"{line1}\n{line2}\n{mix_label}", fontsize=10)

    fig.tight_layout()
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{png_prefix}-{file_slug}.png"
    fig.savefig(path, dpi=150)
    plt.close(fig)
    print(f"OK: {path}")


def main() -> None:
    p = argparse.ArgumentParser(
        description="Лаб. 6: 3 PNG — время отклика vs лимит CPU для смесей 5/95, 50/50, 95/5"
    )
    p.add_argument(
        "results",
        type=Path,
        nargs="?",
        default=Path("k6/cpu-runs"),
        help="Каталог с подкаталогами cpu-0.5, cpu-1.0, …",
    )
    p.add_argument(
        "-o",
        "--out-dir",
        type=Path,
        default=None,
        help="Куда писать PNG (по умолчанию <каталог_cpu>/plots)",
    )
    p.add_argument(
        "--vus",
        type=int,
        default=None,
        metavar="N",
        help="Брать только <summary-prefix>-*-vus-N.json (если в папках смешаны 30 и 400)",
    )
    p.add_argument(
        "--summary-prefix",
        default="lab6-summary",
        metavar="PREFIX",
        help="Префикс имён JSON (lab6-summary или lab8-summary)",
    )
    p.add_argument(
        "--title-tag",
        default="Лаб. 6: время отклика vs CPU",
        metavar="TEXT",
        help="Первая строка заголовка графика",
    )
    p.add_argument(
        "--png-prefix",
        default="lab6-vs-cpu",
        metavar="PREFIX",
        help="Префикс имён PNG (lab6-vs-cpu или lab8-vs-cpu)",
    )
    p.add_argument(
        "--post-legend",
        default="POST /api/films (среднее, мс)",
        metavar="TEXT",
        help="Подпись легенды для POST-ветки (лаб. 8: POST /api/viewers)",
    )
    p.add_argument(
        "--get-legend",
        default="GET analytics (среднее, мс)",
        metavar="TEXT",
        help="Подпись легенды для GET-ветки",
    )
    args = p.parse_args()
    base = args.results.resolve()
    if not base.is_dir():
        die(f"Нет каталога: {base}")

    out = (args.out_dir or (base / "plots")).resolve()

    subdirs = [
        d for d in base.iterdir() if d.is_dir() and d.name.lower().startswith("cpu-")
    ]
    if not subdirs:
        die(f"Нет подкаталогов cpu-* в {base}")

    all_issues: List[str] = []
    meta_cores: List[Tuple] = []
    to_plot: List[Tuple[List[Tuple[float, float, float]], dict, str, str]] = []

    for mix_pat, mix_label, file_slug in MIX_SPECS:
        series, _vus, meta, issues = collect_series_for_mix(
            base,
            mix_pat,
            vus_filter=args.vus,
            summary_prefix=args.summary_prefix,
        )
        all_issues.extend(issues)
        if len(series) < 1:
            all_issues.append(f"Нет точек для графика {file_slug}")
            continue
        meta_cores.append(meta_core_tuple(meta))
        to_plot.append((series, meta, file_slug, mix_label))

    if len(set(meta_cores)) > 1:
        all_issues.append(
            "Между смесиями 5/95, 50/50, 95/5 различается lab6_meta — "
            "нужна одна серия прогонов с теми же TARGET_VUS, DURATION, BASE_URL, K6_ROUTE."
        )

    if all_issues:
        for msg in all_issues:
            print(f"Ошибка: {msg}", file=sys.stderr)
        die("Исправьте k6/cpu-runs/cpu-* или переснимите прогоны.")

    for series, meta, file_slug, mix_label in to_plot:
        plot_mix(
            out,
            series,
            mix_label,
            file_slug,
            meta,
            title_tag=args.title_tag,
            png_prefix=args.png_prefix,
            post_legend=args.post_legend,
            get_legend=args.get_legend,
        )

    if not to_plot or not any(out.glob(f"{args.png_prefix}-*.png")):
        die("Ни одного PNG не сгенерировано")


if __name__ == "__main__":
    main()

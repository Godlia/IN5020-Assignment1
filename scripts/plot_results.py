"""
Draws the graphs required by exercise_1.pdf, section "4 Result", plus a few
comparison figures for the report.

Input: one folder per run in output/, named <variant><T>, holding the client's
output file <variant>.txt and the servers' queue logs server<A-E>-queue.log:
  output/naive_server50/naive_server.txt   output/naive_server20/naive_server.txt
  output/server_cache50/server_cache.txt   output/server_cache20/server_cache.txt
  output/client_cache50/client_cache.txt   output/client_cache20/client_cache.txt

Required by the handout (one set per run, i.e. per variant and T):
  <RUN>_turnaround   x: query number,   y: turn-around time
  <RUN>_queues       x: Unix timestamp, y: queue size, one panel per server

Comparison figures across naive server / server cache / client cache and T = 50 / T = 20:
  compare_turnaround     rolling mean of turn-around time per query
  compare_distribution   cumulative distribution (ECDF) of turn-around time
  compare_averages       average turn-around split into waiting / execution / rest
  compare_by_method      average turn-around per remote method
  compare_queues         heatmap: peak queue size per server over time, all runs

It also writes summary.csv (the numbers behind the figures, for tables in the report).

Usage (from the repo folder):
  python scripts/plot_results.py [output_dir] [--graph-dir graph] [--format png,pdf] [--window 50]

Defaults: reads <repo>/output, writes to <repo>/graph (existing files with the same
name are overwritten). Times switch from ms to seconds automatically when the
slowest query takes longer than 10 s. Only matplotlib, numpy and pandas are needed.
"""
import argparse
import re
from datetime import datetime, timezone
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.colors import LinearSegmentedColormap
from matplotlib.lines import Line2D
from matplotlib.patches import Patch
from matplotlib.ticker import FuncFormatter, MaxNLocator

# --------------------------------------------------------------------------- #
# Constants
# --------------------------------------------------------------------------- #
MODES = ["naive_server", "server_cache", "client_cache"]   # folder / file names
DELAYS = [50, 20]
SERVERS = ["A", "B", "C", "D", "E"]
METHODS = ["getPopulationofCountry", "getNumberofCities",
           "getNumberofCountries", "getNumberofCountriesMM"]

METHOD_SHORT = {"getPopulationofCountry": "getPopulation\nofCountry",
                "getNumberofCities": "getNumber\nofCities",
                "getNumberofCountries": "getNumber\nofCountries",
                "getNumberofCountriesMM": "getNumber\nofCountriesMM"}

LATENCY_MS = 80        # simulated same-zone latency (handout, 2.3)
OVERLOAD_LIMIT = 18    # proxy redirects when the waiting list reaches this (handout, 1)

# One fixed colour per cache mode, the same in every figure.
MODE_COLORS = {"naive_server": "#2a78d6", "server_cache": "#eb6834", "client_cache": "#1baf7a"}
MODE_LABELS = {"naive_server": "Naive server (no cache)", "server_cache": "Server-side cache",
               "client_cache": "Client-side cache"}
MODE_SHORT = {"naive_server": "Naive", "server_cache": "Server\ncache", "client_cache": "Client\ncache"}

# Time unit for every figure. main() switches to seconds when the runs are slow;
# the parsed data is divided by UNIT_DIV once, right after parsing.
UNIT, UNIT_DIV = "ms", 1


def fmt_t(v) -> str:
    """A time value in the current unit, e.g. '113 ms' or '12.4 s'."""
    return f"{v:,.0f} ms" if UNIT == "ms" else f"{v:,.1f} s"


def num_t(v) -> str:
    """Same as fmt_t but without the unit (for labels on bars)."""
    return f"{v:,.0f}" if UNIT == "ms" else f"{v:,.1f}"

# Text and chrome
INK, MUTED, FAINT, GRID, SURFACE = "#0b0b0b", "#52514e", "#8a8984", "#e6e5e0", "#ffffff"

# Parts of the turn-around time (stacked bars): neutral greys, so they never
# get confused with the mode colours above.
PART_COLORS = {"Waiting": "#b9b8b2", "Execution": "#6b6a66", "Rest": "#2b2b29"}

# Sequential blue ramp for the queue heatmap (light = empty, dark = long queue)
QUEUE_CMAP = LinearSegmentedColormap.from_list(
    "queue", ["#f4f8fd", "#b7d3f6", "#6da7ec", "#2a78d6", "#1c5cab", "#0d366b"])

# The query line ends with "(turnaround time: .., execution time: .., waiting time: ..,
# processed by Server ..)". search() instead of match() so a marker added to the
# line later (e.g. "(client cache)") does not break the parser.
LINE_RE = re.compile(
    r"^(?P<result>\S+) (?P<method>\w+) .*?Zone:(?P<zone>\d+).*?"
    r"\(turnaround time: (?P<turnaround>\d+) ms, execution time: (?P<execution>\d+) ms, "
    r"waiting time: (?P<waiting>\d+) ms, processed by Server (?P<server>[^)]+)\)"
)
QUEUE_RE = re.compile(r"^(?P<ts>\S+) (?P<event>\w+) queue-size=(?P<size>\d+)")


# --------------------------------------------------------------------------- #
# Parsing
# --------------------------------------------------------------------------- #
def parse_client_output(path: Path) -> pd.DataFrame:
    """One row per query, in input order. The summary lines at the end are skipped."""
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        m = LINE_RE.search(line)
        if m:
            row = m.groupdict()
            row["client_hit"] = "client cache" in line.lower()
            rows.append(row)
    df = pd.DataFrame(rows)
    for col in ["zone", "turnaround", "execution", "waiting"]:
        df[col] = df[col].astype(int)
    df.insert(0, "query", range(1, len(df) + 1))
    # Whatever turn-around is not execution or waiting: network latency sleep,
    # RMI overhead, proxy lookup. 0 as long as waiting = turnaround - execution.
    df["rest"] = (df["turnaround"] - df["execution"] - df["waiting"]).clip(lower=0)
    return df


def parse_queue_log(path: Path) -> pd.DataFrame:
    """Timestamp (Unix ms, or ISO-8601) + queue size after each event."""
    rows = [m.groupdict() for m in map(QUEUE_RE.match, path.read_text(encoding="utf-8").splitlines()) if m]
    df = pd.DataFrame(rows)
    df["size"] = df["size"].astype(int)
    if df["ts"].str.isdigit().all():
        df["ts"] = df["ts"].astype("int64")
    else:
        df["ts"] = pd.to_datetime(df["ts"]).astype("int64") // 1_000_000
    return df.sort_values("ts", kind="stable").reset_index(drop=True)


def binned_max(q: pd.DataFrame, t0: int, t1: int, bin_ms: int) -> pd.DataFrame:
    """Largest queue size seen in each bin_ms window between t0 and t1 (Unix ms).

    The queue size is a step function: between two events it keeps the value of
    the last event, so a bin with no events inherits the previous size.
    """
    edges = np.arange(t0, t1 + bin_ms, bin_ms)
    idx = np.clip((q["ts"].to_numpy() - t0) // bin_ms, 0, len(edges) - 2)
    peak = pd.Series(q["size"].to_numpy()).groupby(idx).max()
    out = pd.Series(np.nan, index=range(len(edges) - 1))
    out.loc[peak.index] = peak.to_numpy()
    # carry the size forward into bins without events (the queue did not change)
    last = pd.Series(q["size"].to_numpy()).groupby(idx).last()
    carry = pd.Series(np.nan, index=out.index)
    carry.loc[last.index] = last.to_numpy()
    carry = carry.ffill().shift(1).fillna(0)
    out = out.fillna(carry)
    return pd.DataFrame({"ts": edges[:-1], "peak": out.to_numpy()})


# --------------------------------------------------------------------------- #
# Style helpers
# --------------------------------------------------------------------------- #
def style():
    plt.rcParams.update({
        "figure.facecolor": SURFACE, "axes.facecolor": SURFACE, "savefig.facecolor": SURFACE,
        "font.size": 10, "axes.titlesize": 12, "axes.titleweight": "bold", "axes.titlelocation": "left",
        "axes.titlepad": 22, "axes.labelsize": 10, "axes.labelcolor": MUTED,
        "axes.edgecolor": GRID, "axes.linewidth": 0.8, "axes.spines.top": False, "axes.spines.right": False,
        "axes.grid": True, "grid.color": GRID, "grid.linewidth": 0.7, "axes.axisbelow": True,
        "xtick.color": MUTED, "ytick.color": MUTED, "xtick.major.size": 0, "ytick.major.size": 0,
        "text.color": INK, "legend.frameon": False, "legend.fontsize": 9,
        "figure.dpi": 110, "savefig.dpi": 180, "savefig.bbox": "tight",
    })


def subtitle(ax, text):
    """Grey line under the (left-aligned) title."""
    ax.text(0, 1.02, text, transform=ax.transAxes, color=MUTED, fontsize=9, va="bottom")


def latency_line(ax, orientation="h", top=None):
    """Hairline at the 80 ms simulated latency: no query can be faster than this.

    Skipped on a linear axis where 80 ms would sit right on top of zero (top = axis maximum).
    """
    lat = LATENCY_MS / UNIT_DIV
    if top is not None and lat < 0.03 * top:
        return
    if orientation == "h":
        ax.axhline(lat, color=FAINT, linewidth=0.9, zorder=1)
        ax.text(1.0, lat, f" {LATENCY_MS} ms latency", transform=ax.get_yaxis_transform(),
                color=MUTED, fontsize=8, va="center", ha="left")
    else:
        ax.axvline(lat, color=FAINT, linewidth=0.9, zorder=1)
        ax.text(lat, 1.0, f"{LATENCY_MS} ms latency ", transform=ax.get_xaxis_transform(),
                color=MUTED, fontsize=8, va="top", ha="right", rotation=90)


def unix_axis(ax, t0: int, t1: int):
    """Keep the x data in Unix ms (as the handout asks) but tick at round seconds after t0."""
    span_s = (t1 - t0) / 1000
    step_s = next(s for s in [1, 2, 5, 10, 20, 30, 60, 120, 300] if span_s / s <= 10)
    ax.set_xticks(t0 + np.arange(0, span_s + step_s, step_s) * 1000)
    ax.xaxis.set_major_formatter(FuncFormatter(lambda x, _: f"+{(x - t0) / 1000:.0f} s"))


def unix_label(t0: int) -> str:
    human = datetime.fromtimestamp(t0 / 1000, tz=timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    return f"Unix timestamp (ms), shown as time since t₀ = {t0}  ({human})"


def save(fig, path_stem: Path, formats):
    for fmt in formats:
        fig.savefig(path_stem.with_suffix(f".{fmt}"))
    plt.close(fig)



# --------------------------------------------------------------------------- #
# Per-run figures (required by the handout)
# --------------------------------------------------------------------------- #
def plot_turnaround(df: pd.DataFrame, mode: str, delay: int, window: int, out: Path, formats):
    color = MODE_COLORS[mode]
    fig, ax = plt.subplots(figsize=(12, 4.8))

    roll = df["turnaround"].rolling(window, center=True, min_periods=1)
    lo, hi = roll.quantile(0.10), roll.quantile(0.90)
    ax.fill_between(df["query"], lo, hi, color=color, alpha=0.14, linewidth=0,
                    label=f"10th–90th percentile ({window}-query window)")

    served = df[~df["client_hit"]]
    ax.scatter(served["query"], served["turnaround"], s=7, color=color, alpha=0.45,
               linewidths=0, label="One query", zorder=2)
    hits = df[df["client_hit"]]
    if len(hits):
        ax.scatter(hits["query"], hits["turnaround"], s=9, facecolors="none", edgecolors=MUTED,
                   linewidths=0.6, label="Answered from client cache", zorder=2)

    ax.plot(df["query"], roll.median(), color=INK, linewidth=1.6,
            label=f"Rolling median ({window} queries)", zorder=3)

    t = df["turnaround"]
    top = max(t.max() * 1.05, 2 * LATENCY_MS / UNIT_DIV)
    latency_line(ax, top=top)
    ax.set_title(f"{MODE_LABELS[mode]} — turn-around time per query, T = {delay} ms")
    subtitle(ax, f"{len(df):,} queries  ·  mean {fmt_t(t.mean())}  ·  median {fmt_t(t.median())}"
                 f"  ·  95th pct {fmt_t(t.quantile(0.95))}  ·  max {fmt_t(t.max())}")
    ax.set_xlabel("Query number (order in the input file)")
    ax.set_ylabel(f"Turn-around time ({UNIT})")
    ax.set_xlim(0, len(df) + 1)
    ax.set_ylim(0, top)
    ax.legend(loc="upper center", bbox_to_anchor=(0.5, -0.13), ncols=3)
    save(fig, out, formats)


def plot_queues(logs: dict, mode: str, delay: int, out: Path, formats):
    """One panel per server. Thin line: every logged event. Filled: peak per time window."""
    color = MODE_COLORS[mode]
    t0 = min(q["ts"].min() for q in logs.values())
    t1 = max(q["ts"].max() for q in logs.values())
    bin_ms = max(100, int(round((t1 - t0) / 300, -2)))   # ~300 windows per run
    ymax = max(q["size"].max() for q in logs.values())
    show_limit = ymax >= OVERLOAD_LIMIT * 2 / 3   # draw the limit only when the queue gets close

    fig, axes = plt.subplots(len(logs), 1, figsize=(12, 1.55 * len(logs) + 0.9),
                             sharex=True, sharey=True)
    axes = np.atleast_1d(axes)
    for ax, (server, q) in zip(axes, logs.items()):
        b = binned_max(q, t0, t1, bin_ms)
        ax.fill_between(b["ts"], b["peak"], step="post", color=color, alpha=0.22, linewidth=0)
        ax.step(b["ts"], b["peak"], where="post", color=color, linewidth=1.2)
        ax.step(q["ts"], q["size"], where="post", color=INK, linewidth=0.35, alpha=0.35)

        peak_row = q.loc[q["size"].idxmax()]
        ax.annotate(f"peak {int(peak_row['size'])}", (peak_row["ts"], peak_row["size"]),
                    xytext=(4, 2), textcoords="offset points", fontsize=8, color=MUTED)
        ax.set_ylabel(f"Server {server}", rotation=0, ha="right", va="center", color=INK)
        if show_limit:
            ax.axhline(OVERLOAD_LIMIT, color=FAINT, linewidth=0.9)
            ax.text(1.0, OVERLOAD_LIMIT, f" overload {OVERLOAD_LIMIT}", transform=ax.get_yaxis_transform(),
                    color=MUTED, fontsize=8, va="center", ha="left")
        ax.set_ylim(0, max(ymax, OVERLOAD_LIMIT if show_limit else 0) * 1.12 + 1)
        ax.yaxis.set_major_locator(MaxNLocator(integer=True, nbins=3))
        ax.grid(axis="x", visible=False)

    unix_axis(axes[-1], t0, t1)
    axes[-1].set_xlim(t0, t1)
    axes[-1].set_xlabel(unix_label(t0))
    axes[0].set_title(f"{MODE_LABELS[mode]} — waiting-list (queue) size per server, T = {delay} ms")
    subtitle(axes[0], f"Filled: largest queue size in each {bin_ms} ms window.  Thin line: every logged event.  "
                      f"Highest queue in this run: {ymax} (proxy overload limit is {OVERLOAD_LIMIT}).")
    fig.supylabel("Queue size (requests waiting)", color=MUTED, fontsize=10, x=0.0)
    fig.align_ylabels(axes)
    save(fig, out, formats)


# --------------------------------------------------------------------------- #
# Comparison figures
# --------------------------------------------------------------------------- #
def mode_legend(fig_or_ax, modes, **kw):
    handles = [Line2D([], [], color=MODE_COLORS[m], linewidth=2.2, label=MODE_LABELS[m]) for m in modes]
    return fig_or_ax.legend(handles=handles, **kw)


def plot_turnaround_comparison(clients: dict, window: int, out: Path, formats):
    delays = [d for d in DELAYS if any(k[1] == d for k in clients)]
    fig, axes = plt.subplots(len(delays), 1, figsize=(12, 3.3 * len(delays) + 0.6), sharex=True, sharey=True)
    axes = np.atleast_1d(axes)
    for ax, d in zip(axes, delays):
        for m in MODES:
            if (m, d) not in clients:
                continue
            df = clients[(m, d)]
            ax.plot(df["query"], df["turnaround"].rolling(window, center=True, min_periods=1).mean(),
                    color=MODE_COLORS[m], linewidth=1.6)
        ax.text(0.005, 0.95, f"T = {d} ms", transform=ax.transAxes, fontweight="bold", va="top")
        ax.set_ylabel(f"Turn-around ({UNIT})")
    top = max(line.get_ydata().max() for ax in axes for line in ax.get_lines()) * 1.05
    for ax in axes:
        latency_line(ax, top=top)
    axes[0].set_ylim(0, top)
    axes[0].set_title(f"Turn-around time over the run — rolling mean of {window} queries")
    subtitle(axes[0], "Same input file in every run, so the lines can be compared query by query.")
    mode_legend(axes[0], [m for m in MODES if any(k[0] == m for k in clients)],
                loc="upper right", ncols=3)
    axes[-1].set_xlabel("Query number (order in the input file)")
    axes[-1].set_xlim(0, max(len(df) for df in clients.values()) + 1)
    save(fig, out, formats)


def plot_distribution(clients: dict, out: Path, formats):
    """ECDF: for any time x, the share of queries that finished within x ms."""
    delays = [d for d in DELAYS if any(k[1] == d for k in clients)]
    fig, axes = plt.subplots(1, len(delays), figsize=(6 * len(delays), 4.4), sharey=True)
    axes = np.atleast_1d(axes)
    for ax, d in zip(axes, delays):
        for m in MODES:
            if (m, d) not in clients:
                continue
            # log axis: a 0 ms answer (client-cache hit) is drawn at 1 ms
            t = np.sort(np.maximum(clients[(m, d)]["turnaround"].to_numpy(), 1 / UNIT_DIV))
            y = np.arange(1, len(t) + 1) / len(t)
            ax.step(t, y, where="post", color=MODE_COLORS[m], linewidth=1.8)
            med = np.median(t)
            ax.plot([med], [0.5], "o", ms=5, color=MODE_COLORS[m], mec=SURFACE, mew=1.5, zorder=4)
        latency_line(ax, "v")
        ax.axhline(0.5, color=GRID, linewidth=0.9)
        ax.set_xscale("log")
        ax.xaxis.set_major_formatter(FuncFormatter(lambda x, _: f"{x:g}"))
        ax.set_xlabel(f"Turn-around time ({UNIT}, log scale; 0 ms drawn at 1 ms)")
        ax.set_ylim(0, 1.01)
        ax.yaxis.set_major_formatter(FuncFormatter(lambda v, _: f"{v:.0%}"))
        ax.set_title(f"T = {d} ms", pad=8)
    axes[0].set_ylabel("Share of queries finished within x")
    fig.suptitle("Distribution of turn-around times (dot = median)", x=0.06, ha="left",
                 fontweight="bold", fontsize=12)
    mode_legend(fig, [m for m in MODES if any(k[0] == m for k in clients)],
                loc="upper right", ncols=3, bbox_to_anchor=(0.98, 1.0))
    fig.tight_layout()
    save(fig, out, formats)


def plot_average_breakdown(clients: dict, out: Path, formats):
    """Average turn-around per run as a stacked bar: waiting + execution + rest."""
    delays = [d for d in DELAYS if any(k[1] == d for k in clients)]
    fig, axes = plt.subplots(1, len(delays), figsize=(6.2 * len(delays), 3.4), sharex=True)
    axes = np.atleast_1d(axes)
    xmax = max(df["turnaround"].mean() for df in clients.values()) * 1.18
    for ax, d in zip(axes, delays):
        modes = [m for m in MODES if (m, d) in clients]
        ypos = np.arange(len(modes))[::-1]
        for y, m in zip(ypos, modes):
            df = clients[(m, d)]
            left = 0.0
            for part, col in [("Waiting", "waiting"), ("Execution", "execution"), ("Rest", "rest")]:
                v = df[col].mean()
                ax.barh(y, v, left=left, height=0.62, color=PART_COLORS[part],
                        edgecolor=SURFACE, linewidth=2)
                if v >= 0.07 * xmax:
                    ax.text(left + v / 2, y, num_t(v), ha="center", va="center", fontsize=8,
                            color=SURFACE if part != "Waiting" else INK)
                left += v
            ax.text(df["turnaround"].mean() + xmax * 0.01, y, fmt_t(df["turnaround"].mean()),
                    va="center", fontsize=9, color=INK, fontweight="bold")
            # colour chip carries the mode identity (the bars are neutral grey)
            ax.add_patch(plt.Rectangle((-0.02, y - 0.31), 0.012, 0.62, transform=ax.get_yaxis_transform(),
                                       color=MODE_COLORS[m], clip_on=False))
        ax.set_yticks(ypos, [MODE_LABELS[m] for m in modes])
        ax.tick_params(axis="y", pad=12, labelcolor=INK)
        ax.set_xlim(0, xmax)
        ax.grid(axis="y", visible=False)
        ax.set_xlabel(f"Average time per query ({UNIT})")
        ax.set_title(f"T = {d} ms", pad=8)
    handles = [Patch(color=PART_COLORS["Waiting"], label="Waiting"),
               Patch(color=PART_COLORS["Execution"], label="Execution"),
               Patch(color=PART_COLORS["Rest"], label="Rest (latency, RMI, proxy lookup)")]
    fig.suptitle("Where the turn-around time goes — average per query", x=0.02, ha="left",
                 fontweight="bold", fontsize=12)
    fig.legend(handles=handles, loc="upper right", ncols=3, bbox_to_anchor=(0.99, 1.0))
    fig.tight_layout()
    save(fig, out, formats)


def plot_by_method(clients: dict, out: Path, formats):
    """Average turn-around per remote method, grouped by cache mode."""
    delays = [d for d in DELAYS if any(k[1] == d for k in clients)]
    fig, axes = plt.subplots(1, len(delays), figsize=(6.4 * len(delays), 4.2), sharey=True)
    axes = np.atleast_1d(axes)
    width = 0.26
    for ax, d in zip(axes, delays):
        modes = [m for m in MODES if (m, d) in clients]
        x = np.arange(len(METHODS))
        for i, m in enumerate(modes):
            means = clients[(m, d)].groupby("method")["turnaround"].mean().reindex(METHODS)
            offs = (i - (len(modes) - 1) / 2) * width
            bars = ax.bar(x + offs, means, width, color=MODE_COLORS[m], edgecolor=SURFACE, linewidth=2)
            ax.bar_label(bars, labels=[num_t(v) for v in means], fontsize=7.5, color=MUTED, padding=2)
        ax.set_xticks(x, [METHOD_SHORT[mt] for mt in METHODS], fontsize=8.5)
        ax.grid(axis="x", visible=False)
        ax.set_title(f"T = {d} ms", pad=8)
    top = max(ax.get_ylim()[1] for ax in axes)
    for ax in axes:
        latency_line(ax, top=top)
    axes[0].set_ylabel(f"Average turn-around time ({UNIT})")
    fig.suptitle("Average turn-around time per remote method", x=0.02, ha="left",
                 fontweight="bold", fontsize=12)
    handles = [Patch(color=MODE_COLORS[m], label=MODE_LABELS[m]) for m in MODES
               if any(k[0] == m for k in clients)]
    fig.legend(handles=handles, loc="upper right", ncols=3, bbox_to_anchor=(0.99, 1.0))
    fig.tight_layout()
    save(fig, out, formats)


def plot_queue_heatmap(queues: dict, out: Path, formats, bin_ms: int = 1000):
    """All runs in one figure: rows = servers, colour = largest queue in each second."""
    runs = [(m, d) for d in DELAYS for m in MODES if (m, d) in queues]
    vmax = max(q["size"].max() for logs in queues.values() for q in logs.values())
    longest = max(max(q["ts"].max() for q in logs.values()) - min(q["ts"].min() for q in logs.values())
                  for logs in queues.values()) / 1000

    fig, axes = plt.subplots(len(runs), 1, figsize=(12, 1.25 * len(runs) + 1.2), sharex=True)
    axes = np.atleast_1d(axes)
    for ax, (m, d) in zip(axes, runs):
        logs = queues[(m, d)]
        t0 = min(q["ts"].min() for q in logs.values())
        t1 = max(q["ts"].max() for q in logs.values())
        grid = np.vstack([binned_max(q, t0, t1, bin_ms)["peak"].to_numpy() for q in logs.values()])
        mesh = ax.imshow(grid, aspect="auto", cmap=QUEUE_CMAP, vmin=0, vmax=vmax, interpolation="nearest",
                         extent=(0, grid.shape[1] * bin_ms / 1000, len(logs) - 0.5, -0.5))
        ax.set_yticks(range(len(logs)), list(logs.keys()), fontsize=7.5)
        ax.grid(False)
        for s in ax.spines.values():
            s.set_visible(False)
        ax.set_ylabel(f"{MODE_SHORT[m]}\nT = {d}", rotation=0, ha="right", va="center", color=INK, fontsize=9, labelpad=8)
        ax.text(1.005, 0.5, f"max {int(np.nanmax(grid))}", transform=ax.transAxes, fontsize=8,
                color=MUTED, va="center")
    axes[-1].set_xlim(0, longest)
    axes[-1].set_xlabel("Seconds since the run started (each run has its own start timestamp)")
    axes[0].set_title("Queue size per server across all runs — largest value in each second")
    subtitle(axes[0], f"Rows inside a run = servers A–E.  Proxy overload limit is {OVERLOAD_LIMIT}; "
                      f"the highest queue reached in any run is {vmax}.")
    cbar = fig.colorbar(mesh, ax=axes, location="right", fraction=0.015, pad=0.07)
    cbar.set_label("Queue size", color=MUTED)
    cbar.outline.set_visible(False)
    cbar.ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    save(fig, out, formats)


# --------------------------------------------------------------------------- #
# Summary table
# --------------------------------------------------------------------------- #
def write_summary(clients: dict, queues: dict, out: Path):
    rows = []
    for (m, d), df in clients.items():
        t = df["turnaround"]
        served_by = df["server"].str.strip()
        known = served_by.str.isdigit()
        row = {"variant": m, "T_ms": d, "unit": UNIT, "queries": len(df),
               "turnaround_mean": round(t.mean(), 2), "turnaround_median": round(t.median(), 2),
               "turnaround_p95": round(t.quantile(0.95), 2), "turnaround_max": round(t.max(), 2),
               "execution_mean": round(df["execution"].mean(), 2),
               "waiting_mean": round(df["waiting"].mean(), 2),
               "rest_mean": round(df["rest"].mean(), 2),
               "client_cache_hits_marked": int(df["client_hit"].sum()),
               # only countable when the output names the server by number ("processed by Server 3")
               "redirected_to_other_zone": (int((df["zone"].astype(str)[known] != served_by[known]).sum())
                                            if known.all() else "unknown")}
        for s, q in queues.get((m, d), {}).items():
            row[f"queue_peak_{s}"] = int(q["size"].max())
        rows.append(row)
    pd.DataFrame(rows).to_csv(out, index=False)


# --------------------------------------------------------------------------- #
def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    repo = Path(__file__).resolve().parent.parent
    ap.add_argument("output_dir", nargs="?", default=repo / "output", type=Path,
                    help="folder with the run folders (default: <repo>/output)")
    ap.add_argument("--graph-dir", default=repo / "graph", type=Path,
                    help="where the graphs go (default: <repo>/graph)")
    ap.add_argument("--format", default="png",
                    help="comma-separated, e.g. png,pdf (pdf/svg stay sharp in the report)")
    ap.add_argument("--window", type=int, default=50, help="rolling window in queries (default 50)")
    args = ap.parse_args()
    formats = [f.strip().lower() for f in args.format.split(",") if f.strip()]

    style()
    graph_dir = args.graph_dir
    graph_dir.mkdir(parents=True, exist_ok=True)

    # 1. read every run that exists
    clients, queues = {}, {}
    for mode in MODES:
        for delay in DELAYS:
            run = args.output_dir / f"{mode}{delay}"
            result_file = run / f"{mode}.txt"
            if not result_file.exists():
                print(f"skip {run.name}: {result_file.name} not found")
                continue
            clients[(mode, delay)] = parse_client_output(result_file)
            logs = {s: parse_queue_log(run / f"server{s}-queue.log")
                    for s in SERVERS if (run / f"server{s}-queue.log").exists()}
            if logs:
                queues[(mode, delay)] = logs
            print(f"{run.name}: {len(clients[(mode, delay)])} queries, "
                  f"{sum(map(len, logs.values()))} queue events")
    if not clients:
        print(f"No runs found in {args.output_dir}")
        return

    # 2. one time unit for all figures: seconds if the slowest query took more than 10 s
    global UNIT, UNIT_DIV
    if max(df["turnaround"].max() for df in clients.values()) > 10_000:
        UNIT, UNIT_DIV = "s", 1000
        for df in clients.values():
            for col in ["turnaround", "execution", "waiting", "rest"]:
                df[col] = df[col] / UNIT_DIV

    # 3. per-run figures
    for (mode, delay), df in clients.items():
        plot_turnaround(df, mode, delay, args.window, graph_dir / f"{mode}{delay}_turnaround", formats)
        if (mode, delay) in queues:
            plot_queues(queues[(mode, delay)], mode, delay, graph_dir / f"{mode}{delay}_queues", formats)

    # 4. comparison figures + table
    plot_turnaround_comparison(clients, args.window, graph_dir / "compare_turnaround", formats)
    plot_distribution(clients, graph_dir / "compare_distribution", formats)
    plot_average_breakdown(clients, graph_dir / "compare_averages", formats)
    plot_by_method(clients, graph_dir / "compare_by_method", formats)
    write_summary(clients, queues, graph_dir / "summary.csv")
    if queues:
        plot_queue_heatmap(queues, graph_dir / "compare_queues", formats)
    print(f"Graphs written to {graph_dir}")


if __name__ == "__main__":
    main()

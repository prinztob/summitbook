import bisect
from typing import Any, List, Sequence, Tuple

from gpxpy.gpx import GPXTrackPoint

from Extension import Extension

KM_M = 1000.0

# Best-effort window lengths in km shown in the times tab
BEST_EFFORT_WINDOWS_KM = (1, 2, 5, 10)

# Best-effort time window lengths in minutes shown in the times tab
BEST_EFFORT_WINDOWS_MIN = (1, 10, 60)

# Schema version of the interval data. Bumping it makes the Kotlin side
# treat existing gpxpy.json files as outdated and recompute them once.
INTERVAL_DATA_VERSION = 2

# Hysteresis for ascent/descent detection: a section starts at a sustained
# grade of at least ASCENT_START_GRADE percent and ends once the road
# flattens out or tips over (ASCENT_END_GRADE), so gentle drags inside a
# climb do not split it. It only counts when at least MIN_ELEVATION_DELTA
# meters were gained (ascents) or lost (descents). Tuned on the
# Kleinhelfendorf fixture: 1.5% as the end grade detected only 109 m of the
# ~386 m gpxpy elevation gain, because every small dip closed the section.
ASCENT_START_GRADE = 3.0
ASCENT_END_GRADE = 0.0
MIN_ELEVATION_DELTA = 20.0

# Rolling distance window (meters) over which elevations are smoothed
# before grades are computed.
ELEVATION_SMOOTHING_WINDOW_M = 100.0


def elapsed_at_distance(
    distances: Sequence[float], elapsed: Sequence[float], distance: float
) -> float:
    """Elapsed time in seconds at an exact distance, interpolated linearly
    between the two surrounding track points."""
    if distance <= distances[0]:
        return elapsed[0]
    if distance >= distances[-1]:
        return elapsed[-1]
    i = bisect.bisect_right(distances, distance) - 1
    if distances[i] == distance:
        return elapsed[i]
    d0, d1 = distances[i], distances[i + 1]
    fraction = (distance - d0) / (d1 - d0)
    return elapsed[i] + (elapsed[i + 1] - elapsed[i]) * fraction


def compute_km_times(
    distances: Sequence[float], elapsed: Sequence[float]
) -> List[float]:
    """Seconds per each full 1-km interval of the track: the elapsed time is
    taken at every exact kilometer mark and the marks are differenced."""
    if len(distances) < 2:
        return []
    km_times: List[float] = []
    last_mark_elapsed = 0.0
    for km in range(1, int(distances[-1] // KM_M) + 1):
        mark = km * KM_M
        if mark > distances[-1]:
            break
        elapsed_at_mark = elapsed_at_distance(distances, elapsed, mark)
        km_times.append(round(elapsed_at_mark - last_mark_elapsed, 1))
        last_mark_elapsed = elapsed_at_mark
    return km_times


def compute_best_efforts(
    distances: Sequence[float],
    elapsed: Sequence[float],
    windows_km: Sequence[float] = BEST_EFFORT_WINDOWS_KM,
) -> dict[str, dict[str, float]]:
    """Fastest time over a rolling window of each length: the minimum of
    elapsed(d + window) - elapsed(d) over all window positions d. The minimum
    of that piecewise linear function lies at a track point, so it is enough
    to try windows starting and ending at track points."""
    if len(distances) < 2:
        return {}
    max_distance = distances[-1]
    best_efforts: dict[str, dict[str, float]] = {}
    for window_km in windows_km:
        window_m = window_km * KM_M
        if max_distance < window_m:
            continue
        best: Tuple[float, float, float] | None = None
        # windows starting exactly at a track point
        for i in range(len(distances)):
            window_end = distances[i] + window_m
            if window_end > max_distance:
                break
            duration = elapsed_at_distance(distances, elapsed, window_end) - elapsed[i]
            if best is None or duration < best[0]:
                best = (duration, distances[i], window_end)
        # windows ending exactly at a track point
        for j in range(len(distances) - 1, -1, -1):
            window_start = distances[j] - window_m
            if window_start < distances[0]:
                break
            duration = elapsed[j] - elapsed_at_distance(distances, elapsed, window_start)
            if best is None or duration < best[0]:
                best = (duration, window_start, distances[j])
        if best is not None:
            best_efforts[str(window_km)] = {
                "time": round(best[0], 1),
                "start_distance": round(best[1], 1),
                "end_distance": round(best[2], 1),
            }
    return best_efforts


def distance_at_time(
    elapsed: Sequence[float], distances: Sequence[float], time: float
) -> float:
    """Distance in meters at an exact elapsed time, interpolated linearly
    between the two surrounding track points."""
    if time <= elapsed[0]:
        return distances[0]
    if time >= elapsed[-1]:
        return distances[-1]
    i = bisect.bisect_right(elapsed, time) - 1
    if elapsed[i] == time:
        return distances[i]
    t0, t1 = elapsed[i], elapsed[i + 1]
    fraction = (time - t0) / (t1 - t0)
    return distances[i] + (distances[i + 1] - distances[i]) * fraction


def _prefix_sums(values: Sequence[float]) -> List[float]:
    """Running totals of the one-second samples: prefix[i] is the sum of
    values[0:i], so any window sum is a difference of two prefixes."""
    prefix = [0.0]
    for value in values:
        prefix.append(prefix[-1] + value)
    return prefix


def _best_time_window_sum(
    elapsed: Sequence[float], values: Sequence[float], window_s: float
) -> float | None:
    """Largest sum of the one-second samples within a rolling window of
    window_s seconds of elapsed time. Every track point's sample counts for
    exactly one second, like the per-second rows of the power analysis, so
    pauses between points contribute nothing while still diluting the
    average. The maximum of the window sum lies where a window boundary
    hits a track point, so windows starting and ending at track points are
    enough."""
    if len(elapsed) < 2 or elapsed[-1] - elapsed[0] < window_s:
        return None
    prefix = _prefix_sums(values)
    best: float | None = None
    # windows starting at a track point
    for i in range(len(elapsed)):
        window_end = elapsed[i] + window_s
        if window_end > elapsed[-1]:
            break
        j = bisect.bisect_left(elapsed, window_end)
        window_sum = prefix[j] - prefix[i]
        if best is None or window_sum > best:
            best = window_sum
    # windows ending at a track point
    for j in range(len(elapsed)):
        window_start = elapsed[j] - window_s
        if window_start < elapsed[0]:
            continue
        i = bisect.bisect_left(elapsed, window_start)
        window_sum = prefix[j] - prefix[i]
        if best is None or window_sum > best:
            best = window_sum
    return best


def _best_distance_window_average(
    distances: Sequence[float],
    elapsed: Sequence[float],
    values: Sequence[float],
    window_m: float,
) -> float | None:
    """Best average of the one-second samples over a rolling window of
    window_m meters, averaged over the elapsed time the window spans. Like
    the time windows, the maximum lies where a window boundary hits a track
    point."""
    if len(distances) < 2 or distances[-1] - distances[0] < window_m:
        return None
    prefix = _prefix_sums(values)
    best: float | None = None
    # windows starting at a track point
    for i in range(len(distances)):
        window_end = distances[i] + window_m
        if window_end > distances[-1]:
            break
        j = bisect.bisect_left(distances, window_end)
        window_sum = prefix[j] - prefix[i]
        duration = elapsed_at_distance(distances, elapsed, window_end) - elapsed[i]
        if duration > 0 and (best is None or window_sum / duration > best):
            best = window_sum / duration
    # windows ending at a track point
    for j in range(len(distances) - 1, -1, -1):
        window_start = distances[j] - window_m
        if window_start < distances[0]:
            break
        i = bisect.bisect_left(distances, window_start)
        window_sum = prefix[j] - prefix[i]
        duration = elapsed[j] - elapsed_at_distance(distances, elapsed, window_start)
        if duration > 0 and (best is None or window_sum / duration > best):
            best = window_sum / duration
    return best


def _best_distance_in_time_window(
    elapsed: Sequence[float], distances: Sequence[float], window_s: float
) -> float | None:
    """Farthest distance in meters covered within a rolling window of
    window_s seconds of elapsed time."""
    if len(elapsed) < 2 or elapsed[-1] - elapsed[0] < window_s:
        return None
    best: float | None = None
    # windows starting at a track point
    for i in range(len(elapsed)):
        window_end = elapsed[i] + window_s
        if window_end > elapsed[-1]:
            break
        covered = distance_at_time(elapsed, distances, window_end) - distances[i]
        if best is None or covered > best:
            best = covered
    # windows ending at a track point
    for j in range(len(elapsed) - 1, -1, -1):
        window_start = elapsed[j] - window_s
        if window_start < elapsed[0]:
            break
        covered = distances[j] - distance_at_time(elapsed, distances, window_start)
        if best is None or covered > best:
            best = covered
    return best


def compute_per_km_values(
    distances: Sequence[float],
    elapsed: Sequence[float],
    values: Sequence[float],
) -> List[float]:
    """Average of the one-second samples within each full 1-km interval,
    divided by the interval's elapsed time. Empty when no sample has a
    value, i.e. when the track has no data of this kind."""
    if len(distances) < 2 or not any(v > 0 for v in values):
        return []
    prefix = _prefix_sums(values)
    per_km: List[float] = []
    for km in range(1, int(distances[-1] // KM_M) + 1):
        mark = km * KM_M
        if mark > distances[-1]:
            break
        previous_mark = (km - 1) * KM_M
        i0 = bisect.bisect_left(distances, previous_mark)
        i1 = bisect.bisect_left(distances, mark)
        window_sum = prefix[i1] - prefix[i0]
        duration = elapsed_at_distance(distances, elapsed, mark) - elapsed_at_distance(
            distances, elapsed, previous_mark
        )
        per_km.append(window_sum / duration if duration > 0 else 0.0)
    return per_km


def _average_over_distance(
    distances: Sequence[float],
    values: Sequence[float],
    start_distance: float,
    end_distance: float,
    duration: float,
) -> float | None:
    """Average of the one-second samples within a distance range, divided by
    the range's duration; None when no sample has a value."""
    if not any(v > 0 for v in values) or duration <= 0:
        return None
    prefix = _prefix_sums(values)
    i0 = bisect.bisect_left(distances, start_distance)
    i1 = bisect.bisect_left(distances, end_distance)
    return (prefix[i1] - prefix[i0]) / duration


def _smooth_elevations(
    distances: Sequence[float], elevations: Sequence[float]
) -> List[float]:
    """Trailing rolling mean over at most ELEVATION_SMOOTHING_WINDOW_M meters,
    to keep GPS elevation noise from producing phantom grades."""
    prefix_sums = [0.0]
    for elevation in elevations:
        prefix_sums.append(prefix_sums[-1] + elevation)
    smoothed: List[float] = []
    left = 0
    for i in range(len(distances)):
        while distances[i] - distances[left] > ELEVATION_SMOOTHING_WINDOW_M:
            left += 1
        count = i - left + 1
        smoothed.append((prefix_sums[i + 1] - prefix_sums[left]) / count)
    return smoothed


def _append_section(
    sections: List[dict[str, float]],
    distances: Sequence[float],
    smoothed: Sequence[float],
    elapsed: Sequence[float],
    start_idx: int,
    end_idx: int,
    sign: float,
) -> None:
    elevation_delta = smoothed[end_idx] - smoothed[start_idx]
    if sign * elevation_delta >= MIN_ELEVATION_DELTA:
        sections.append(
            {
                "start_distance": round(distances[start_idx], 1),
                "end_distance": round(distances[end_idx], 1),
                "start_elevation": round(smoothed[start_idx], 1),
                "end_elevation": round(smoothed[end_idx], 1),
                "duration": round(elapsed[end_idx] - elapsed[start_idx], 1),
            }
        )


def detect_elevation_intervals(
    distances: Sequence[float],
    elevations: Sequence[float],
    elapsed: Sequence[float],
    ascending: bool = True,
) -> List[dict[str, float]]:
    """Sustained ascent (or descent) sections of the track. Walks the track
    with hysteresis over the smoothed grade: a section opens at a grade of at
    least ASCENT_START_GRADE percent, closes once the grade drops below
    ASCENT_END_GRADE, and is kept when it spans at least
    MIN_ELEVATION_DELTA meters of elevation."""
    if len(distances) < 2:
        return []
    smoothed = _smooth_elevations(distances, elevations)
    sign = 1.0 if ascending else -1.0
    sections: List[dict[str, float]] = []
    start_idx: int | None = None
    for i in range(1, len(distances)):
        delta_distance = distances[i] - distances[i - 1]
        if delta_distance <= 0:
            continue
        grade = (smoothed[i] - smoothed[i - 1]) / delta_distance * 100.0
        if start_idx is None:
            if sign * grade >= ASCENT_START_GRADE:
                start_idx = i - 1
        elif sign * grade <= ASCENT_END_GRADE:
            _append_section(sections, distances, smoothed, elapsed, start_idx, i, sign)
            start_idx = None
    if start_idx is not None:
        _append_section(
            sections, distances, smoothed, elapsed, start_idx, len(distances) - 1, sign
        )
    return sections


class IntervalAnalyzer:
    """Computes the per-activity interval data for the times tab: seconds per
    full 1-km interval, best efforts over rolling distance windows, and the
    sustained ascent and descent sections of the track."""

    def __init__(
        self, points_with_extension: List[Tuple[GPXTrackPoint, Extension]]
    ) -> None:
        points_with_time = [
            p
            for p in points_with_extension
            if p[0].time and p[1].distance is not None
        ]
        self.distances = [float(p[1].distance) for p in points_with_time]
        start_time = points_with_time[0][0].time if points_with_time else None
        self.elapsed = [
            (p[0].time - start_time).total_seconds() for p in points_with_time
        ]
        self.power = [float(p[1].power) for p in points_with_time]
        self.hr = [float(p[1].hr) for p in points_with_time]
        points_with_time_and_elevation = [
            p for p in points_with_time if p[0].elevation is not None
        ]
        self.elevation_distances = [
            float(p[1].distance) for p in points_with_time_and_elevation
        ]
        self.elevations = [float(p[0].elevation) for p in points_with_time_and_elevation]
        elevation_start_time = (
            points_with_time_and_elevation[0][0].time
            if points_with_time_and_elevation
            else None
        )
        self.elevation_elapsed = [
            (p[0].time - elevation_start_time).total_seconds()
            for p in points_with_time_and_elevation
        ]

    def analyze(self) -> dict[str, Any]:
        data: dict[str, Any] = {}
        try:
            data["km_times"] = compute_km_times(self.distances, self.elapsed)
        except Exception as err:  # pylint: disable=broad-except
            print(f"km_times computation failed with {err}")
        try:
            data["best_efforts_km"] = compute_best_efforts(self.distances, self.elapsed)
        except Exception as err:  # pylint: disable=broad-except
            print(f"best efforts computation failed with {err}")
        try:
            data["best_distance_min"] = {}
            for window_min in BEST_EFFORT_WINDOWS_MIN:
                covered = _best_distance_in_time_window(
                    self.elapsed, self.distances, window_min * 60.0
                )
                if covered is not None:
                    data["best_distance_min"][str(window_min)] = round(covered, 1)
        except Exception as err:  # pylint: disable=broad-except
            print(f"distance windows computation failed with {err}")
        try:
            data["best_power_min"] = {}
            data["best_power_km"] = {}
            if any(p > 0 for p in self.power):
                for window_min in BEST_EFFORT_WINDOWS_MIN:
                    best_sum = _best_time_window_sum(
                        self.elapsed, self.power, window_min * 60.0
                    )
                    if best_sum is not None:
                        data["best_power_min"][str(window_min)] = round(
                            best_sum / (window_min * 60.0)
                        )
                for window_km in BEST_EFFORT_WINDOWS_KM:
                    best_avg = _best_distance_window_average(
                        self.distances, self.elapsed, self.power, window_km * KM_M
                    )
                    if best_avg is not None:
                        data["best_power_km"][str(window_km)] = round(best_avg)
        except Exception as err:  # pylint: disable=broad-except
            print(f"power windows computation failed with {err}")
        try:
            data["best_hr_min"] = {}
            data["best_hr_km"] = {}
            if any(v > 0 for v in self.hr):
                for window_min in BEST_EFFORT_WINDOWS_MIN:
                    best_sum = _best_time_window_sum(
                        self.elapsed, self.hr, window_min * 60.0
                    )
                    if best_sum is not None:
                        data["best_hr_min"][str(window_min)] = round(
                            best_sum / (window_min * 60.0)
                        )
                for window_km in BEST_EFFORT_WINDOWS_KM:
                    best_avg = _best_distance_window_average(
                        self.distances, self.elapsed, self.hr, window_km * KM_M
                    )
                    if best_avg is not None:
                        data["best_hr_km"][str(window_km)] = round(best_avg)
        except Exception as err:  # pylint: disable=broad-except
            print(f"heart rate windows computation failed with {err}")
        try:
            data["power_per_km"] = [
                round(v)
                for v in compute_per_km_values(self.distances, self.elapsed, self.power)
            ]
        except Exception as err:  # pylint: disable=broad-except
            print(f"per-km power computation failed with {err}")
        try:
            data["hr_per_km"] = [
                round(v)
                for v in compute_per_km_values(self.distances, self.elapsed, self.hr)
            ]
        except Exception as err:  # pylint: disable=broad-except
            print(f"per-km heart rate computation failed with {err}")
        try:
            data["ascents"] = detect_elevation_intervals(
                self.elevation_distances, self.elevations, self.elevation_elapsed, True
            )
        except Exception as err:  # pylint: disable=broad-except
            print(f"ascent detection failed with {err}")
        try:
            data["descents"] = detect_elevation_intervals(
                self.elevation_distances, self.elevations, self.elevation_elapsed, False
            )
        except Exception as err:  # pylint: disable=broad-except
            print(f"descent detection failed with {err}")
        try:
            # Average power and heart rate of every ascent/descent section
            for sections_key in ("ascents", "descents"):
                for section in data.get(sections_key, []):
                    power_avg = _average_over_distance(
                        self.distances, self.power,
                        section["start_distance"], section["end_distance"],
                        section["duration"],
                    )
                    section["power"] = round(power_avg) if power_avg is not None else None
                    hr_avg = _average_over_distance(
                        self.distances, self.hr,
                        section["start_distance"], section["end_distance"],
                        section["duration"],
                    )
                    section["hr"] = round(hr_avg) if hr_avg is not None else None
        except Exception as err:  # pylint: disable=broad-except
            print(f"section power/heart rate computation failed with {err}")
        data["interval_data_version"] = INTERVAL_DATA_VERSION
        return data

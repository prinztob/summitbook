"""Tests for the interval analysis (km splits, best efforts, ascent/descent
detection) against a real GPX fixture.

Chaquopy cannot run inside the JVM unit tests, so this test runs with the
same interpreter that builds the embedded Python environment:

    /home/prinzt/.pyenv/shims/python app/src/test/python/test_interval_analyzer.py

It can also be run with pytest. The GPX fixture is the same file the Kotlin
track-similarity tests use, so both sides operate on identical data.
"""

import sys
import unittest
from datetime import datetime, timedelta
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(PROJECT_ROOT / "app" / "src" / "main" / "python"))

from Extension import Extension  # noqa: E402
from gpxpy.gpx import GPXTrackPoint  # noqa: E402
from interval_analyzer import (  # noqa: E402
    ASCENT_END_GRADE,
    ASCENT_START_GRADE,
    MIN_ELEVATION_DELTA,
    IntervalAnalyzer,
)
from utils import haversine_distances_cumsum_m, parse_track  # noqa: E402

FIXTURE = (
    PROJECT_ROOT / "app" / "src" / "test" / "resources"
    / "2026-10-02_Kleinhelfendorf.gpx"
)


class IntervalAnalyzerTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        gpx = parse_track(FIXTURE)
        points = [
            p for t in gpx.tracks for s in t.segments for p in s.points
            if p.longitude != 0 and p.latitude != 0
        ]
        distances = haversine_distances_cumsum_m(
            [p.latitude for p in points], [p.longitude for p in points]
        )
        cls.points_with_extension = [
            (p, Extension(distance=float(d))) for p, d in zip(points, distances)
        ]
        cls.analyzer = IntervalAnalyzer(cls.points_with_extension)
        cls.data = cls.analyzer.analyze()

    def test_km_times_match_track_duration(self):
        km_times = self.data["km_times"]
        total_distance_m = self.analyzer.distances[-1]
        self.assertEqual(int(total_distance_m // 1000), len(km_times))
        # every split is a positive number of seconds
        self.assertTrue(all(t > 0 for t in km_times))
        # the splits together must not exceed the total elapsed time
        self.assertLessEqual(sum(km_times), self.analyzer.elapsed[-1] + 1)

    def test_best_efforts_are_monotonic_over_window_length(self):
        best_efforts = self.data["best_efforts_km"]
        self.assertTrue(set(best_efforts.keys()).issubset({"1", "2", "5", "10"}))
        times = [best_efforts[key]["time"] for key in sorted(
            best_efforts, key=lambda k: int(k)
        )]
        windows = [int(key) for key in sorted(best_efforts, key=lambda k: int(k))]
        # a longer window can never be faster than a shorter one
        for i in range(1, len(times)):
            self.assertGreaterEqual(times[i], times[i - 1])
        # each window spans exactly its length
        for window_km, key in zip(windows, sorted(best_efforts, key=lambda k: int(k))):
            effort = best_efforts[key]
            self.assertAlmostEqual(
                effort["end_distance"] - effort["start_distance"],
                window_km * 1000.0,
                delta=1.0,
            )
            self.assertAlmostEqual(effort["time"], times[windows.index(window_km)])

    def test_ascents_and_descents_are_plausible_sections(self):
        for key, sign in (("ascents", 1.0), ("descents", -1.0)):
            sections = self.data[key]
            for section in sections:
                self.assertGreaterEqual(
                    sign * (section["end_elevation"] - section["start_elevation"]),
                    MIN_ELEVATION_DELTA,
                    f"{key[:-1]} gains less than the minimum: {section}",
                )
                self.assertGreater(section["end_distance"], section["start_distance"])
                self.assertGreater(section["duration"], 0)
                # the section is steep enough on average to have opened the
                # hysteresis at ASCENT_START_GRADE
                length_m = section["end_distance"] - section["start_distance"]
                grade_percent = (
                    sign
                    * (section["end_elevation"] - section["start_elevation"])
                    / length_m
                    * 100.0
                )
                self.assertGreaterEqual(
                    grade_percent, ASCENT_END_GRADE,
                    f"section flatter than the end grade: {section}",
                )
            # sections are sorted by distance and do not overlap
            for earlier, later in zip(sections, sections[1:]):
                self.assertLessEqual(earlier["end_distance"], later["start_distance"])
            print(f"\n{key} ({len(sections)} sections):")
            for section in sections:
                length_m = section["end_distance"] - section["start_distance"]
                delta_m = section["end_elevation"] - section["start_elevation"]
                grade_percent = abs(delta_m) / length_m * 100.0
                print(
                    f"  km {section['start_distance'] / 1000:.2f}"
                    f"-{section['end_distance'] / 1000:.2f}: "
                    f"{delta_m:+.0f} m, "
                    f"{length_m:.0f} m at {grade_percent:.1f}%, "
                    f"{section['duration']:.0f} s"
                )

    def test_elevation_sections_cover_the_climbs_of_the_track(self):
        """The detected sections must cover a reasonable share of the track's
        total elevation gain: gpxpy also counts gentle rolling that a
        sustained-climb detector deliberately skips, so not everything can be
        covered - but losing most of the climbing means the hysteresis
        closes sections too early again."""
        gpx = parse_track(FIXTURE)
        gpx.smooth()
        uphill_downhill = gpx.get_uphill_downhill()
        ascents_total = sum(
            s["end_elevation"] - s["start_elevation"] for s in self.data["ascents"]
        )
        descents_total = sum(
            s["end_elevation"] - s["start_elevation"] for s in self.data["descents"]
        )
        print(f"\ngpxpy uphill {uphill_downhill.uphill:.0f} m, "
              f"detected ascents {ascents_total:.0f} m, "
              f"gpxpy downhill {uphill_downhill.downhill:.0f} m, "
              f"detected descents {descents_total:.0f} m")
        # gpxpy's numbers include all the micro-rolling a sustained-climb
        # detector skips; the tuned thresholds reach ~45% on this fixture,
        # a collapse below 35% means sections got too small again
        self.assertGreaterEqual(
            ascents_total, 0.35 * uphill_downhill.uphill,
            "ascents cover too little of the track's elevation gain",
        )
        self.assertLessEqual(ascents_total, uphill_downhill.uphill + 50)
        self.assertLessEqual(
            descents_total, 0.0,
        )
        self.assertGreaterEqual(
            descents_total, -uphill_downhill.downhill - 50,
            "descents lose more elevation than the track has",
        )

    def test_power_hr_and_distance_windows_on_synthetic_track(self):
        """A synthetic 30-minute track at constant 10 m/s: 20 minutes at
        200 W / 150 bpm, then 10 minutes at 400 W / 170 bpm. The best
        windows must pick the hard block where it fits and blend where it
        does not."""
        start = datetime(2026, 1, 1, 12, 0, 0)
        points = []
        for second in range(1800):
            in_block = second >= 1200
            points.append(
                (
                    GPXTrackPoint(
                        latitude=48.0,
                        longitude=11.0,
                        elevation=(
                            # 3% grade at 10 m/s during the hard block, so it
                            # is detected as one ascent section
                            500.0 + 0.3 * (second - 1200) if in_block else None
                        ),
                        time=start + timedelta(seconds=second),
                    ),
                    Extension(
                        distance=second * 10.0,
                        power=400 if in_block else 200,
                        hr=170 if in_block else 150,
                    ),
                )
            )
        data = IntervalAnalyzer(points).analyze()

        # 1- and 10-minute windows fit inside the 10-minute block
        self.assertEqual(data["best_power_min"], {"1": 400, "10": 400})
        self.assertEqual(data["best_hr_min"], {"1": 170, "10": 170})
        # the ride is shorter than 60 minutes, so no 60-minute window exists
        self.assertNotIn("60", data["best_power_min"])
        self.assertNotIn("60", data["best_hr_min"])
        # the 400 W block spans 6 km: the 1/2/5-km windows fit inside it,
        # a 10-km window must blend 400 s of the easy block with 600 s of
        # the hard block: (200*400 + 400*600) / 1000 = 320 W, and for the
        # heart rate (150*400 + 170*600) / 1000 = 162 bpm
        self.assertEqual(
            data["best_power_km"], {"1": 400, "2": 400, "5": 400, "10": 320}
        )
        self.assertEqual(data["best_hr_km"], {"1": 170, "2": 170, "5": 170, "10": 162})
        # per-km values: the track is 17.99 km long, so 17 full km - 12 in
        # the easy block, 5 in the hard block
        self.assertEqual(data["power_per_km"], [200] * 12 + [400] * 5)
        self.assertEqual(data["hr_per_km"], [150] * 12 + [170] * 5)
        # 1- and 10-minute distance windows at 10 m/s
        self.assertAlmostEqual(data["best_distance_min"]["1"], 600.0, delta=1.0)
        self.assertAlmostEqual(data["best_distance_min"]["10"], 6000.0, delta=1.0)
        self.assertNotIn("60", data["best_distance_min"])
        # the 3% block is one ascent section averaging 400 W / 170 bpm
        self.assertEqual(len(data["ascents"]), 1)
        self.assertEqual(data["ascents"][0]["power"], 400)
        self.assertEqual(data["ascents"][0]["hr"], 170)
        self.assertEqual(data["descents"], [])
        # the schema version marks the file as analyzed with this version
        self.assertEqual(data["interval_data_version"], 2)

    def test_tracks_without_power_or_hr_get_empty_values(self):
        """The fixture has no power or heart rate data, so those keys must
        be present but empty - presence is what marks the data as analyzed
        with this version."""
        self.assertEqual(self.data["best_power_min"], {})
        self.assertEqual(self.data["best_power_km"], {})
        self.assertEqual(self.data["best_hr_min"], {})
        self.assertEqual(self.data["best_hr_km"], {})
        self.assertEqual(self.data["power_per_km"], [])
        self.assertEqual(self.data["hr_per_km"], [])
        # distance windows exist on any track with time and distance data
        self.assertTrue(
            set(self.data["best_distance_min"]).issubset({"1", "10", "60"})
        )
        # the fixture has no power or heart rate, so the sections carry
        # null values, and the schema version is present
        for section in self.data["ascents"] + self.data["descents"]:
            self.assertIsNone(section["power"])
            self.assertIsNone(section["hr"])
        self.assertEqual(self.data["interval_data_version"], 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)

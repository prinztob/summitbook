from datetime import datetime
from xml.etree import ElementTree

import dateutil
from dateutil.parser import ParserError
from gpxpy import gpx
from tcxparser import TCXParser


def convert_tcx_to_gpx(in_file_path, out_file_path=None):
    tcx = TCXParser(str(in_file_path))
    number_track_points = len(tcx.position_values())
    has_power = len(tcx.power_values()) >= number_track_points
    has_cadence = len(tcx.cadence_values()) >= number_track_points
    has_hr = len(tcx.hr_values()) >= number_track_points
    has_distance = len(tcx.distance_values()) >= number_track_points
    track_points = zip(tcx.position_values(), tcx.altitude_points(), tcx.time_values(),
                       tcx.hr_values() if has_hr else [-1] * number_track_points,
                       tcx.cadence_values() if has_cadence else [-1] * number_track_points,
                       tcx.power_values() if has_power else [-1] * number_track_points,
                       tcx.distance_values() if has_distance else [-1] * number_track_points
                       )
    print(f"Extracting track points from tcx file {in_file_path}")
    gpx_from_tcx = gpx.GPX()
    gpx_from_tcx.nsmap["gpxtpx"] = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
    try:
        gpx_from_tcx.name = dateutil.parser.parse(tcx.started_at).isoformat()
    except ParserError as pe:
        raise ParserError(f"The start date/time in TCX file {in_file_path} is not in ISO format.") from pe
    gpx_from_tcx.description = ""
    gpx_track = gpx.GPXTrack(
        name=dateutil.parser.parse(tcx.started_at).isoformat(),
        description="",
    )
    gpx_track.type = tcx.activity_type
    gpx_from_tcx.tracks.append(gpx_track)
    gpx_segment = gpx.GPXTrackSegment()
    gpx_track.segments.append(gpx_segment)
    for point in track_points:
        try:
            time = datetime.fromisoformat(point[2])
        except ValueError:
            time = datetime.strptime(point[2], "%Y-%m-%dT%H:%M:%S.%fZ")
        gpx_track_point = gpx.GPXTrackPoint(
            latitude=point[0][0],
            longitude=point[0][1],
            elevation=point[1],
            time=time,
        )

        if has_hr or has_cadence or has_power:
            gpx_track_point.extensions.append(
                ElementTree.fromstring(f"""<gpxtpx:TrackPointExtension xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
                    {f"<gpxtpx:hr>{point[3]}</gpxtpx:hr>" if has_hr else ""}
                    {f"<gpxtpx:cadence>{point[4]}</gpxtpx:cadence>" if has_cadence else ""}
                    {f"<gpxtpx:power>{point[5]}</gpxtpx:power>" if has_power else ""}
                    {f"<gpxtpx:distance>{point[6]}</gpxtpx:distance>" if has_distance else ""}
                    </gpxtpx:TrackPointExtension>
                """)
            )
        gpx_segment.points.append(gpx_track_point)
    if not out_file_path:
        out_file_path = in_file_path.replace(".tcx", ".gpx")
    with open(out_file_path, "w", encoding="utf8") as output:
        output.write(gpx_from_tcx.to_xml())
    print(f"Successfully converted tcx file to gpx and written to {out_file_path}")

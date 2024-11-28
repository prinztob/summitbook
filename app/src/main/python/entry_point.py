import json
import os
from datetime import date, datetime

import garth
import lxml.etree as mod_etree
import requests
from garminconnect import (
    Garmin,
    GarminConnectConnectionError,
    GarminConnectTooManyRequestsError,
    GarminConnectAuthenticationError,
)
from garth.exc import GarthHTTPError

from tcx_to_gpx import convert_tcx_to_gpx
from Extension import Extension
from gpx_track_analyzer import TrackAnalyzer

BASE_URL = 'https://connect.garmin.com'


def init_api(user_name, password, output_file):
    """Initialize Garmin API with your credentials."""
    garth.http.USER_AGENT = {"User-Agent": "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.81 Mobile Safari/537.36", }
    token_store = output_file + "/.garminconnect"
    try:
        print(
            f"Trying to login to Garmin Connect using token data from '{token_store}'...\n"
        )
        garmin = Garmin()
        garmin.login(token_store)
        return garmin
    except (FileNotFoundError, GarthHTTPError, GarminConnectAuthenticationError):
        # Session is expired. You'll need to log in again
        print(
            "Login tokens not present, login with your Garmin Connect credentials to generate them.\n"
            f"They will be stored in '{token_store}' for future use.\n"
        )
        try:
            garmin = Garmin(user_name, password)
            garmin.login()
            garmin.garth.dump(token_store)
            return garmin
        except (FileNotFoundError, GarthHTTPError, GarminConnectAuthenticationError,
                requests.exceptions.HTTPError) \
                as err:
            return f"return code: 1Error occurred during Garmin Connect Client init: {err}"
    except Exception as err:
        return f"return code: 1Unknown error occurred during Garmin Connect Client init {err}"


def get_activities_by_date(api, start_date, end_date, activity_type):
    """Return available activities."""
    url = api.garmin_connect_activities
    activities = []
    start = 0
    limit = 20
    return_data = True

    while return_data:
        params = {
            "startDate": str(start_date),
            "endDate": str(end_date),
            "start": str(start),
            "limit": str(limit)
        }
        if activity_type:
            params["activityType"] = str(activity_type)
        additional_activities = api.connectapi(url, params=params)
        if additional_activities:
            activities.extend(additional_activities)
            start = start + limit
        else:
            return_data = False
    return activities


def get_exercise_sets(api, activity_id):
    activity_id = str(activity_id)
    return api.connectapi(f"{api.garmin_connect_activity}/{activity_id}")


def get_daily_events(api, selected_date):
    try:
        url = f"/wellness-service/wellness/dailyEvents/{api.display_name}"
        params = {
            "calendarDate": str(selected_date),
        }
        print(f"Fetching daily events with url {url} for date {selected_date}")
        return api.connectapi(url, params=params)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return (
            f"return code: 1: Error occurred during Garmin Connect Client get daily events for date "
            f"{selected_date}: {err}")
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client get daily events for date "
            f"{selected_date}: {err}")


def get_user_summary(api, selected_date):
    try:
        return api.get_user_summary(selected_date)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return (
            f"return code: 1: Error occurred during Garmin Connect Client get user summary for date "
            f"{selected_date}: {err}")
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client get user summary for date "
            f"{selected_date}: {err}")


def get_hrv(api, selected_date):
    try:
        print(f"Requesting daily hrv data for date {selected_date}")
        return api.get_hrv_data(selected_date)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return (
            f"return code: 1: Error occurred during Garmin Connect Client get daily hrv for for "
            f"{selected_date}: {err}")
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client get daily hrv for date "
            f"{selected_date}: {err}")


def get_activity_json_for_date(client, selected_date):
    try:
        activities = get_activities_by_date(client, selected_date, selected_date, None)
        for activity in activities:
            activity["vo2MaxPreciseValue"] = get_precise_vo2max(client, selected_date)
        return json.dumps(activities)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return (
            f"return code: 1Error occurred during Garmin Connect Client get activity json for date "
            f"{selected_date}: {err}")
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client get activity json for date "
            f"{selected_date}: "
            f"{err}")


def download_tcx(api, activity_id, output_file_tcx, output_file_gpx):
    try:
        gpx_data = api.download_activity(activity_id, dl_fmt=Garmin.ActivityDownloadFormat.TCX)
        with open(output_file_tcx, "wb") as fb:
            fb.write(gpx_data)
        convert_tcx_to_gpx(output_file_tcx, output_file_gpx)
        return "return code: 0"
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return f"return code: 1Error occurred during Garmin Connect Client download tcx for id {activity_id}: {err}"
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client download tcx for id {activity_id}: "
            f"{err}")


def download_gpx(api, activity_id, output_file):
    try:
        gpx_data = api.download_activity(activity_id, dl_fmt=Garmin.ActivityDownloadFormat.GPX)
        with open(output_file, "wb") as fb:
            fb.write(gpx_data)
        return "return code: 0"
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return f"return code: 1Error occurred during Garmin Connect Client download gpx for id {activity_id}: {err}"
    except Exception as err:
        return (
            f"return code: 1Unknown error occurred during Garmin Connect Client download gpx for id {activity_id}: "
            f"{err}")


def get_exercise_set(api, activity_id, folder):
    try:
        sets = get_exercise_sets(api, activity_id)
        output_file = f"{folder}/activity_{str(activity_id)}_exercise_set.json"
        if not os.path.exists(output_file):
            with open(output_file, "w+") as fb:
                json.dump(sets, fb)
        return sets
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return f"return code: 1Error occurred during Garmin Connect Client get multi sport data: {err}"
    except Exception as err:
        return f"return code: 1Unknown error occurred during Garmin Connect Client get multi sport data {err}"


def get_split_data(api, activity_id, folder):
    try:
        return download_splits(api, activity_id, folder)
    except Exception as err:
        return f"return code: 1Unknown error occurred during Garmin Connect Client get split data {err}"


def get_power_data(api, selected_date):
    """
    Get power data
    """
    try:
        url = f"/fitnessstats-service/powerCurve/?startDate={selected_date}&endDate={selected_date}"
        print(f"Fetching power data with url {url}")
        return api.connectapi(url)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return f"return code: 1Error occurred during Garmin Connect Client get power data: {err}"
    except Exception as err:  # pylint: disable=broad-except
        return f"return code: 1Unknown error occurred during Garmin Connect Client get power data {err}"


def download_activities_by_date(api, folder, start_date, end_date=date.today()):
    try:
        print(f"Download activities between {start_date} and {end_date}.")
        activities = get_activities_by_date(api, start_date, end_date, None)
        write_index = 0
        print(f"Downloading {len(activities)} activities.")
        for activity in activities:
            activity_id = activity["activityId"]
            if activity["activityType"]["typeId"] == 89:
                multi_sport_data = get_exercise_sets(api, activity_id)
                child_ids = multi_sport_data["metadataDTO"]["childIds"]
                activity["childIds"] = child_ids
                for child_id in child_ids:
                    details = get_exercise_sets(api, child_id)
                    output_file = f"{folder}/child_{str(child_id)}.json"
                    if not os.path.exists(output_file):
                        with open(output_file, "w+") as fb:
                            json.dump(details, fb)
                start_time_local = activity["startTimeLocal"].split(" ")
                if start_time_local and len(start_time_local) == 2:
                    update_power_data(activity, api, start_time_local[0])
            else:
                activity["childIds"] = []
            start_time_local = activity["startTimeLocal"].split(" ")
            if start_time_local and len(start_time_local) == 2:
                activity["vo2MaxPreciseValue"] = get_precise_vo2max(api, start_time_local[0])
            output_file = f"{folder}/activity_{str(activity_id)}.json"
            with open(output_file, "w+") as fb:
                json.dump(activity, fb)
                write_index += 1
            download_splits(api, activity_id, folder)
            get_exercise_set(api, activity_id, folder)
        return "return code: 0\nDownloaded {} activities, wrote {} to file".format(len(activities),
                                                                                   write_index)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return f"return code: 1Error occurred during Garmin Connect Client download activities by date: {err}"
    except Exception as err:  # pylint: disable=broad-except
        return f"return code: 1Unknown error occurred during Garmin Connect Client download activities by date {err}"


def get_precise_vo2max(api, selected_date):
    data = api.get_max_metrics(selected_date)
    if len(data) > 0:
        if data[0]["generic"] and "vo2MaxPreciseValue" in data[0]["generic"]:
            vo2MaxPreciseValue = data[0]["generic"]['vo2MaxPreciseValue']
            print(f"Found vo2MaxPreciseValue {vo2MaxPreciseValue}.")
            return vo2MaxPreciseValue
        elif data[0]["cycling"] and "vo2MaxPreciseValue" in data[0]["cycling"]:
            vo2MaxPreciseValue = data[0]["cycling"]['vo2MaxPreciseValue']
            print(f"Found vo2MaxPreciseValue {vo2MaxPreciseValue}.")
            return vo2MaxPreciseValue
    return 0.0


def download_splits(api, activity_id, folder):
    splits = api.get_activity_splits(activity_id)
    output_file = f"{folder}/activity_{str(activity_id)}_splits.json"
    if not os.path.exists(output_file):
        with open(output_file, "w+") as fb:
            json.dump(splits, fb)
    return splits


def update_power_data(activity, api, selected_date):
    power_data = get_power_data(api, selected_date)
    if "entries" in power_data and len(power_data["entries"]) == 15:
        activity['maxAvgPower_1'] = power_data['entries'][0]['power']
        activity['maxAvgPower_2'] = power_data['entries'][1]['power']
        activity['maxAvgPower_5'] = power_data['entries'][2]['power']
        activity['maxAvgPower_10'] = power_data['entries'][3]['power']
        activity['maxAvgPower_20'] = power_data['entries'][4]['power']
        activity['maxAvgPower_30'] = power_data['entries'][5]['power']
        activity['maxAvgPower_60'] = power_data['entries'][6]['power']
        activity['maxAvgPower_120'] = power_data['entries'][7]['power']
        activity['maxAvgPower_300'] = power_data['entries'][8]['power']
        activity['maxAvgPower_600'] = power_data['entries'][9]['power']
        activity['maxAvgPower_1200'] = power_data['entries'][10]['power']
        activity['maxAvgPower_1800'] = power_data['entries'][11]['power']
        activity['maxAvgPower_3600'] = power_data['entries'][12]['power']
        activity['maxAvgPower_7200'] = power_data['entries'][13]['power']
        activity['maxAvgPower_18000'] = power_data['entries'][14]['power']


def analyze_gpx_track(gpx_path, additional_data_folder):
    try:
        start_time = datetime.now()
        analyzer = TrackAnalyzer(gpx_path, additional_data_folder)
        analyzer.analyze()
        analyzer.write_data_and_extension_to_file()
        analyzer.write_simplified_track_to_file()
        print(f"Analyzing of {gpx_path} took {(datetime.now() - start_time).total_seconds()}")
        return "return code: 0"
    except Exception as err:  # pylint: disable=broad-except
        return f"return code: 1Unknown error occurred {err}"


def simplify_gpx_track(gpx_path, additional_data_folder):
    try:
        start_time = datetime.now()
        analyzer = TrackAnalyzer(gpx_path, additional_data_folder)
        analyzer.write_simplified_track_to_file()
        print(f"Simplifying of {gpx_path} took {(datetime.now() - start_time).total_seconds()}")
        return "return code: 0"
    except Exception as err:  # pylint: disable=broad-except
        return f"return code: 1Unknown error occurred {err}"


def merge_tracks(gpx_track_files_to_merge, output_file, name):
    try:
        print(f"Trying to merge the following tracks: {gpx_track_files_to_merge}")
        files = list(gpx_track_files_to_merge)
        analyzer_for_all_tracks = None
        for file in files:
            analyzer_for_single_track = TrackAnalyzer(file)
            analyzer_for_single_track.parse_track()
            if analyzer_for_all_tracks is None:
                analyzer_for_all_tracks = analyzer_for_single_track
            else:
                time1 = get_time(analyzer_for_all_tracks.gpx)
                time2 = get_time(analyzer_for_single_track.gpx)
                if time1 is None or time2 is None or time1 < time2:
                    update_distance(analyzer_for_all_tracks.gpx, analyzer_for_single_track.gpx)
                    analyzer_for_all_tracks.gpx.tracks.extend(analyzer_for_single_track.gpx.tracks)
                else:
                    update_distance(analyzer_for_single_track.gpx, analyzer_for_all_tracks.gpx)
                    analyzer_for_single_track.gpx.tracks.extend(analyzer_for_all_tracks.gpx.tracks)
                    analyzer_for_all_tracks = analyzer_for_single_track
        analyzer_for_all_tracks.gpx.name = name
        with open(output_file, "w") as f:
            f.write(analyzer_for_all_tracks.gpx.to_xml())
        print(f"Wrote file {output_file}")
        return "return code: 0Merging of tracks successful"
    except Exception as err:
        return "return code: 1Unknown error occurred during merging of tracks: %s" % err


def update_distance(gpx_with_correct_distances, gpx_track_to_be_updated):
    last_point = Extension.parse(gpx_with_correct_distances.tracks[-1].segments[-1].points[
                                     -1].extensions)
    delta = last_point.distance
    for track in gpx_track_to_be_updated.tracks:
        for segment in track.segments:
            points = []
            for point in segment.points:
                point.extensions_calculated = Extension.parse(point.extensions)
                set_tag_in_extensions(
                    gpx_track_to_be_updated,
                    delta + point.extensions_calculated.distance,
                    point,
                    "distance"
                )
                points.append(point)
            segment.points = points


def set_tag_in_extensions(gpx, value, point, tag_name):
    namespace_name = 'http://www.garmin.com/xmlschemas/TrackPointExtension/v1'
    namespace = '{' + namespace_name + '}'
    track_extensions = 'TrackPointExtension'
    tag = f"{namespace}{track_extensions}"
    if len([e for e in point.extensions if e.tag == tag]) == 0:
        gpx.nsmap["n3"] = namespace_name
        point.extensions.append(mod_etree.Element(tag))
    root = mod_etree.Element(namespace + tag_name)
    root.text = f"{value}"
    elements = [e for e in point.extensions if e.tag == tag][0]
    extensions = [e for e in elements if e.tag.endswith("}" + tag_name)]
    if len(extensions) == 0:
        elements.append(root)
    else:
        elements.remove(extensions[0])
        elements.append(root)


def get_time(gpx):
    if gpx.time:
        return gpx.time
    if gpx and len(gpx.tracks) > 0 and len(gpx.tracks[0].segments) > 0 and len(
            gpx.tracks[0].segments[0].points) > 0:
        return gpx.tracks[0].segments[0].points[0].time

import gpxpy.gpx
import json
import os
from datetime import date

from garminconnect import (
    Garmin,
    GarminConnectConnectionError,
    GarminConnectTooManyRequestsError,
    GarminConnectAuthenticationError,
)

BASE_URL = 'https://connect.garmin.com'


def get_authenticated_client(user_name, password):
    try:
        api = Garmin(user_name, password)
        api.login()
        return api
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client init: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client init %s" % err


def get_activities_by_date(api, startdate, enddate, activitytype):
    """Return available activities."""
    url = api.garmin_connect_activities
    activities = []
    start = 0
    limit = 20
    return_data = True

    while return_data:
        params = {
            "startDate": str(startdate),
            "endDate": str(enddate),
            "start": str(start),
            "limit": str(limit)
        }
        if activitytype:
            params["activityType"] = str(activitytype)
        additional_activities = api.modern_rest_client.get(url, params=params).json()
        if additional_activities:
            activities.extend(additional_activities)
            start = start + limit
        else:
            return_data = False
    return activities


def get_excercise_sets(api, activity_id):
    activity_id = str(activity_id)
    url = f"{api.garmin_connect_activities}/{activity_id}"
    return api.modern_rest_client.get(url).json()


def get_activity_json_for_date(client, date):
    try:
        activities = get_activities_by_date(client, date, date, None)
        return json.dumps(activities)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


def download_tcx(api, activity_id, output_file):
    try:
        gpx_data = api.download_activity(activity_id, dl_fmt=Garmin.ActivityDownloadFormat.TCX)
        with open(output_file, "wb") as fb:
            fb.write(gpx_data)
        return "return code: 0"
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


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
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


def get_multi_sport_data(api, activity_id):
    try:
        return get_excercise_sets(api, activity_id)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


def get_split_data(api, activity_id, folder):
    try:
        return download_splits(api, activity_id, folder)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


def get_power_data(api, date):
    """
    Get activity splits
    """
    try:
        start = 0
        limit = 20
        url = '/modern/proxy/fitnessstats-service/powerCurve/'
        params = {
            "startDate": str(date),
            "endDate": str(date),
            "start": str(start),
            "limit": str(limit)
        }
        print("Fetching power data with url %s", url)
        api.headers["nk"] = "NT"
        return api.modern_rest_client.get(url).json()
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activity power data: %s" % err
    except Exception as err:  # pylint: disable=broad-except
        return "return code: 1Unknown error occurred during Garmin Connect Client get power data %s" % err


def download_activities_by_date(api, folder, start_date, end_date=date.today()):
    try:
        activities = get_activities_by_date(api, start_date, end_date, None)
        write_index = 0
        for activity in activities:
            activity_id = activity["activityId"]
            if activity["activityType"]["typeId"] == 89:
                multi_sport_data = get_excercise_sets(api, activity_id)
                child_ids = multi_sport_data["metadataDTO"]["childIds"]
                activity["childIds"] = child_ids
                for id in child_ids:
                    details = get_excercise_sets(api, id)
                    output_file = f"{folder}/child_{str(id)}.json"
                    if not os.path.exists(output_file):
                        with open(output_file, "w+") as fb:
                            json.dump(details, fb)
                date = activity["startTimeLocal"].split(" ")
                if date and len(date) == 2:
                    update_power_data(activity, api, date[0])
            else:
                activity["childIds"] = []
            output_file = f"{folder}/activity_{str(activity_id)}.json"
            if not os.path.exists(output_file):
                with open(output_file, "w+") as fb:
                    json.dump(activity, fb)
                    write_index += 1
            download_splits(api, activity_id, folder)
        return "return code: 0\nDownloaded {} activities, wrote {} to file".format(len(activities),
                                                                                   write_index)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities by date: %s" % err
    except Exception as err:  # pylint: disable=broad-except
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities by date %s" % err


def download_splits(api, activity_id, folder):
    splits = api.get_activity_splits(activity_id)
    output_file = f"{folder}/activity_{str(activity_id)}_splits.json"
    if not os.path.exists(output_file):
        with open(output_file, "w+") as fb:
            json.dump(splits, fb)
    return splits


def update_power_data(activity, api, date):
    power_data = get_power_data(api, date)
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


suffix = "_simplified"


def prefix_filename(fn: str, prefix: str) -> str:
    return fn.replace(".gpx", prefix + ".gpx")


def get_gpx_data(gpx):
    extremes = gpx.get_elevation_extremes()
    gpx.smooth()
    moving_data = gpx.get_moving_data()
    uphill_downhill = gpx.get_uphill_downhill()
    return {
        "duration": gpx.get_duration(),
        "min_elevation": extremes.minimum,
        "max_elevation": extremes.maximum,
        "number_points": gpx.get_points_no(),
        "elevation_gain": uphill_downhill.uphill,
        "elevation_loss": uphill_downhill.downhill,
        "moving_time": moving_data.moving_time,
        "moving_distance": moving_data.moving_distance,
        "max_speed": moving_data.max_speed,
    }


def analyze_gpx_track(path):
    try:
        gpx_file = open(path, 'r')
        gpx_file_simplified = prefix_filename(path, suffix)
        gpx_file_gpxpy = path.replace(".gpx", "_gpxpy.json")
        gpx = gpxpy.parse(gpx_file)
        data = get_gpx_data(gpx)
        with open(gpx_file_gpxpy, 'w') as fp:
            json.dump(data, fp, indent=4)
        gpx.simplify()
        with open(gpx_file_simplified, 'w') as f:
            f.write(gpx.to_xml())
        return "return code: 0"
    except Exception as err:  # pylint: disable=broad-except
        return "return code: 1Unknown error occurred %s" % err

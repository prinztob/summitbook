import json
import os
from datetime import date
from garminconnect import (
    Garmin,
    GarminConnectConnectionError,
    GarminConnectTooManyRequestsError,
    GarminConnectAuthenticationError, BASE_URL,
)


def get_authenticated_client(user_name, password):
    try:
        client = Garmin(user_name, password)
        client.login()
        return client
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client init: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client init %s" % err

def get_activity_json_for_date(client, date):
    try:
        activities = client.get_activities_by_date(date, date, None)
        return json.dumps(activities)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err

def download_tcx(client, activity_id, output_file):
    try:
        gpx_data = client.download_activity(activity_id, dl_fmt=client.ActivityDownloadFormat.TCX)
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


def download_gpx(client, activity_id, output_file):
    try:
        gpx_data = client.download_activity(activity_id, dl_fmt=client.ActivityDownloadFormat.GPX)
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


def get_multi_sport_data(client, activity_id):
    try:
        return client.get_excercise_sets(activity_id)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err

def get_split_data(client, activity_id, folder):
    try:
        return download_splits(client, activity_id, folder)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities: %s" % err
    except Exception as err:
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities %s" % err


def get_power_data(client, date):
    """
    Get activity splits
    """
    try:
        start = 0
        limit = 20
        activitiesurl = BASE_URL + '/modern/proxy/fitnessstats-service/powerCurve/?startDate=' + str(date) + \
                        '&endDate=' + str(date)  + '&start=' + str(start) + '&limit=' + str(limit)
        print("Fetching power data with url %s", activitiesurl)
        client.headers["nk"] = "NT"
        return client.fetch_data(activitiesurl)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activity power data: %s" % err
    except Exception as err:  # pylint: disable=broad-except
        return "return code: 1Unknown error occurred during Garmin Connect Client get power data %s" % err


def download_activities_by_date(client, folder, start_date, end_date=date.today()):
    try:
        activities = client.get_activities_by_date(start_date, end_date, None)
        write_index = 0
        for activity in activities:
            activity_id = activity["activityId"]
            if activity["activityType"]["typeId"] == 89:
                multi_sport_data = client.get_excercise_sets(activity_id)
                child_ids =  multi_sport_data["metadataDTO"]["childIds"]
                activity["childIds"] = child_ids
                for id in child_ids:
                    details = client.get_excercise_sets(id)
                    output_file = f"{folder}/child_{str(id)}.json"
                    if not os.path.exists(output_file):
                        with open(output_file, "w+") as fb:
                            json.dump(details, fb)
                date = activity["startTimeLocal"].split(" ")
                if date and len(date) == 2:
                    update_power_data(activity, client, date[0])
            else:
                activity["childIds"] = []
            output_file = f"{folder}/activity_{str(activity_id)}.json"
            if not os.path.exists(output_file):
                with open(output_file, "w+") as fb:
                    json.dump(activity, fb)
                    write_index += 1
            download_splits(client, activity_id, folder)
        return "return code: 0\nDownloaded {} activities, wrote {} to file".format(len(activities), write_index)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        return "return code: 1Error occurred during Garmin Connect Client get activities by date: %s" % err
    except Exception as err:  # pylint: disable=broad-except
        return "return code: 1Unknown error occurred during Garmin Connect Client get activities by date %s" % err


def download_splits(client, activity_id, folder):
    splits = client.get_activity_splits(activity_id)
    output_file = f"{folder}/activity_{str(activity_id)}_splits.json"
    if not os.path.exists(output_file):
        with open(output_file, "w+") as fb:
            json.dump(splits, fb)
    return splits


def update_power_data(activity, client, date):
    power_data = get_power_data(client, date)
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

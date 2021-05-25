from garminconnect import (
    Garmin,
    GarminConnectConnectionError,
    GarminConnectTooManyRequestsError,
    GarminConnectAuthenticationError,
)
import json
import os

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
        print("Error occurred during Garmin Connect Client init: %s" % err)
    except Exception:
        print("Unknown error occurred during Garmin Connect Client init")

def get_json_for_date(client, date):
    try:
        activities = client.get_activities_by_date(date, date, None)
        return json.dumps(activities)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        print("Error occurred during Garmin Connect Client get activities: %s" % err)
    except Exception:
        print("Unknown error occurred during Garmin Connect Client get activities")

def download_gpx(client, activity_id, output_file):
    try:
        gpx_data = client.download_activity(activity_id, dl_fmt=client.ActivityDownloadFormat.GPX)
        with open(output_file, "wb") as fb:
            fb.write(gpx_data)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        print("Error occurred during Garmin Connect Client get activities: %s" % err)
    except Exception:  # pylint: disable=broad-except
        print("Unknown error occurred during Garmin Connect Client get activities")


def get_multi_sport_data(client, activity_id):
    try:
        return client.get_excercise_sets(activity_id)
    except (
            GarminConnectConnectionError,
            GarminConnectAuthenticationError,
            GarminConnectTooManyRequestsError,
    ) as err:
        print("Error occurred during Garmin Connect Client get activities: %s" % err)
    except Exception:  # pylint: disable=broad-except
        print("Unknown error occurred during Garmin Connect Client get activities")
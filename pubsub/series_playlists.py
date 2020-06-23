#!/usr/bin/python3.8

'''A script that will ensure every series from meta has an associated
playlist.'''

import os
from urllib.parse import quote
import requests
from exception_reporting import report

PLAYLIST_SERVER = os.environ.get("PLAYLIST_HOST")
META_SERVER = os.environ.get("META_HOST")

SYSTEM_POSTFIX = ":SYSTEM"
TEST = "SERIES PLAYLIST"

def get_all_series():
    '''Get all series known to meta service'''
    request = requests.get(url=META_SERVER + "series")
    json = request.json()
    print("all series: " + str(json))
    return json["results"]

def is_playlist_defined(playlist_name, playlist):
    '''Does a playlist exist for this series?'''
    request = requests.get(url=PLAYLIST_SERVER +
                           quote(playlist_name, safe=''))
    json = request.json()
    if 'items' in json.keys():
        print("Is " + str(playlist) + " == " +
              str(json['items']) + "? " +
              str(playlist == json['items']))
        return playlist == json['items']
    return False

def get_playlist(series):
    '''Get the elements that should make up a playlist.'''
    request = requests.get(url=META_SERVER +
                           "series/" +
                           quote(series, safe=''))
    json = request.json()
    print("playlist: " + str(json) + " for series " + quote(series, safe=''))
    return json["catalog_ids"]

def save_playlist(series, playlist):
    '''Save a new playlist'''
    request = requests.put(url=PLAYLIST_SERVER +
                           quote(series + SYSTEM_POSTFIX, safe=''),
                           json={"playlist":playlist})
    print("Put for series '" + series + "' was: " + str(request.json()))
    if request.json()["status"] != "ok":
        other_request = requests.post(url=PLAYLIST_SERVER +
                                      quote(series + SYSTEM_POSTFIX, safe=''),
                                      json={"playlist":playlist})
        print("Post for series '" + series + "' was: " +
              str(other_request.json()))

def series_playlist(series_list):
    '''Run the script, where series_list is a list of series to check'''
    failure_results = []
    remediation_succeeded = True
    for series in series_list:
        playlist = get_playlist(series)
        print("series: " + series + "\n\tplaylist: " + str(playlist))
        if not is_playlist_defined(series + SYSTEM_POSTFIX, playlist):
            failure_results.append(series)
            save_playlist(series, playlist)
            playlist = get_playlist(series)
            remediation_succeeded &= is_playlist_defined(series+SYSTEM_POSTFIX,
                                                         playlist)
    return (failure_results, remediation_succeeded)

def run(args):
    '''Meets message interface requirements. Argument for testing.'''
    orig_args = args
    if args == []:
        args = get_all_series()
    (failure, remediation) = series_playlist(args)
    pass_fail = len(failure) == 0
    report(TEST, pass_fail, str(failure), orig_args, remediation=remediation)


if __name__ == "__main__":
    run([])

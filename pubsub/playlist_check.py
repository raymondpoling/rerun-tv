#!/usr/bin/python3.8

'''A script that will ensure every playlist has valid data.'''

import os
from urllib.parse import quote
import requests
from exception_reporting import report

PLAYLIST_SERVER = os.environ.get("PLAYLIST_HOST")
META_SERVER = os.environ.get("META_HOST")

TEST = "PLAYLIST IDS"

def get_all_playlists():
    '''Get all series known to meta service'''
    request = requests.get(url=PLAYLIST_SERVER)
    json = request.json()
    print("all playlists: " + str(json))
    return list(map(lambda x: x['name'], json["playlists"]))

def fetch_playlist(playlist):
    '''Get playlist (catalog_ids) from playlist service'''
    request = requests.get(url=PLAYLIST_SERVER + quote(playlist, safe=""))
    return request.json()['items']

def is_catalog_id_defined(catalog_id):
    '''Does a catalog id exist?'''
    request = requests.get(url=META_SERVER + "catalog-id/" +
                           catalog_id + "?fields=series")
    json = request.json()
    return json['status'] == 'ok'

def check_playlist(playlist_list):
    '''Run the script, where series_list is a list of series to check'''
    failed_ids = {}
    for playlist in playlist_list:
        print("playlist: " + playlist)
        catalog_ids = fetch_playlist(playlist)
        failures = list(
            filter(lambda t: not is_catalog_id_defined(t),
                   catalog_ids))
        if failures != []:
            failed_ids[playlist] = failures
    return failed_ids

def run(args):
    '''To match the interface of messages for running tests.'''
    orig_args = args
    if args == []:
        args = get_all_playlists()
    print("Checking args: " + str(args))
    result = check_playlist(args)
    report(TEST, len(result) == 0, str(result), orig_args)
    print("Failed ids? " + str(result))

if __name__ == "__main__":
    run([])

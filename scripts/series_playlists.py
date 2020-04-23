#!/usr/bin/python

'''A script that will ensure every series from meta has an associated
playlist.'''

from urllib.parse import quote
import requests

PLAYLIST_SERVER = "http://playlist:4001"
META_SERVER = "http://meta:4004"

SYSTEM_POSTFIX = ":SYSTEM"

def get_all_series():
    '''Get all series known to meta service'''
    request = requests.get(url=META_SERVER + "/series")
    json = request.json()
    print("all series: " + str(json))
    return json["results"]

def is_playlist_defined(playlist_name, playlist):
    '''Does a playlist exist for this series?'''
    request = requests.get(url=PLAYLIST_SERVER + "/" +
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
                           "/series/" +
                           quote(series, safe=''))
    json = request.json()
    print("playlist: " + str(json) + " for series " + quote(series, safe=''))
    return json["catalog_ids"]

def save_playlist(series, playlist):
    '''Save a new playlist'''
    request = requests.put(url=PLAYLIST_SERVER + "/" +
                           quote(series + SYSTEM_POSTFIX, safe=''),
                           json={"playlist":playlist})
    print("Put for series '" + series + "' was: " + str(request.json()))
    if request.json()["status"] != "ok":
        other_request = requests.post(url=PLAYLIST_SERVER + "/" +
                                      quote(series + SYSTEM_POSTFIX, safe=''),
                                      json={"playlist":playlist})
        print("Post for series '" + series + "' was: " +
              str(other_request.json()))

def main():
    '''Run the script'''
    all_series = get_all_series()
    for series in all_series:
        playlist = get_playlist(series)
        print("series: " + series + "\n\tplaylist: " + str(playlist))
        if not is_playlist_defined(series + SYSTEM_POSTFIX, playlist):
            save_playlist(series, playlist)

main()

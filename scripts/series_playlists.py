#!/usr/bin/python

from urllib import quote
import requests

playlist_server = "http://playlist:4001"
meta_server = "http://meta:4004"

system_postfix = ":SYSTEM"

def get_all_series():
    request = requests.get(url = meta_server + "/series")
    json = request.json()
    print "all series: " + str(json)
    return json["results"]

def is_playlist_defined(playlist_name,playlist):
    request = requests.get(url = playlist_server + "/" +
                           quote(playlist_name,safe=''))
    json = request.json()
    if 'items' in json.keys():
        print "Is " + str(playlist) + " == " + str(json['items']) + "? " + str(playlist == json['items'])
        return playlist == json['items']
    else:
        return False

def get_playlist(series):
    request = requests.get(url = meta_server + "/series/" + quote(series,safe=''))
    json = request.json()
    print "playlist: " + str(json) + " for series " + quote(series,safe='')
    return json["catalog_ids"]

def save_playlist(series,playlist):
    request = requests.put(url = playlist_server + "/" + quote(series + system_postfix, safe=''), json = {"playlist":playlist})
    print "Put for series '" + series + "' was: " + str(request.json())
    if (request.json()["status"] != "ok"):
        r2 = requests.post(url = playlist_server + "/" + quote(series + system_postfix,safe=''), json = {"playlist":playlist})
        print "Post for series '" + series + "' was: " + str(r2.json())

def main():
    all_series = get_all_series()
    for series in all_series:
        playlist = get_playlist(series)
        print "series: " + series + "\n\tplaylist: " + str(playlist)
        if(not is_playlist_defined(series + system_postfix, playlist)):
            save_playlist(series,playlist)

main()

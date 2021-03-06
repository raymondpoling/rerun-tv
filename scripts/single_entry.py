#!/usr/bin/python

'''Add new items to rerun-tv using login and fronend bulk-updates.'''

from os import listdir
from os.path import isfile, join, isdir
from sys import argv
import json
import requests

FRONTEND = ""
USER = ""
PASSWORD = ""
SERVER_CERT = ""

def login(session, user, password):
    '''Log into frontend using user/password defined above'''
    url = FRONTEND + "/login"
    data = {"username": user, "password": password}
    session.post(url=url,
                 data=data,
                 verify=SERVER_CERT)
    return session.cookies

def bulk_create(series, ep_list, session):
    '''Add new entries via bulk-update in frontend'''
    url = FRONTEND + "/bulk-update.html"
    series_map = {"name": series}
    load = json.dumps({"series":series_map, "records":ep_list})
    post = {"update":load, "series":series, "create?":"true"}
    print("To Create: " + str(post))
    session.post(url=url, data=post)

def insert_season(name, season, list_of_items):
    '''Collect all episodes from a season.'''
    prepare = zip(range(len(list_of_items)), list_of_items)

    out = []
    for (idx, obj) in prepare:
        print("name: " + name + "\n" + "\tseason: " + str(season) + "\n" +
              "\tidx: " + str(idx + 1) + "\n" + "\tobj: " + str(obj) + "\n")
        out += [{"episode":idx + 1,
                 "season":season,
                 "locations":["file://CrystalBall" + str(obj)]}]
    return out

# Quick check below
#insert_name("testing")

#insert_playlist("testing", ["a", "b", "c"])

def walking(rootdir, series):
    '''Walk a given directory producing either episodes, or seasons
    of episodes to upload'''
    print("Inserting name: " + series + "\n")
    episodes = []
    all_seasons = [f for f in listdir(rootdir) if isdir(join(rootdir, f))]
    if all_seasons == []:
        episodes = [join(rootdir, f)
                    for f
                    in listdir(rootdir)
                    if isfile(join(rootdir, f))]
        episodes.sort()
        return insert_season(series, 1, episodes)
    season_n = 1
    episode_n = 0
    all_seasons.sort()
    all_eps = []
    for season in all_seasons:
        episodes = [join(rootdir, season, f)
                    for f
                    in listdir(join(rootdir, season))
                    if isfile(join(rootdir, season, f))]
        episodes.sort()
        all_eps += insert_season(series, season_n, episodes)
        season_n += 1
        episode_n = episode_n + len(episodes)
    return all_eps

def main():
    '''Run the application'''
    print("arguments: " + str(argv))
    if len(argv) == 1:
        print("Usage: single-entry.py <path> <series name>")
        return
    path = argv[1]
    series = argv[2]
    to_create = walking(path, series)
    session = requests.Session()
    login(session, USER, PASSWORD)
    bulk_create(series, to_create, session)

main()

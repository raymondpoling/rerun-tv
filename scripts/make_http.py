#!/usr/bin/python

'''This script takes files from locator as identified by meta,
and ensures every file reference has an http reference. Mostly only
useful locally.'''

from urllib.parse import quote
from re import sub, match
import requests

META_SERVER = "http://meta:4004"
LOCATOR_SERVER = "http://locator:4005"

MATCH_PAT = r"file://CrystalBall/home/ruguer/Videos(.*)"
REPLACE_PAT = r"/video\1"
HTTP_EXISTS = r"http://archive/video.*"

def get_series():
    '''Get a list of all series meta knows'''
    result = requests.get(url=META_SERVER + "/series")
    return result.json()['results']

def test_results(locations):
    '''Check if a set of locations has an HTTP_EXISTS'''
    for location in locations:
        if match(HTTP_EXISTS, location):
            return True
    return False

def create_http(locations):
    '''Create the http resource based on existing file resource'''
    for location in locations:
        if match(MATCH_PAT, location):
            return sub(MATCH_PAT, REPLACE_PAT, location)
    return None

def walk_series(series):
    '''For every series, get the catalog_ids of episodes and use
    it to check for existing locations.'''
    for serie in series:
        print("Working on serie " + serie)
        result = requests.get(url=META_SERVER +
                              "/series/" +
                              quote(serie, safe=''))
        print("For " + serie + " processing: " + str(result.json()['catalog_ids']))
        catalog_ids = result.json()['catalog_ids']
        for item in catalog_ids:
            print("Processing item: " + item)
            result = requests.get(url=LOCATOR_SERVER +
                                  "/catalog-id/" +
                                  item)
            print("Got result: " + str(result.json()))
            locations = result.json()['files']
            print("Existing locations for " + item + ": " + str(locations))
            if not test_results(locations):
                http = create_http(locations)
                print("New location is: " + str(http))
                if http is not None:
                    result = requests.post(url=LOCATOR_SERVER +
                                           "/http/archive/" + item,
                                           json={"path": http})
                    print("Result is " + str(result))
                else:
                    print("No file location for " + item)

def main():
    '''Run the program'''
    series = get_series()
    print("All series: " + str(series))
    walk_series(series)

main()

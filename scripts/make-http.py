#!/usr/bin/python

from urllib import quote
import requests
from re import sub, match

meta_server = "http://meta:4004"
locator_server = "http://locator:4005"

match_pat = r"file://CrystalBall/home/ruguer/Videos(.*)"
replace_pat = r"/video\1"
http_exists = r"http://archive/video.*"

def get_series():
    result = requests.get(url = meta_server + "/series")
    return result.json()['results']

def test_results(locations):
    for x in locations:
        if match(http_exists,x):
            return True
    return False

def create_http(locations):
    for x in locations:
        if match(match_pat,x):
            return sub(match_pat,replace_pat,x)
    return None

def walk_series(series):
    for serie in series:
        print "Working on serie " + serie
        result = requests.get(url = meta_server +
                              "/series/" +
                              quote(serie, safe=''))
        print "For " + serie + " processing: " + str(result.json()['catalog_ids'])
        catalog_ids = result.json()['catalog_ids']
        for item in catalog_ids:
            print "Processing item: " + item
            result = requests.get(url = locator_server +
                                  "/catalog-id/" +
                                  item)
            print "Got result: " + str(result.json())
            locations = result.json()['files']
            print "Existing locations for " + item + ": " + str(locations)
            if not test_results(locations):
                http = create_http(locations)
                print "New location is: " + str(http)
                if http is not None:
                    result = requests.post(url = locator_server +
                                           "/http/archive/" + item,
                                           json = {"path": http})
                    print "Result is " + str(result)
                else:
                    print "No file location for " + item

def main():
    series = get_series()
    print "All series: " + str(series)
    walk_series(series)

main()

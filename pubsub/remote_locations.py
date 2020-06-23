#!/usr/bin/python3.8

'''Tests to see if http resources are available.'''

import os
from urllib.parse import quote
import requests
from exception_reporting import report

LOCATOR_SERVER = os.environ.get("LOCATOR_HOST")
META_SERVER = os.environ.get("META_HOST")

CHECKING = "http/archive/"
TEST = "REMOTE LOCATIONS"

def get_all_series():
    '''Get all series in system for testing. Catchall case.'''
    result = requests.get(url=META_SERVER + "series")
    return result.json()['results']

def catalog_ids(series):
    '''Get the catalog ids for series.'''
    result = requests.get(url=META_SERVER + "series/" + quote(series, safe=''))
    return result.json()['catalog_ids']

def get_location(catalog_id):
    '''Get the available locations for catalog ids.'''
    result = requests.get(url=LOCATOR_SERVER + CHECKING + catalog_id)
    try:
        return result.json()['url']
    except BaseException:
        return None

def check_remote(remote):
    '''Check the remote location for validity.'''
    url = quote(remote, safe=":/")
    request = requests.head(url=url)
    result = request.status_code == 200
    print("For location: " + str(url) + " pass? " + str(request.status_code))
    return result

def check_locations(series_list):
    '''Given a list of series, check locations.'''
    failures = []
    for series in series_list:
        ids = catalog_ids(series)
        print("Checking remote locations for series: " + series, flush=True)
        for an_id in ids:
            location = get_location(an_id)
            if location is None:
                failures.append(('No location found!', an_id))
            elif not check_remote(location):
                failures.append(('Could not find file: ' + location, an_id))
    return failures

def run(args):
    '''Matches the interface for messages for testing. args
    are the series to test'''
    orig_args = args
    if args == []:
        args = get_all_series()
    failures = check_locations(args)
    pass_fail = len(failures) == 0
    report(TEST, pass_fail, str(failures), orig_args)

if __name__ == "__main__":
    run([])

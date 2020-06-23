#!/usr/bin/python3.8

'''Verify that naming patterns meet standard defined by a given source.'''

import os
from urllib.parse import quote
import requests
from exception_reporting import report

LOCATOR_SERVER = os.environ.get("LOCATOR_HOST")
META_SERVER = os.environ.get("META_HOST")

TEST = "ROOT LOCATIONS"
BASE = os.environ.get("BASE")
ROOTS = [BASE] + os.environ.get("ROOTS").split(",")

def get_all_series():
    '''Get a list of all series.'''
    result = requests.get(url=META_SERVER + "series")
    return result.json()['results']

def get_series(series):
    '''Get the catalog_ids for a series.'''
    result = requests.get(url=META_SERVER + "series/" + quote(series, safe=""))
    return result.json()['catalog_ids']

def make_pair(path):
    '''Make a pair of root and the rest of a path.'''
    for root in ROOTS:
        if path.startswith(root):
            return (root, path[len(root):])
    return None

def make_pairs(path_list):
    '''Convert the path list to pairs of roots and the rest of the path.'''
    return list(filter(lambda p: p is not None,
                       map(make_pair, path_list)))

def all_roots(pair_list, files):
    '''Verify all roots exist.'''
    return len(ROOTS) == len(set(map(lambda x: x[0], pair_list))) == len(files)

def all_paths(pair_list):
    '''Ensure all paths have the same terminus (matching file structure).'''
    return len(set(map(lambda x: x[1], pair_list))) == 1

def get_locations(catalog_id):
    '''Get the locations based on catalog id.'''
    result = requests.get(url=LOCATOR_SERVER + "catalog-id/" + catalog_id)
    return result.json()['files']

def make_locations(pair_list):
    '''Select the BASE root for creating expected path lists.'''
    try:
        select = list(filter(lambda x: x[0] == BASE, pair_list))[0][1]
        return list(map(lambda x: x + select, ROOTS))
    except BaseException:
        return list(map(lambda x: x[0] + x[1], pair_list))

def update_locations(catalog_id, pair_list):
    '''Using the base root, create expected paths list.'''
    updated = make_locations(pair_list)
    requests.put(url=LOCATOR_SERVER + "catalog-id/" + catalog_id,
                 json={"files": updated})

def test_series(series_list):
    '''Run tests against the list of series.'''
    failures = []
    remediation = True
    for series in series_list:
        print("Checking series: " + series, flush=True)
        catalog_ids = get_series(series)
        for an_id in catalog_ids:
            locations = get_locations(an_id)
            pairs = make_pairs(locations)
            if not (all_roots(pairs, locations) and all_paths(pairs)):
                print("series failed: " + series + " id: " + an_id)
                failures.append(("catalog_id "+an_id, locations))
                update_locations(an_id, pairs)
                locations = get_locations(an_id)
                pairs = make_pairs(locations)
                if not (all_roots(pairs, locations) and all_paths(pairs)):
                    remediation = False
    return (remediation, failures)

def run(args):
    '''Match the expected interface defined in messages. Args are arguments.'''
    orig_args = args
    if args == []:
        args = get_all_series()
    (remediation, failures) = test_series(args)
    pass_fail = len(failures) == 0
    report(TEST, pass_fail, str(failures), orig_args, remediation=remediation)

if __name__ == "__main__":
    run([])

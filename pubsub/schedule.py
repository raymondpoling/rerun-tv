#!/usr/bin/python3.8

'''Schedule validity, ensure that schedules are still valid.'''

import os
from urllib.parse import quote
import requests
from exception_reporting import report

SCHEDULE_SERVER = os.environ.get("SCHEDULE_HOST")
BUILDER_SERVER = os.environ.get("BUILDER_HOST")

TEST = "SCHEDULE VALIDITY"

def get_schedules():
    '''Get all schedules for testing.'''
    result = requests.get(url=SCHEDULE_SERVER)
    return result.json()['schedules']

def check_schedule(schedule):
    '''Check a specific schedule for validity errors.'''
    result = requests.get(url=BUILDER_SERVER + "schedule/validate/" +
                          quote(schedule, safe=''))
    result_json = result.json()
    if result_json['status'] != "ok":
        return result_json['messages']
    return []

def check_schedules(schedule_list):
    '''Check a list of schedules.'''
    failures = []
    for schedule in schedule_list:
        checked = check_schedule(schedule)
        print("checked " + schedule + " got " + str(checked))
        if checked:
            failures.append((schedule, checked))
    return failures

def run(args):
    '''Meets the interface defined by messages for running tests.'''
    orig_args = args
    if args == []:
        args = get_schedules()
    failures = check_schedules(args)
    print("failures are: " + str(failures))
    pass_fail = len(failures) == 0
    report(TEST, pass_fail, str(failures), orig_args)

if __name__ == "__main__":
    run([])

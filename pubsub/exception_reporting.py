#!/usr/bin/python3.8

'''A script that will report errors.'''

import json
import os
from datetime import datetime
import requests

EXCEPTION_SERVER = os.environ.get("EXCEPTION_HOST")

def create_test(test):
    '''Create a test if it does not yet exist in database.'''
    print("making test: " + test)
    requests.post(url=EXCEPTION_SERVER + "test/" + test,
                      json={'Cron':'', 'Name':test})

def post_report(jsondoc):
    '''Publish the report to the database.'''
    return requests.post(url=EXCEPTION_SERVER + "result/" + jsondoc['test'],
                         json=jsondoc).json()

def report(test, passing, status_message, args, remediation=False):
    '''Define record to publish to database.'''
    print("type of args is: " + str(type(json.dumps(args))))
    date = datetime.now().isoformat()
    jsondoc = {'test': test,
               'passFail':passing,
               'statusMessage': status_message,
               'args': str(json.dumps(args)),
               'remediationSucceeded': remediation,
               'date': str(date)}
    print("json: " + str(jsondoc))
    try:
        post_report(jsondoc)
    except json.decoder.JSONDecodeError:
        create_test(test)
        post_report(jsondoc)

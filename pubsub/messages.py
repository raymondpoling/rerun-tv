#!/usr/bin/python3.6

'''Main application, gets message from redis, and processes the test to run.'''

import os
import sys
import traceback
import json
import redis
import schedule
import root_locations
import remote_locations
import series_playlists
import playlist_check
import ensure_tags

REDIS_SERVER = os.environ.get("REDIS_SERVER")
REDIS_PORT = os.environ.get("REDIS_PORT")

def create_redis():
    '''Create redis connection.'''
    return redis.Redis(host=REDIS_SERVER, port=REDIS_PORT)

def subscribe(redis_conn):
    '''Subscribe to exception.'''
    pubsub = redis_conn.pubsub()
    pubsub.subscribe("exception")
    return pubsub

def select_test(name):
    '''Select the test to run.'''
    return {
        schedule.TEST: schedule.run,
        root_locations.TEST: root_locations.run,
        remote_locations.TEST: remote_locations.run,
        series_playlists.TEST: series_playlists.run,
        playlist_check.TEST: playlist_check.run,
        ensure_tags.TEST: ensure_tags.run
    }.get(name, lambda x: print("Test not found: " + name +
                                "\n\targs: " + str(x)))

def main():
    '''Run the application. Get a message from redis, and run test with
    args.'''
    redis_conn = create_redis()
    pubsub = subscribe(redis_conn)
    for new_message in pubsub.listen():
        print("message: " + str(new_message))
        message = {}
        try:
            message = new_message['data'].decode('utf-8')
            j = json.loads(message)
            test = j['test']
            arguments = j['args']
            print("testing: " + str(test) + " args: " + str(arguments))
            selected = select_test(test)
            print("selected? " + str(selected))
            selected(arguments)
            print("??? " + str(j), flush=True)
        except BaseException:
            info = sys.exc_info()
            print("Cannot process: " + str(new_message['data']) +
                  "\n\t" + str(info[0]))
            traceback.print_exception(info[0], info[1], info[2],
                                      file=sys.stdout)
        finally:
            print("", flush=True)

if __name__ == "__main__":
    main()

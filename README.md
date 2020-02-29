# schedule-builder

Uses schedule and playlist to allow for building schedules. Also
 provides validation services for schedules. So, the following
 responsibilities:

 - Get a list of available playlists, or the length of a specific
 playlist.
 - Allow for generating a schedule, and validating the lengths
 of the playlists in that schedule.
 - This will be used by a periodic validator to ensure changes to
 playlists will not invalidate a schedule.

## Exposed Interface

- GET /playlists
  Get a list of all playlists.
  * [{"name":"playlist1","length":12,{"name":"playlist2","length":21},...] A json
  array of JSON objects of all playlists and their lengths. Used for creating a
  schedule.
- GET /playlist/:playlist
  Get a playlist with name :playlist.
  * NOT IMPLEMENTED
- POST /schedule/store/:schedule
  Store a new schedule (refer to schedule for format). Also performs basic
  validations in addition to correct formatting done by schedule.
  * {"status":"ok"} The schedule was stored correctly.
  * {"status":"failure","message":"cannot create schedule"} Some error occurred
    when storing the schedule.
  * {"status":"invalid","message":"failed validations":["series_Test1",...] where
  the list series_Test1 and ... are either invalid playlists or the schedule is
  trying to store invalid lengths.
  * {"status":"invalid","message":"invalid schedule"} The posted schedule is
  invalid, either because it has the wrong or missing name, does not have a
  playlists section, or no schedule was posted at all.
  * Other responses for this operation in the schedule API /schedule/store/:schedule
- PUT /schedule/store/:schedule
  Overwrite an existing schedule with name :schedule. All outputs are the same
  as for POST above.
- GET /schedule/validate
  Validate a schedule JSON body. Essentially the same output from POST except
  you cannot get "message":"cannot create schedule".
- GET /schedule/validate/:schedule
  Revalidate an existing schedule. Useful for checking schedules when a playlist
  is updated for checking if the given schedule is affected. :schedule is the
  name of the schedule to check.
  * As for GET /schedule/validate.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 FIXME

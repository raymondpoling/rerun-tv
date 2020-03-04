# playlist-schedule

Manages and resolves schedules into json playlists. Schedules are a
template filled with basic playlist information. Essentially, the
output should be [{"playlist":"playlist-a","index":23},
{"playlist":"playlist-b":,"index":2},...]. This way it has minimal
knowledge about the playlists: only the name of the list, and the
total length of that element. Then, the information can be resolved
via playlist listings (and from there file meta and/or file location).

So, the data format will be:
{"name":"schedule name", "schedule": [{"type":"simple", "playlist":
{"name": "playlist a name", "length": 24},
{"type":"complex", "playlist":[{...}]}

So, a name and a schedule. The schedule is a list (array) of
descriptors. The following descriptors are defined:
- simple: just a
playlist name and length.
- merge: a list of playlist name and
length. Treated as a concatenation.
- complex: Each listed subelement
(merge or simple) is alternated over.
- multi: shows up multiple
times in a schedule, to allow for playing multiple episodes of a show
in a playlist. Duplicated elements with information on:
* how many rows there is
* the simple/merge list used to make it up

## Exposed Interface

- POST /:name
  With a JSON body formatted {"name"::name, "playlists":[...]} where :name is the
  same name as the schedule in the URL and ... is formatted list of items as per
  the summary above.
  * {"status":"ok"} The new schedule stored correctly.
  * {"status":"failed","message":":name is already defined."} with a 400 status code means an
  attempt to overwrite an existing schedule was attempted.
  * {"status":"failed","message":"..."} with a 412 status code means an unspecified error occurred,
  and ... is the error message.
- GET /:name
  Returns the schedule JSON document associated with the name if it exists.
  * {"status":"ok", "schedule": {"name"::name, "playlists": [...]}} is the requested schedule JSON document.
  * {"status":"not-found"} means the schedule does not exist.
- GET /:name/:index
  Returns a JSON array of reference ids (catalog ids in this implementation) of
  the rendered playlist for the schedule at this index.
  * {"status":"ok", "items":[{"name":"playlist_name1","index":3},{"name":"playlist_name2","index":17},...]}
   is a playlist derived from the schedule before being formatted.
- PUT /:name
  Takes a schedule JSON as specified under POST /:name, and is used to replace
  an existing schedule. Used for updating an existing schedule.
  * {"status":"ok"} if the document is updated correctly.
  * {"status":"failed","message":"..."} with a 412 status code means an unspecified error occurred,
  and ... is possibly more information.
- DELETE /:name
  Deletes an exiting schedule with the given :name.
  * {"status":"ok"} if the schedule is deleted.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

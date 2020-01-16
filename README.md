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


## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond M. Poling

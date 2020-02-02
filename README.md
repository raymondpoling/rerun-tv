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

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 FIXME

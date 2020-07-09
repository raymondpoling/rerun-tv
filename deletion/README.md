# deletion

Provides basic deletion services. Media services may nominate items for
deletion, and admin users may execute those deletions. Deletions may be:
- Playlist
- Schedule
- Series
- Season
- Episode

## Exposed Interface

- POST /nominate/:type/:name
  :type is one of playlist, schedule, series, or episode. :name is either the
  name of the schedule or playlist, or the catalog-id for series, season, or
  episode.
  * {"status":"ok"} deletion record created
  * {"status":"failed", "message":msg} means that a deletion record could not be
  created for the item. Example failures would be because an *outstanding*
  record already exists, or database issues.
- POST /execute/:type/:name
  Same as above, except a record for :type/:name must already exist, and that
  item should be deleted.
  * {"status":"ok"} record was deleted
  * {"status":"failed","message":msg} means that the deletion could not be
    performed for some reason.
- POST /reject/:type/:name
  Same as above for :type/:name. This means the execution was rejected. This
  does not prevent a new nomination from being created.


## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

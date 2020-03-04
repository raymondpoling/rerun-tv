# format

Formats the output in preferred format. Currently only m3us, but expect to do
zip files or tars with playlist and files together, and maybe other formats.

## Exposed Interface

- GET /:user/:schedule-name
  For a given :user, return the current playlist for :schedule-name (uses the user
    API for determining current schedule index and auto-increment it).
  * An m3u file with the name:
  :schedule-name - :index.m3u

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

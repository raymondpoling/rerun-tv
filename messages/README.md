# messages

Allows posting of system or admin messages to inform user about system changes,
downtime, etc.

## Exposed Interface

- POST /
  Takes a parameters for author, title, and information. It generates posted date
  internally.
  * {"status":"ok"} message was saved.
  * {"status":"failed",  "message": "service failed"} with status code 500 means
   the message could not be saved.
- GET /
  Takes parameters for step and start. Step is the number of messages to return,
  start is the message number to start from (in descending order).
  * {"status":"ok", "events": [{"author":"System", "posted": ISO-DATETIME,
    "information":"the body of an article", "title": "name of article"},...]}
  * {"status":"failed",  "message": "service failed"} with status code 500 means
   the message could not be fetched.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

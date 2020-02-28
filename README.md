# file-locator

Associates catalog ids with file location(s). A single file could have multiple
locations, so that ideally this file can be accessed from multiple locations
depending on where it is being ran.

## Exposed Interface

GET /:protocol/:host/:catalog_id
  Returns a url for the given :catalog_id, based on requested :host and :protocol.
  * {"status":"ok","url":"some url"} where some url is a url/uri for the file
  referenced by the catalog_id.
POST /:protocol/:host/:catalog_id
  With a JSON body in format of {"path":path} where path is the path to the
  file resource (if protocol://host/path/to/resource is an example URL,
    /path/to/resource would be the path in the above example).
  * {"status":"ok"} means the given URL/URI has been stored.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 FIXME

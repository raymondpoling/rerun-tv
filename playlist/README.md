# playlist-playlist

Represents a list of catalog items that are used to find media items. A playlist
has the following guaranteed features:
- Strictly ordered
- Finite length
- Named

## Exposed Interface

- GET /
  Returns a list of all playlist names in the database, and how many items are
  in the playlist.
  * {"status": "ok", "playlists":[{"name":"playlist1","length":27},...]
  respresents a successful response and all items.
- POST /:name
  With a JSON body in the format of {"playlist":["item1","item2",...]}
  will create a new playlist with the given :name, and the listed items (catalog
  ids).
  * {"status":"ok"} means it was created successfully.
  * {"status":"invalid"} with a 412 status code means the operation was unsuccessful,
  such as the playlist name already being in use.
- PUT /:name
  With a JSON body in the format of {"playlist":["item1","item2",...]} will
  replace the playlist with name :name with a different playlist. The operation
  is treated as an overwrite operation for simplicity.
  * {"status":"ok"} means that the operation completed successfully.
  * {"status":"invalid"} means that the operation failed, such as the named list
  does not exist.
- DELETE /:name
  Deletes the playlist with name :name.
  * {"status":"ok"} means the operation completed successfully.
- GET /:name/:index
  Returns the playlist item from the named playlist at the given index.
  * {"status":"ok", "item":"item1"} is the catalog id of the returned item.
  * {"status":"not-found"} If the playlist is not found.
- GET /:name
  Returns the given playlist in schedule ready format.
  * {"status":"ok", "playist":{"name"::name, "length": :len}} where :name is the
  name of the playlist from the request, and :len is the number of items in the
  playlist.
  * {"status":"not-found"} If the playlist is not found.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 FIXME

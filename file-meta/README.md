# file-meta

Used for storing and retrieving meta data about data, including creating catalog
ids.

## Exposed Interface

- POST /series/:name/:season/:episode
  Creates a reference to a new episode, for series :name, with season and episodes
  of :season and :episode.
  * {"status":"ok","catalog_ids":["catalog_id"]} where catalog_id is the new
  catalog id for the new episode inserted.
  * {"status":"failure"} with status code 500 means some error occurred and
  the new episode was not inserted.
- PUT /series/:name/:season/:episode
  With a JSON body with the following fields:
  * summary A short text summary of the episode.
  * episode_name The text name of the episode.
  Results:
  * {"status":"ok","catalog_ids":["catalog_id"]} where catalog_id is the id of
  the updated record.
  * {"status":"failure"} with status code 500 means some error occurred and
  the episode was not updated.
- PUT /series/:name
  Like PUT above, but used with bulk uploading. Includes a list of records with
  season and episode numbers, allowing for updating multiple episodes in a single
  series.
  * {"status":"ok","catalog_ids":[catalog_ids]} Records for all returned catalog_ids
  were updated correctly
  * {"status":"failure"} with status code 500 means there was an error updating
  the requested records.
- GET /series/:name
  Will return all members of the series. Takes a param, fields, which is a comma
  separated list of the fields that the client is interested in.
  * {"status":"ok","catalog_ids":["catalog_id1",...],
  "records":[{"season":1,"episode":1,"episode_name":"ep_name","summary":"text",
  "series":"series_name"},...]} Successful result with all the records from series
  with select fields.
  * {"status":"not_found"} with 404 status code means that the series does not exist.
- GET /series/:name/:season/:episode
  Get the records from the series named :name with season and episode number :season
  and :episode.
  * Same resules as for GET /series/:name
- GET /series/:name/:season/:episode
  Take the episode corresponding to season number :season and episode number
  :episode from series named :name, and removed it.
  * {"status":"ok"} means the operation completed successfully.
- GET /catalog-id/:catalog_id
  Get an episode by the :catalog_id, which uniquely identifies every episode.
  * {"status":"ok","catalog_ids":[catalog_id],"records":[record]} Is the returned
  record. The record is in the same format as listed above.
  * {"status":"not-found"} with status code 404 means that the catalog id does not
  exist.
- GET /series
  Returns a list of all available series.
  * {"status":"ok","results":["series1","series2",...]} with series1 and series2
  are series names of available series.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 FIXME

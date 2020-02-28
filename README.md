# user API

This API is used for keeping track of user meta data, such as the schedule the
user has used, and their most recent index. Other features like roles may be
added later, but authentication is NOT part of this API.

## Exposed Interface

- POST /:user
  Creates a new user with name :user, and returns {"status":"ok"} if it completes
  correctly, but returns {"status":"failed"} if the user cannot be created.
- DELETE /:user
  Deletes a user with name :user, and all related metadata. Returns {"status":"ok"}
  if it completes correctly, and {"status":"failed"} if otherwise.
- GET /:user/:schedule
  Returns the current index for the given schedule, and automatically advances
  the index. Format of the result is {"idx":integer} for a successful result,
  and {"status":"not found"} on error, though if the schedule was never viewed
  it will automatically start from index 0.
- PUT /:user/:schedule/:index
  Allows for setting the current index, overwriting the stored value. Returned
  values are {"status":"ok"} on success, and {"status":"failed"} on failure.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

You will also need a configured MySQL server with the user.sql file imported.

## Building

lein do clean, compile :all, uberjar
docker build .

## Running

While ring is used to build this application, http-kit's web server is used to
run the application. It is best to customize the docker file with your own
settings.

## License

Copyright © 2020 FIXME

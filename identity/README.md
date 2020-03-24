# identity

Provides basic identity and authorization services. Must keep even with OAuth2.

## Exposed Interface

- POST /user/:user
  Takes a JSON in the format of {"email":email, "role":role} where role is a
  system defined role, and email is the users email address.
  * {"status":"ok"} user was created.
  * {"status":"failed", "message":msg} with status code 400 means the user could not
  be created for the reason given in msg.
- PUT /user/:user
  Takes a JSON in the format of {"role":role} where role is a system defined
  role.
  * {"status":"ok"} user role changed.
  * {"status":"failed", "message":msg} with status code 400 means that provided
  msg, most likely role does not exist, occurred.
- GET /user/:user
  Returns the user identity and role for authorization.
  * {"status":"ok","user":user,"role":role,"email":email} is the information about
  the user.
  * {"status":"not found"} with a 404 means the user is not found.
  * {"status":"failed","message":msg} with a 400 code means an error occurred.
- POST /role/:role
  Define a new role :role.
  * {"status":"ok"} the was created.
  - {"status":"failed", "message":msg} msg will provide the reason why the role
  was not added, most likely because the role already exists.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

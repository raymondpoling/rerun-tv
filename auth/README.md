# auth

Provides basic authentication services. Prefer to replace with OAuth at some point.

## Exposed Interface

- POST /new/:user
  Takes a JSON in the format of {"password":pass} where pass is a password.
  * {"status":"ok"} user was created.
  * {"status":"could-not-create"} with status code 400 means the user could not
  be created.
- POST /validate/:user
  Takes a JSON in the format of {"password":pass} where pass is a password.
  * {"status":"ok"} user provided valid password.
  * {"status":"invalid-credentials"} with status code 400 means that the user
  provided invalid credentials.
- POST /update/:user
  Takes a JSON in the format of {"new-password":pass1,"old-password":pass2} where
  pass2 is the original password and pass1 is the password to be changed to.
  * {"status":"ok"} the password was updated.
  - {"status":"invalid-credentials"} old-password was incorrect, so the password
  could not be updated.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 Raymond Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

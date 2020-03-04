# ReRun TV

Series of small HTTP services that work together to produce playlists based off
TV schedule like tables. Because while it can be fun to binge a new show, older
shows are more fun when there's that bit of a surprise when the an episode you
like shows up.

## Design Goals and Design Choices

There are three principle design goals to this project:

1. Many shows are sequential rather than episodic, so episodes should play in
order.
2. If there are at least two series in a schedule, then the same show should
not play twice in a row (unless that is exactly what is wanted).
3. Playlists should be generated forever. That is to say, when one series ends,
it should start over again from the beginning, in perpetuity.

To achieve these goals, it was decided to lean towards functional. If some
f(schedule,idx) -> playlist, where idx is a simple index, and schedule is a
structure defining how to derive a playlist, then a properly defined schedule
structure will have all the desired traits.

One of the advantages of using this approach is repeatability. It is always
possible to recreate a playlist for a given index with little to no effort. This
means if a playlist needs to be recreated, such as changing the device it will
be viewed from, it can be done with little effort.

The choice to use Clojure is just that personal experience suggests it has less
boilerplate and ancillary code associated with it. Or, to put it in another way,
I spend less time working with design patterns, frameworks, meta-coding with
annotations, or mucking around with complex object graphs with IoC. Instead, most
of my time feels spent on solving the domain problem. I did consider using Scala
with Spray, but elected against it.

JSON is the main IPC language of choice, for the following:
1. Simple
2. No need for numerical accuracy beyond integer
3. Most of the meaningful information is character in nature
4. Binary is generally painful to debug interactively
5. Works well with HTTP, both in request parameters and result bodies

Playlist items are represented between services with a catalog id. This id is
meant to be human readable but uniquely identify a particular episode of a
series.

## HTTP Servers

### AUTH

Simplistic MySQL backed password authentication server. To be replaced with
better authentication.

### FILE-LOCATOR

Service for producing URLs based on protocol, the host to be used, and catalog id.

### FILE-META

Service for storing episode information, and creating catalog ids.

### FORMAT

Service for taking a user and schedule, and using other services, producing an
m3u playlist.

### PLAYLIST

Service for storing named playlists. A playlist could be a TV series, or just a
bunch of episodes with a related theme, or any other criteria desired. These are
used for schedules, and are NOT the output playlists of the system.

### SCHEDULE

Service for storing schedules (a structure of playlists) for producing the m3u
playlists. Also takes an index for producing the output playlist.

### SCHEDULE-BUILDER

Service intended to provide support for creating and storing schedules. Uses
other services.

### USER

Service intended to store user information. Currently just stores the user's
current index in a given schedule.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

For individual services:

To run tests:
```
rm *.log
lein test
```

To build:
```
lein do clean, compile :all, uberjar
docker build .
```
## Running

See service for more information.

## License

Copyright © 2020 Raymond M. Poling
Released under the MIT License: [http://www.opensource.org/licenses/mit-license.php](http://www.opensource.org/licenses/mit-license.php)

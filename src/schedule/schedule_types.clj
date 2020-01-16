(ns schedule.schedule-types)

(defrecord Playlist [playlist length]])

(defrecord Merge [playlists])

(defrecord Complex [playlists])

(defrecord Multi [playlists])

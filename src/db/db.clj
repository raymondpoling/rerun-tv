(ns db.db
  (:require [clojure.java.jdbc :as j]))

(def database (atom {:dbtype "mysql"
               :dbname "playlist"
               :user nil
               :password nil}))

               (defn initialize
                 ([name password]
                   (swap! database merge {:user name :password password}))
                 ([name password host port]
                   (swap! database merge {:user name :password password :host host :port port})))

(defn insert-series [name]
  (j/insert! @database "playlist.name" {:name name}))

(defn delete-series [name]
  (j/delete! @database "playlist.name" ["name = ?" name]))

(defn name-key [name]
  (let [n (j/query @database
            ["SELECT id FROM playlist.name WHERE name = ?" name])]
    (-> n
      first
      :id)))

(defn insert-playlist [name playlist]
  (let [name_key (name-key name)
        to-insert (map vector (range (count playlist)) playlist)]
    (j/insert-multi! @database "playlist.playlist"
      (map (fn [n]
        {:name_key name_key
          :idx (first n)
          :object (second n)})
        to-insert))))

(defn replace-playlist [name playlist]
  (let [name_key (name-key name)]
    (j/delete! @database "playlist.playlist" ["name_key = ?" name_key]))
  (insert-playlist name playlist))

(defn find-item [name index]
  (let [out (j/query @database
    ["SELECT object FROM playlist.playlist WHERE idx = ? AND name_key = (SELECT id FROM playlist.name WHERE name = ?)" index name]
    )]
    (:object (first out))))

(defn get-all-playlists []
  (j/query @database
    ["SELECT name.name AS name, count(name.name) AS length FROM name
      JOIN playlist WHERE name.id = playlist.name_key
      GROUP BY name.name"]))

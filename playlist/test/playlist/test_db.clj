(ns playlist.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]
    [cheshire.core :refer :all]
    [cheshire.generate :refer [add-encoder encode-str remove-encoder]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA PLAYLIST"])
  (j/execute! @database (j/create-table-ddl "playlist.name"
    [[:id :SERIAL]
    [:name "VARCHAR(50)"]
    [:posted :DATETIME]
    ["PRIMARY KEY (id)"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_names ON playlist.name(name)"])
  (j/execute! @database (j/create-table-ddl "playlist.playlist"
    [[:id :SERIAL]
     [:name_key "BIGINT(20) UNSIGNED NOT NULL"]
     [:idx :int "UNSIGNED"]
     [:object "VARCHAR(256)"]
     ["PRIMARY KEY(id)"]
     ["FOREIGN KEY(name_key) REFERENCES playlist.name(id) ON DELETE CASCADE"]]))
   (j/execute! @database ["CREATE UNIQUE INDEX find_item ON playlist.playlist(name_key,idx)"]))

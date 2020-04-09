(ns file-meta.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]
    [cheshire.core :refer :all]
    [cheshire.generate :refer [add-encoder encode-str remove-encoder]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA META"])
  (j/execute! @database (j/create-table-ddl "meta.series"
    [[:id :SERIAL]
    [:name "VARCHAR(50)"]
    [:catalog_prefix "CHAR(7) UNIQUE NOT NULL"]
    [:summary "VARCHAR(100)"]
    [:thumbnail "VARCHAR(255)"]
    [:imdbid "CHAR(10)"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_series_name ON meta.series(name)"])
  (j/execute! @database ["CREATE UNIQUE INDEX by_catalog_prefix ON meta.series(catalog_prefix)"])
  (j/execute! @database (j/create-table-ddl "meta.season_title"
    [[:id "IDENTITY"]
     [:series_id :int "NOT NULL"]
     [:season :int "NOT NULL"]
     [:title "VARCHAR(50) NOT NULL"]
     ["FOREIGN KEY (series_id) REFERENCES meta.series(id) ON DELETE CASCADE"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX subtitles ON meta.season_title(series_id, season)"])
  (j/execute! @database (j/create-table-ddl "meta.files"
    [[:id "IDENTITY"]
     [:series_id :int "NOT NULL"]
     [:season :int "NOT NULL"]
     [:episode :int "NOT NULL"]
     [:episode_name "VARCHAR(120)"]
     [:summary "VARCHAR(100)"]
     [:thumbnail "VARCHAR(255)"]
     [:imdbid "CHAR(10)"]
     ["FOREIGN KEY (series_id) REFERENCES meta.series(id) ON DELETE CASCADE"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_series_season_episode ON meta.files(series_id,season,episode)"]))

(add-encoder org.h2.jdbc.JdbcClob
  (fn [c jsonGenerator]
    (let [str (slurp (.getCharacterStream c))]
      (.writeString jsonGenerator str))))

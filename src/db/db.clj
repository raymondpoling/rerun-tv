(ns db.db
  (:require
    [catalog.id :refer :all]
    [clojure.java.jdbc :as j]
    [cheshire.core :refer :all]))

(def database (atom {:dbtype "mysql"
               :dbname "meta"
               :user nil
               :password nil}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "hsql" :dbname "meta"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))

(defn find-series [series-name]
  (first (j/query @database ["SELECT id,catalog_prefix FROM meta.series WHERE name = ?" series-name])))

(defn insert-series [series-name]
  (j/with-db-transaction [db @database]
    (let [catalog-prefix (create-id series-name)
      previous-series (j/query db ["SELECT catalog_prefix FROM meta.series WHERE catalog_prefix LIKE ?" (str catalog-prefix "%")])
      catalog-id (if (empty? previous-series) (clojure.string/join [catalog-prefix "01"]) (next-id (:catalog_prefix (first previous-series))))]
    (merge {:catalog_prefix catalog-id}
      (first (j/insert! db "meta.series"
        {:name series-name :catalog_prefix catalog-id}))))))

(defn update-record [series_key season episode record]
  (j/update! @database "meta.files"
    record ["files.series_id = ?
            AND files.season = ?
            AND files.episode = ?" series_key season episode]))

(defn insert-record [series_key record]
  (j/insert! @database "meta.files"
    (merge {:series_id series_key} record)))

(defn find-by-catalog_id [catalog-id]
  (let [catalog-prefix (subs catalog-id 0 7)
        season (Integer/parseInt (subs catalog-id 7 9))
        episode (Integer/parseInt (subs catalog-id 9 12))]
  (j/query @database ["SELECT series.name AS series, episode, season, summary, episode_name, catalog_prefix FROM meta.series JOIN meta.files
    ON series.id = files.series_id
    WHERE series.catalog_prefix = ?
    AND files.season = ?
    AND files.episode = ?" catalog-prefix season episode])))

(defn find-by-series-season-episode [name season episode]
  (j/query @database ["SELECT series.name AS series, episode, season, summary, episode_name, catalog_prefix FROM meta.series JOIN meta.files
    ON series.id = files.series_id
    WHERE series.name = ?
    AND files.season = ?
    AND files.episode = ?" name season episode]))

(defn get-catalog-id [series-name season episode]
  (let [prefix (first (j/query @database ["SELECT series.catalog_prefix
        FROM meta.series series.name = ?" series-name]))]
    (format "%s%02d%03d" prefix season episode)))

(defn delete-record [series-name season episode]
  (let [id (find-series series-name)]
    (j/delete! @database "meta.files" ["series_id = ? AND season = ? AND episode = ?" (:id id) season episode])
    (:catalog_prefix id)))

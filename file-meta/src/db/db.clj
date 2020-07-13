(ns db.db
  (:require
   [catalog.id :refer [create-id next-id]]
   [clojure.java.jdbc :as j]
   [clojure.string :as cls]))

(def database (atom {:dbtype "mysql"
                     :dbname "meta"
                     :user nil
                     :password nil
                     :serverTimezone "America/New_York"}))

(defn initialize
  ([]
   (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "meta"}))
  ([name password host port]
   (swap! database merge {:user name
                          :password password
                          :host host
                          :port port})))

(defn find-series [series-name]
  (first (j/query @database
                  [(str "SELECT id, "
                        "catalog_prefix "
                        "FROM meta.series "
                        "WHERE name = ?") series-name])))

(defn insert-series [series-name]
  (j/with-db-transaction [db @database]
    (let [catalog-prefix (create-id series-name)
          previous-series
          (j/query db [(str "SELECT catalog_prefix "
                            "FROM meta.series "
                            "WHERE catalog_prefix LIKE ? "
                            "ORDER BY catalog_prefix DESC")
                            (str catalog-prefix "%")])
          catalog-id (if (empty? previous-series)
                       (cls/join [catalog-prefix "01"])
                       (next-id (:catalog_prefix (first previous-series))))]
      (merge {:catalog_prefix catalog-id}
             (first (j/insert! db "meta.series"
                               {:name series-name
                                :catalog_prefix catalog-id}))))))

(defn update-record [series_key season episode record]
  (j/update! @database "meta.files"
             record ["files.series_id = ?
            AND files.season = ?
            AND files.episode = ?" series_key season episode]))

(defn update-series [series record]
  (j/update! @database "meta.series"
             record ["series.name = ?" series]))

(defn insert-record [series_key record]
  (j/insert! @database "meta.files"
             (merge {:series_id series_key} record)))

(defn find-by-catalog_id [catalog-id]
  (let [catalog-prefix (subs catalog-id 0 7)
        season (Integer/parseInt (subs catalog-id 7 9))
        episode (Integer/parseInt (subs catalog-id 9 12))]
    (j/query
     @database
     [
      (str "SELECT series.name AS series, "
           "episode, "
           "season, "
           "files.summary AS summary, "
           "episode_name, "
           "catalog_prefix, "
           "files.imdbid AS imdbid, "
           "CASE "
           "WHEN files.thumbnail is NULL OR files.thumbnail = '' "
           "THEN series.thumbnail "
           "ELSE files.thumbnail END AS thumbnail "
           "FROM meta.series JOIN meta.files "
           "ON series.id = files.series_id "
           "WHERE series.catalog_prefix = ? "
           "AND files.season = ? "
           "AND files.episode = ?") catalog-prefix season episode])))

(defn find-by-series-season-episode [name season episode]
  (j/query
   @database
   [
    (str "SELECT series.name AS series, "
         "episode, "
         "season, "
         "files.summary AS summary, "
         "episode_name, "
         "catalog_prefix, "
         "files.imdbid AS imdbid, "
         "CASE WHEN files.thumbnail is NULL OR files.thumbnail = '' "
         "THEN series.thumbnail "
         "ELSE files.thumbnail END AS thumbnail "
         "FROM meta.series JOIN meta.files "
         "ON series.id = files.series_id "
         "WHERE series.name = ? "
         "AND files.season = ? "
         "AND files.episode = ?") name season episode]))

(defn get-catalog-id [series-name season episode]
  (let [prefix (first (j/query @database ["SELECT series.catalog_prefix
        FROM meta.series series.name = ?" series-name]))]
    (format "%s%02d%03d" prefix season episode)))

(defn delete-record [series-name season episode]
  (let [id (find-series series-name)]
    (j/delete!
     @database "meta.files"
     ["series_id = ? AND season = ? AND episode = ?" (:id id) season episode])
    (:catalog_prefix id)))

(defn find-by-series [series-name]
  [(j/query @database [(str "select series.summary AS summary, "
                            "series.imdbid AS imdbid, "
                            "series.thumbnail AS thumbnail, "
                            "series.catalog_prefix AS catalog_id "
                            "FROM meta.series WHERE name = ?") series-name])
   (j/query @database
            [(str "SELECT catalog_prefix, "
                  "season, "
                  "episode "
                  "FROM meta.series "
                  "JOIN meta.files "
                  "ON series.id = files.series_id "
                  "WHERE series.name = ? "
                  "GROUP BY catalog_prefix, season, episode "
                  "ORDER BY catalog_prefix, season, episode")
             series-name])])

(defn find-all-series []
  (j/query @database
           ["SELECT name FROM meta.series GROUP BY name ORDER BY name"]))

(defn get-summary []
  (j/query @database
           [(str "SELECT count(DISTINCT name) AS series, "
                 "count(DISTINCT CONCAT(name,':',season)) AS seasons, "
                 "count(*) AS episodes "
                 "FROM meta.series "
                 "JOIN meta.files "
                 "ON series.id = files.series_id")]))

(defn- find-by-catalog-prefix [catalog-prefix]
  (:id
   (first
    (j/query
     @database [(str "SELECT id FROM meta.series WHERE catalog_prefix = ?")
                catalog-prefix]))))

(defn- delete-series [catalog-id]
  (j/delete!
   @database "meta.series"
   ["catalog_prefix = ?" catalog-id])
  catalog-id)

(defn- delete-season [catalog-id season]
  (let [id (find-by-catalog-prefix catalog-id)]
    (j/delete!
     @database "meta.files"
     ["series_id = ? AND season = ?" id (Integer/parseInt season)])
    (str catalog-id season)))

(defn- delete-episode [catalog-id season episode]
  (let [id (find-by-catalog-prefix catalog-id)]
    (j/delete!
     @database "meta.files"
     ["series_id = ? AND season = ? AND episode = ?"
      id
      (Integer/parseInt season)
      (Integer/parseInt episode)])
    (str catalog-id season episode)))

(defn delete-by-catalog-id [catalog-id]
  (let [pat #"(\w{7})(\w{2})?(\w{3})?"
        [_ series season episode] (re-matches pat catalog-id)]
    (cond
      (and series season episode) (delete-episode series season episode)
      (and series season) (delete-season series season)
      (and series) (delete-series series)
      :else (throw (Exception. (str "Bad catalog-id " catalog-id))))))

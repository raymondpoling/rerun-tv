(ns omdb-meta.update
  (:require
    [remote-call.omdb :refer :all]
    [clojure.tools.logging :as logger]
    [remote-call.meta :as meta]))

(defn- need-update? [record in-map]
  (not (every? some? (map #(% record) (map first in-map)))))

(defn- replace-on-nil [[k1 k2 d] r1 r2]
  (if (nil? (k1 r1))
    (if (nil? (k2 r2))
      (assoc r1 k1 d)
      (assoc r1 k1 (k2 r2)))
    r1))

(defn- solve-each [r1 r2 function-list]
  (if (empty? function-list)
    r1
    (recur ((first function-list) r1 r2) r2 (rest function-list))))

(defn- if-nil-update [meta omdb key-map]
  (solve-each meta omdb (map #(partial replace-on-nil %) key-map)))

(def series-default-map
  [[:summary :Plot "Not in omdb"]
  [:thumbnail :Poster ""]
  [:imdbid :imdbID ""]])

(defn update-series [series-record series-name omdb-host apikey]
  (if (need-update? series-record series-default-map)
    (let [omdb-record (lookup-series omdb-host apikey series-name)
          updated-record (if-nil-update series-record omdb-record series-default-map)]
          updated-record)
    series-record))

(def episode-default-map
  [[:summary :Plot "Not in omdb"]
    [:thumbnail :Poster ""]
    [:episode_name :Title "Not in omdb"]
    [:imdbid :imdbID ""]])

(defn update-episode [record series omdb-host apikey]
  (if (need-update? record episode-default-map)
    (let [season (:season record)
          episode (:episode record)
          omdb-record (lookup-episode omdb-host apikey series season episode)
          updated-record (if-nil-update record omdb-record episode-default-map)]
          (logger/debug "Updated record: " updated-record)
          updated-record)
      (do
        (logger/debug "No update needed: " record)
        record)))

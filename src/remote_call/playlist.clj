(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [clj-http.client :as client]
            [cheshire.core :refer :all]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
              :delay-ms 1000})

(defn get-playlists [host]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (let [x (parse-stream (clojure.java.io/reader (:body (client/get (str "http://" host "/") {:as :stream}))) true)]
        x))
  (catch Exception e
    (println "oops, caught an error: ")
    (println e)
      {:status :failure :message "playlist service not available"})))

(defn get-playlists-map [host]
  (let [playlists (get-playlists host)]
    (println "playlists: " playlists)
    (into {} (map (fn [t] [(:name t) (:length t)]) playlists))))

(defn get-playlist [host playlist-name]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (client/get (str "http://" host "/" playlist-name)))
  (catch Exception e {:status :failure :message "playlist service not available"})))

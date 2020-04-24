(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [clj-http.client :as client]
            [remote-call.util :refer [not-exceptional log-on-error]]
            [clojure.tools.logging :as logging]
            [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as cji]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
              :delay-ms 1000})

(defn- make-url
  ([host] (format "http://%s" host))
  ([host playlist] (format "http://%s/%s" host playlist)))

(defn get-playlists [host]
  (log-on-error {:status :failure :message "playlist service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (let [x (parse-stream
                (cji/reader
                  (:body
                    (client/get (make-url host)
                                {:as :stream
                                 :unexceptional-status (not-exceptional)})))
                true)]
        x))))

(defn get-playlists-map [host]
  (let [playlists (get-playlists host)]
    (logging/debug "playlists: " playlists)
    (if (= (:status playlists) :failure)
      playlists
      (into {} (map
                (fn [t] [(:name t) (:length t)])
                (:playlists playlists))))))


(defn get-playlist [host playlist-name]
  (log-on-error {:status :failure :message "playlist service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (make-url host playlist-name)
              {:unexceptional-status (not-exceptional)})))))

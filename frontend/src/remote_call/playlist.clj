(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-playlists [host]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:playlists (:body (client/get (str "http://" host "/") {:as :json}))))))

(defn fetch-catalog-id [host playlist idx]
  (clc/log-on-error {:status "failed", :message "could not find catalog id"}
    (dh/with-circuit-breaker ckt-brkr
      (let [url (str "http://" host "/" playlist "/" idx)]
        (logger/debug "Looking up url: " url)
        (:body (client/get url {:as :json}))))))

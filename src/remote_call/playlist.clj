(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-playlists [host]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:playlists (:body (client/get (str "http://" host "/") {:as :json}))))))

(defn fetch-current-schedule [host user schedule]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (client/get "http"))))

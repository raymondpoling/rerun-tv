(ns remote-call.format
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-playlist [server user schedule params]
  (clc/log-on-error
   {"status" "failed" "message" "format service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (client/get (str "http://" server "/" user "/" schedule)
                 {:query-params
                  (into {} (filter second params))}))))

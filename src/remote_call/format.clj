(ns remote-call.format
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-playlist [host user schedule params]
  (clc/log-on-error
   {"status" "failed" "message" "format service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (client/get (str "http://" host "/" user "/" schedule)
                 {:query-params (into {} (filter second params))}))))

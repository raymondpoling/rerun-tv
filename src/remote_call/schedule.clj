(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-schedule [host schedule-name index]
  (try
      (dh/with-circuit-breaker ckt-brkr
        (:body (client/get (str "http://" host "/" schedule-name "/" index) {:as :json})))
      (catch Exception e
        [])))

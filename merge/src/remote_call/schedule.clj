(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-schedule [host schedule-name index]
  (clc/log-on-error nil
      (dh/with-circuit-breaker ckt-brkr
        (:items (:body (client/get (str "http://" host "/" schedule-name "/" index) {:as :json}))))))

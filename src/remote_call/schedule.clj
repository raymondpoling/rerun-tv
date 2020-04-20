(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-schedules [host]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:schedules (:body (client/get (str "http://" host "/") {:as :json}))))))

(defn get-schedule [host schedule-name]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:schedule (:body (client/get (str "http://" host "/" schedule-name)
                                    {:as :json}))))))

(defn get-schedule-items [host schedule-name idx]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:items (:body (client/get (str "http://" host "/" schedule-name "/" idx)
                                 {:as :json}))))))

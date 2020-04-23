(ns remote-call.schedule-builder
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn validate-schedule [host schedule]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/schedule/validate")
                {:as :json
                  :body schedule
                  :headers {:content-type "application/json"}})))))

(defn send-schedule [host mode name schedule]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (case (keyword mode)
        :Update (:body (client/put (str "http://" host "/schedule/store/" name)
                    {:as :json
                      :body schedule
                      :headers {:content-type "application/json"}}))
        :Create (:body (client/post (str "http://" host "/schedule/store/" name)
                    {:as :json
                      :body schedule
                      :headers {:content-type "application/json"}}))))))

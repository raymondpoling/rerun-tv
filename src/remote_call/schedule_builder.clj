(ns remote-call.schedule-builder
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn validate-schedule [host schedule]
  (try
    (let [_ (parse-string schedule)]
      (clc/log-on-error nil
        (dh/with-circuit-breaker ckt-brkr
          (:body (client/get (str "http://" host "/schedule/validate")
                    {:as :json
                      :body schedule
                      :headers {:content-type "application/json"}})))))
    (catch Exception e {:status "invalid" :messages [(.getMessage e)]})))

(defn send-schedule [host type name schedule]
  (try
    (let [_ (parse-string schedule)]
      (clc/log-on-error nil
        (dh/with-circuit-breaker ckt-brkr
          (case (keyword type)
            :Update (:body (client/put (str "http://" host "/schedule/store/" name)
                        {:as :json
                          :body schedule
                          :headers {:content-type "application/json"}}))
            :Create (:body (client/post (str "http://" host "/schedule/store/" name)
                        {:as :json
                          :body schedule
                          :headers {:content-type "application/json"}}))))))
    (catch Exception e {:status "invalid" :messages [(.getMessage e)]})))

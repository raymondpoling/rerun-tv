(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                            :delay-ms 1000})

(defn get-schedule [host schedule-name]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/" schedule-name) {:as :json})))
  (catch Exception e
    (println e)
    {:status :failure :message "schedule service not available"})))

(defn post-schedule [host schedule-name schedule]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (client/post (str "http://" host "/" schedule-name) {:body (generate-string schedule) :content-type :json :headers {:content-type "application/json"}}))
  (catch Exception e {:status :failure :message "schedule service not available"})))

(defn put-schedule [host schedule-name schedule]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (println "schedule is this: " schedule)
      (client/put (str "http://" host "/" schedule-name) {:body (generate-string schedule) :content-type :json :headers {:content-type "application/json"}}))
  (catch Exception e
    (println "ERROR***** " e)
    {:status :failure :message "schedule service not available"})))

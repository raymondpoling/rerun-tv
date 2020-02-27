(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as logging]
            [clj-http.client :as client]
            [remote-call.util :refer [log-on-error]]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                            :delay-ms 1000})

(defn get-schedule [host schedule-name]
  (log-on-error {:status :failure :message "schedule service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/" schedule-name) {:as :json})))))

(defn post-schedule [host schedule-name schedule]
  (log-on-error (do
    {:status :failure :message "schedule service not available"})

    (dh/with-circuit-breaker ckt-brkr
      (client/post (str "http://" host "/" schedule-name)
        {:body (generate-string schedule)
          :content-type :json
          :headers {:content-type "application/json"}}))))


(defn put-schedule [host schedule-name schedule]
  (log-on-error {:status :failure :message "schedule service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (logging/debug "schedule is this: " schedule)
      (client/put (str "http://" host "/" schedule-name)
        {:body (generate-string schedule)
          :content-type :json
          :headers {:content-type "application/json"}}))))

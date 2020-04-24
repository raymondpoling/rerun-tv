(ns remote-call.schedule
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [clojure.tools.logging :as logging]
            [clj-http.client :as client]
            [remote-call.util :refer [log-on-error]]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                                :delay-ms 1000})

(defn- make-url [host schedule-name]
  (format "http://%s/%s" host schedule-name))

(defn get-schedule [host schedule-name]
  (log-on-error
   {:status :failure :message "schedule service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (:schedule (:body
                 (client/get (make-url host schedule-name)
                             {:as :json}))))))

(defn post-schedule [host schedule-name schedule]
  (log-on-error
   {:status :failure :message "schedule service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (client/post (make-url host schedule-name)
                  {:body (generate-string schedule)
                   :content-type :json
                   :headers {:content-type "application/json"}}))))


(defn put-schedule [host schedule-name schedule]
  (log-on-error
   {:status :failure :message "schedule service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (logging/debug "schedule is this: " schedule)
     (client/put (make-url host schedule-name)
                 {:body (generate-string schedule)
                  :content-type :json
                  :headers {:content-type "application/json"}}))))

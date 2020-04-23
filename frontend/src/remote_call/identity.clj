(ns remote-call.identity
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-user [host user]
  (clc/log-on-error {:status :failed :message "identity service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/user/" user) {:as :json})))))

(defn fetch-users [host]
  (clc/log-on-error {:status :failed :message "identity service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/user") {:as :json})))))

(defn fetch-roles [host]
  (clc/log-on-error {:status :failed :message "identity service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/role") {:as :json})))))

(defn user-update [host user role]
  (clc/log-on-error {:status :failed :message "identity service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/put (str "http://" host "/user/" user)
                {:as :json
                  :headers {:content-type "application/json"}
                  :body (generate-string {:role role})})))))

(defn create-user [host user email role]
  (clc/log-on-error {:status :failed :message "identity service not available"}
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/post (str "http://" host "/user/" user)
                {:as :json
                  :headers {:content-type "application/json"}
                  :body (generate-string {:role role :email email})})))))

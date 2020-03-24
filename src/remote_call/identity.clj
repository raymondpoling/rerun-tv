(ns remote-call.identity
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-user [host user]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/get (str "http://" host "/user/" user) {:as :json})))
  (catch Exception e
    (println e)
    {:status :failure :message "identity service not available"})))

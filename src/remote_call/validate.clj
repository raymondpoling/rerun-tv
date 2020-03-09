(ns remote-call.validate
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                            :delay-ms 1000})

(defn validate-user [host user password]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (:body (client/post (str "http://" host "/validate/" user)
              {:body (generate-string {:password password}) 
                :content-type :json
                :headers {:content-type "application/json"}
                :as :json})))
  (catch Exception e
    (println e)
    {:status :failure :message "schedule service not available"})))

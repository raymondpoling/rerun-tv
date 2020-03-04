(ns remote-call.user
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-index [host user schedule-name]
  (clc/log-on-error {:status "failure" :message "user service not available"}
      (dh/with-circuit-breaker ckt-brkr
        (:idx (parse-string (:body (client/get
                          (str "http://" host "/" user "/" schedule-name))
                          {:as :json}) true)))))

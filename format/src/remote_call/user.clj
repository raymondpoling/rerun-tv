(ns remote-call.user
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(def message {:status "failure" :message "user service not available"})

(defn get-index [host user schedule-name update]
  (clc/log-on-error message
      (dh/with-circuit-breaker ckt-brkr
        (:idx (:body (client/get
                          (str "http://" host "/" user "/" schedule-name)
                          (merge {:as :json}
                                 (when update
                                   {:update update}))))))))

(defn set-index [host user schedule-name idx]
  (clc/log-on-error message
    (dh/with-circuit-breaker ckt-brkr
      (client/put (str "http://" host "/" user "/" schedule-name "/" idx)))))

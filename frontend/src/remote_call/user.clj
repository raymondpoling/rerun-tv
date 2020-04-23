(ns remote-call.user
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-index [host user schedule preview?]
  (clc/log-on-error {:status :failed}
    (dh/with-circuit-breaker ckt-brkr
      (if preview?
        (:idx (:body
               (client/get
                (str "http://" host "/" user "/" schedule "?preview=true")
                {:as :json})))
        (:idx (:body
               (client/get
                (str "http://" host "/" user "/" schedule)
                {:as :json})))))))

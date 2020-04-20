(ns remote-call.meta
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-meta [host catalog-id]
  (clc/log-on-error nil
      (dh/with-circuit-breaker ckt-brkr
        (first
         (:records
          (:body
           (client/get (str "http://" host "/catalog-id/" catalog-id)
                       {:as :json})) true)))))

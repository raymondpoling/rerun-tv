(ns remote-call.meta
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-meta [host catalog-id fields]
  (clc/log-on-error nil
      (dh/with-circuit-breaker ckt-brkr
        (first (:records (parse-string (:body (client/get
          (str "http://" host "/catalog-id/" catalog-id "?fields=" (clojure.string/join "," fields))) {:as :json}) true))))))

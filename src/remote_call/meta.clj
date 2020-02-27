(ns remote-call.meta
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-meta [host catalog-id fields]
  (try
      (dh/with-circuit-breaker ckt-brkr
        (first (:records (parse-string (:body (client/get
          (str "http://" host "/catalog-id/" catalog-id "?fields=" (clojure.string/join "," fields))) {:as :json}) true))))
      (catch Exception e
        {:series nil :episode nil :season nil :title nil})))

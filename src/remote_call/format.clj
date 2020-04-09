(ns remote-call.format
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn fetch-playlist [host user schedule params]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (client/get (str "http://" host "/" user "/" schedule) {:query-params (into {} (filter second params))}))
  (catch Exception e
    (println e)
    {:status :failure :message "format service not available"})))

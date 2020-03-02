(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-catalog-id [host name index]
  (logger/debug (str "trying to find '" name "' '" index "'"))
  (clc/log-on-error nil
      (dh/with-circuit-breaker ckt-brkr
        (:item (parse-string (:body (client/get
          (str "http://" host "/" name "/" index))) true)))))

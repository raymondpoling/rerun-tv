(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [ring.util.codec :refer [url-encode]]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-catalog-id [host name index]
  (logger/debug (str "trying to find '" name "' '" index "'"))
  (clc/log-on-error nil
      (dh/with-circuit-breaker ckt-brkr
        (:item
         (:body
          (client/get
           (str "http://" host "/" (url-encode name) "/" index)
           {:as :json}))))))

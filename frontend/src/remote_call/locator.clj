(ns remote-call.locator
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-locations [host catalog-id]
  (clc/log-on-error
   {:status "failed" :message "locator service not available"}
   (let [resp (:body (client/get (str "http://" host "/catalog-id/" catalog-id)
                                 {:as :json}))]
     (println "RESP??? " resp)
     (:files resp))))

(defn save-locations [host catalog-id locations]
  (clc/log-on-error
   {:status "failed" :message "locator service not available"}
   (let [result (:body (client/put (str "http://" host "/catalog-id/" catalog-id)
                                   {:as :json
                                    :headers {:content-type "application/json"}
                                    :body (generate-string {:files locations})}))]
     (println (format "For catalog ID %s got %s" catalog-id (str result)))
     result)))

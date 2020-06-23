(ns remote-call.locator
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [clj-http.client :as client]
            [redis-cache.core :as cache]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-locations [host catalog-id]
  (clc/log-on-error
   {:status "failed" :message "locator service not available"}
   (let [resp (cache/redis-cache host (str "/catalog-id/" catalog-id))]
     (:files resp))))

(defn save-locations [host catalog-id locations]
  (clc/log-on-error
   {:status "failed" :message "locator service not available"}
   (let [result (:body (client/put
                        (format "http://%s/catalog-id/%s" host catalog-id)
                                   {:as :json
                                    :headers {:content-type "application/json"}
                                    :body (generate-string {:files locations})}))]
     (cache/evict host (str "/catalog-id/" catalog-id))
     result)))

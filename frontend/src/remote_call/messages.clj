(ns remote-call.messages
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-messages [host start]
  (clc/log-on-error {:status "failed"}
    (let [param-map (if start {:step 10 :start start} {:step 10})]
      (:body (client/get (str "http://" host "/") {:as :json :query-params param-map})))))

(defn add-message [host user title information]
  (clc/log-on-error {:status "failed"}
    (client/post (str "http://" host "/")
      {:as :json :form-params {:author user :title title :information information}})))

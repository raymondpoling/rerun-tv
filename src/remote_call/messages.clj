(ns remote-call.messages
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [java-time :as jt]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-messages [host]
  (clc/log-on-error {:status "failed"}
      (:body (client/get (str "http://" host "/") {:as :json :query-params {:step 10}}))))

(defn add-message [host user title information]
  (clc/log-on-error {:status "failed"}
    (client/post (str "http://" host "/")
      {:as :json :form-params {:author user :title title :information information}})))

(ns remote-call.locator
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-file-url [host catalog-id]
  (clc/log-on-error {:status "failure" :message "locator service not available"}
      (dh/with-circuit-breaker ckt-brkr
        (:url (parse-string (:body (client/get
          (str "http://" host "/file/CrystalBall/" catalog-id)) {:as :json}) true)))))

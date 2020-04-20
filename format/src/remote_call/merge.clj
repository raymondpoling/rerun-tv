(ns remote-call.merge
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-merge [server schedule user index host protocol]
  (clc/log-on-error
   {:status :failure :message "merge service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server "/" user "/" schedule "/" index)]
       (:body
        (client/get url (merge {:as :json}
                    (when (and host protocol)
                      {:query-params
                       {:host host
                        :protocol protocol}}))))))))

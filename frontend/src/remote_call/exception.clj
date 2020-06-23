(ns remote-call.exception
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                                :delay-ms 1000})

(defn get-all-tests [server]
  (let [url (str "http://" server "/test")]
    (println "Exception url: " url)
  (clc/log-on-error
   {"status" "failed" "message" "exception service not available"}
   (dh/with-circuit-breaker ckt-brkr
      (map :name (:results (:body (client/get url
                                              {:as :json}))))))))

(defn get-test-results [server test]
  (clc/log-on-error
   {"status" "failed" "message" "exception service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (:results (:body (client/get (str "http://" server "/result/" test)
                           {:as :json}))))))


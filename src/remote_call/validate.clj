(ns remote-call.validate
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [clj-http.client :as client]
            [clojure.tools.logging :as logger]
            [common-lib.core :as clc]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                            :delay-ms 1000})

(defn validate-user [host user password]
  (try
    (dh/with-circuit-breaker ckt-brkr
      (let [url (str "http://" host "/validate/" user)]
        (logger/debug "validation url is: " url)
        (:body (client/post
                url
                {:body (generate-string {:password password}) 
                 :content-type :json
                 :headers {:content-type "application/json"}
                 :as :json}))))
    (catch Exception e
      (logger/error e)
      {:status :failure :message "schedule service not available"})))

(defn create-auth [host user password]
  (clc/log-on-error
   {:status "failure" :message "auth service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (:body (client/post (str "http://" host "/new/" user)
                         {:body (generate-string {:password password})
                          :content-type :json
                          :headers {:content-type "application/json"}
                          :as :json})))))

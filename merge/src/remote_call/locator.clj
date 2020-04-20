(ns remote-call.locator
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-locations [host catalog-id]
  (let [url (str "http://" host "/catalog-id/" catalog-id)]
    (logger/debug (str "Looking for catalog-id: " catalog-id "\n\turl: " url))
    (clc/log-on-error
     nil
     (dh/with-circuit-breaker ckt-brkr
       (:files 
        (:body
         (client/get url {:as :json})) true)))))

(defn get-protocol-host [server protocol host catalog-id]
  (let [url (str "http://" server "/" protocol "/" host "/" catalog-id)]
    (logger/debug (str "Looking for catalog-id: " catalog-id "\n\turl: " url))
    (clc/log-on-error
     nil
     (dh/with-circuit-breaker ckt-brkr
       (:url 
        (:body
         (client/get url {:as :json})) true)))))
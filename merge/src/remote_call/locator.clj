(ns remote-call.locator
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]
            [redis-cache.core :as cache]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-locations [host catalog-id]
  (let [url (str "/catalog-id/" catalog-id)]
    (logger/debug (str "Looking for catalog-id: " catalog-id "\n\turl: " url))
    (clc/log-on-error
     nil
     (dh/with-circuit-breaker ckt-brkr
       (:files 
        (cache/redis-cache host url))))))

(defn get-protocol-host [server protocol host catalog-id]
  (let [url (str "http://" server "/" protocol "/" host "/" catalog-id)]
    (logger/debug (str "Looking for catalog-id: " catalog-id "\n\turl: " url))
    (clc/log-on-error
     nil
     (dh/with-circuit-breaker ckt-brkr
       (:url 
        (:body
         (client/get url {:as :json})) true)))))

(defn get-protocol-hosts [server]
  (let [url (format "http://%s/protocol-host" server)]
    (logger/debug (str "trying to get all: " url))
    (clc/log-on-error
     nil
     (dh/with-circuit-breaker ckt-brkr
       (:body (client/get url {:as :json}))))))

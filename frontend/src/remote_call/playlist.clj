(ns remote-call.playlist
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [ring.util.codec :refer [url-encode]]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]
            [cheshire.core :refer [generate-string]]
            [clojure.string :as cls]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-playlists [host]
  (clc/log-on-error nil
    (dh/with-circuit-breaker ckt-brkr
      (:playlists (:body (client/get (str "http://" host "/") {:as :json}))))))

(defn get-playlist [host name]
  (clc/log-on-error
   nil
   (dh/with-circuit-breaker ckt-brkr
     (:items (:body (client/get (str "http://" host "/" name) {:as :json}))))))

(defn fetch-catalog-id [host playlist idx]
  (clc/log-on-error {:status "failed", :message "could not find catalog id"}
    (dh/with-circuit-breaker ckt-brkr
      (let [url (str "http://" host "/" (url-encode playlist) "/" idx)]
        (logger/debug "Looking up url: " url)
        (:body (client/get url {:as :json}))))))

(defn post-playlist [host name items]
  (clc/log-on-error
   {:status "failed", :message "failed to save playlist"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" host "/" (url-encode name))
           item-list (cls/split items #"\n")]
       (logger/debug "Looking up url: " url)
       (:body (client/post url {:as :json
                                :headers {:content-type "application/json"}
                                :body  (generate-string
                                        {
                                        :playlist item-list})}))))))

(defn put-playlist [host name items]
  (clc/log-on-error
   {:status "failed", :message "failed to save playlist"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" host "/" (url-encode name))
           item-list (cls/split items #"\n")]
       (logger/debug "Looking up url: " url)
       (:body (client/put url {:as :json
                               :headers {:content-type "application/json"}
                               :body  (generate-string
                                       {:playlist item-list})}))))))

(ns remote-call.deletion
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]
            [cheshire.core :refer [generate-string]]
            [ring.util.codec :refer [url-encode]]
            [remote-call.meta :refer [get-meta-by-catalog-id]]
            [redis-cache.core :as cache]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                                :delay-ms 1000})

(defn nominate [server atype a-name user reason]
  (clc/log-on-error
   {:status "failed" :message "deletion service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server
                    "/nominate/" atype
                    "/" (url-encode a-name))]
       (:body (client/post
               url
               {:unexceptional-status #(or (= 200 %)
                                           (= 400 %))
                :as :json
                :content-type :json
                :body (generate-string {:user user :reason reason})}))))))

(defn get-nominations [server]
  (clc/log-on-error
   {:status "failed" :message "deletion service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server "/nominate")]
       (:body (client/get
               url
               {:as :json
                :content-type :json}))))))

(defn get-recent [server]
  (clc/log-on-error
   {:status "failed" :message "deletion service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server "/recent")]
       (:body (client/get
               url
               {:as :json
                :content-type :json}))))))

(defn execute [server meta-server atype a-name user reason]
  (clc/log-on-error
   {:status "failed" :message "deletion service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server
                    "/execute/" atype
                    "/" (url-encode a-name))
           series (:series (first (:records
                          (get-meta-by-catalog-id meta-server a-name))))
           response (:body (client/post
                            url
                            {:unexceptional-status #(or (= 200 %)
                                                        (= 400 %))
                             :as :json
                             :content-type :json
                             :body (generate-string {:reason reason
                                                     :user user})}))]
       (when (or (= atype "series")
                 (= atype "season")
                 (= atype "episode"))
         (cache/evict meta-server "/series")
         (cache/evict meta-server (str "/catalog-id/" a-name))
         (when series
           (cache/evict meta-server (str "/series/" (url-encode series)))))
       response))))

(defn reject [server atype a-name user reason]
  (clc/log-on-error
   {:status "failed" :message "deletion service not available"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" server "/reject/" atype "/" (url-encode a-name))]
       (:body (client/post
               url
               {:unexceptional-status #(or (= 200 %)
                                           (= 400 %))
                :as :json
                :content-type :json
                :body (generate-string {:reason reason :user user})}))))))

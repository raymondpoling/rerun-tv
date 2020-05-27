(ns remote-call.meta
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [ring.util.codec :refer [url-encode]]
            [clj-http.client :as client]
            [clojure.tools.logging :as logger]
            [redis-cache.core :as cache]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-all-series [host]
  (clc/log-on-error
   {:status "failed"}
   (let [t (cache/redis-cache host "/series")]
     (logger/debug "t is" t "with type" (type t))
     (:results t))))

(defn bulk-update-series [host series update]
  (clc/log-on-error
   {:status "failed"}
   (let [url (str "http://" host "/series/" (url-encode series))]
     (logger/debug "url: " url)
     (let [response (:body
                     (client/put url
                                 {:as :json
                                  :body (generate-string update)
                                  :headers {:content-type
                                            "application/json"}}))]
       (dorun (map #(cache/evict host (str "/catalog-id/" %))
                   (:catalog_ids response)))
       response))))

(defn bulk-create-series [host series create]
  (clc/log-on-error
   {:status "failed"}
   (let [url (str "http://" host "/series/" (url-encode series))
         result (:body
                 (client/post url
                              {:as :json
                               :body (generate-string create)
                               :headers {:content-type "application/json"}}))]
     (logger/debug "url: " url)
     (cache/evict host "/series")
     (cache/evict host "/series/" (url-encode series))
     result)))

(defn save-episode [host series episode]
  (clc/log-on-error
   {:status "failed"}
   (let [url (str "http://" host "/series/"
                  (url-encode series) "/"
                  (:season episode) "/"
                  (:episode episode))]
     (logger/debug "URL: " url)
     (:body
      (let [response
            (client/put url {:as :json
                             :body (generate-string episode)
                             :headers {:content-type "application/json"}})]
        (logger/debug "***********Evicting" host (:catalog_ids
                                                  (:body response)))
        (dorun (map #(cache/evict host
                                  (str "/catalog-id/" %))
                    (:catalog_ids (:body response))))
        response)))))

(defn get-meta-by-catalog-id [host id]
  (clc/log-on-error
   {:status "failed" :message "Could not find item in catalog"}
   (cache/redis-cache host (format "/catalog-id/%s" id))))

(defn get-meta-by-imdb-id [host id]
  (clc/log-on-error
   {:status "failed" :message "Could not find item in catalog"}
    (let [url (str "http://" host "/imdbid/" id)]
      (:body (client/get url {:as :json})))))

(defn get-series-by-imdb-id [host id]
  (clc/log-on-error 
   {:status "failed" :message "Could not find item in catalog"}
   (let [url (str "http://" host "/imdbid/series/" id)]
     (logger/debug "series imdb id url: " url)
     (:body (client/get url {:as :json})))))

(defn get-series-episodes [host series]
  (clc/log-on-error {:status "failed" :message "meta host not available"}
    (let [path (format "/series/%s" (url-encode series))]
      (logger/debug "meh " path)
      (cache/redis-cache host path))))

(defn get-summary [host]
  (clc/log-on-error
   {:status "failed" :message "meta host not available"}
   (let [path (format "http://%s/summary" host)]
     (logger/debug "Getting summary from " path)
     (:body (client/get path {:as :json})))))

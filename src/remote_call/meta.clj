(ns remote-call.meta
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn get-all-series [host]
  (clc/log-on-error {:status "failed"}
    (:results (:body (client/get (str "http://" host "/series") {:as :json})))))

(defn bulk-update-series [host series update]
  (clc/log-on-error
   {:status "failed"}
   (let [url (str "http://" host "/series/" series)]
     (println "url: " url)
     (:body
      (client/put url
                  {:as :json
                   :body (generate-string update)
                   :headers {:content-type "application/json"}})))))

(defn create-series [host series-name series]
  (clc/log-on-error
   {:status "failed" :message "omdb-meta service not available"}
   (let [url (str "http://" host "/series/" series-name)]
     (println "create series url: " url)
     (println "create series payload: " series)
     (:body
      (client/put url
                   {:as :json
                    :body (generate-string {:series series})
                    :headers {:content-type "application/json"}})))))

(defn create-episode [host series episode]
  (clc/log-on-error
   {:status "failed" :message "omdb-meta service not available"}
   (let [url (str "http://" host "/series/" series "/" (:season episode) "/" (:episode episode))]
     (println "url: " url)
     (:body
      (client/post url
                   {:as :json
                    :body (generate-string episode)
                    :headers {:content-type "application/json"}})))))

(defn save-episode [host series episode]
  (clc/log-on-error
   {:status "failed"}
   (let [url (str "http://" host "/series/"
                  series "/"
                  (:season episode) "/"
                  (:episode episode))]
     (println "URL: " url)
     (:body
      (client/put url {:as :json
                       :body (generate-string episode)
                       :headers {:content-type "application/json"}})))))

(defn get-meta-by-catalog-id [host id]
  (clc/log-on-error {:status "failed" :message "Could not find item in catalog"}
    (let [url (str "http://" host "/catalog-id/" id)]
      (:body (client/get url {:as :json})))))

(defn get-meta-by-imdb-id [host id]
  (clc/log-on-error
   {:status "failed" :message "Could not find item in catalog"}
    (let [url (str "http://" host "/imdbid/" id)]
      (:body (client/get url {:as :json})))))

(defn get-series-episodes [host series]
  (clc/log-on-error {:status "failed" :message "meta host not available"}
    (let [url (str "http://" host "/series/" series)]
      (println "meh " url)
      (:body (client/get url {:as :json})))))

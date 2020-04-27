(ns remote-call.meta
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [ring.util.codec :refer [url-encode]]
            [clj-http.client :as client]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

;; following code should be used to check for finding nil values in a map
;; (every? some? (map second (into [] {:a 1 :b 2 :c 3})))

(def series-pattern "http://%s/series/%s/%s/%s")

(defn create-episode [host series season episode record]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (client/post
    (format series-pattern host (url-encode series) season episode)
    (if record {:as :json
                :content-type :json
                :body (generate-string record)}
        {:as :json}))))

(defn update-episode [host series season episode data]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (client/put (format series-pattern
                    host (url-encode series) season episode)
      {:as :json
        :content-type :json
        :headers {"content-type" "application/json"}
        :body (generate-string data)
        })))

(defn update-series [host series data]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (client/put (format "http://%s/series/%s" host (url-encode series))
      {:as :json :body (generate-string data)
        :headers {"content-type" "application/json"}})))

(defn create-series [host series data]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (client/post (format "http://%s/series/%s" host (url-encode series))
                {:as :json :body (generate-string data)
                 :headers {"content-type" "application/json"}})))

(defn fetch-series [host series catalog_id_only]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
    (:body (client/get (format "http://%s/series/%s" host (url-encode series))
                       (merge {:as :json}
                              (when catalog_id_only
                                {:query-params {:catalog_id_only "true"}}))))))

(defn get-episode [host series season episode catalog_id_only]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (:body (client/get
           (format series-pattern
                host (url-encode series) season episode)
           (merge {:as :json} (when catalog_id_only
                                {:query-params {:catalog_id_only "true"}}))))))

(defn delete-episode [host series season episode]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (client/delete (format series-pattern
                          host (url-encode series) season episode)
                  {:as :json})))

(defn get-by-catalog-id [host catalog-id fields]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (:body (client/get (format "http://%s/catalog-id/%s%s" host catalog-id
                              (if fields (str "?fields="fields) ""))
      {:as :json :unexceptional-status #(or (= 200 %) (= 404 %))}))))

(defn get-all-series [host]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (:body (client/get (format "http://%s/series" host)
                      {:as :json}))))

(defn get-summary [host]
  (clc/log-on-error
   {:status "failed" :message "meta service not available"}
   (:body (client/get (format "http://%s/summary" host)
                      {:as :json}))))

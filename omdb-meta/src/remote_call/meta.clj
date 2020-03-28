(ns remote-call.meta
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

;; following code should be used to check for finding nil values in a map
;; (every? some? (map second (into [] {:a 1 :b 2 :c 3})))

(defn create-episode [host series season episode record]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (client/post (str "http://" host "/series/" series "/" season "/" episode)
                 (if record {:as :json
                             :content-type :json
                             :body (generate-string record)}
                     {:as :json}))))

(defn update-episode [host series season episode data]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (client/put (str "http://" host "/series/" series "/" season "/" episode)
      {:as :json
        :content-type :json
        :headers {"content-type" "application/json"}
        :body (generate-string data)
        })))

(defn update-series [host series data]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (client/put (str "http://" host "/series/" series)
      {:as :json :body (generate-string data)
        :headers {"content-type" "application/json"}})))

(defn fetch-series [host series catalog_id_only]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (:body (client/get (str "http://" host "/series/" series)
      (merge {:as :json} (if catalog_id_only {:query-params {:catalog_id_only "true"}}))))))

(defn get-episode [host series season episode catalog_id_only]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (:body (client/get (str "http://" host "/series/" series "/" season "/" episode)
      (merge {:as :json} (if catalog_id_only {:query-params {:catalog_id_only "true"}}))))))

(defn delete-episode [host series season episode]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (client/delete (str "http://" host "/series/" series "/" season "/" episode)
      {:as :json})))

(defn get-by-catalog-id [host catalog-id fields]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (:body (client/get (str "http://" host "/catalog-id/" catalog-id (if fields (str "?fields="fields)))
      {:as :json :unexceptional-status #(or (= 200 %) (= 404 %))}))))

(defn get-all-series [host]
  (clc/log-on-error {:status "failed" :message "meta service not available"}
    (:body (client/get (str "http://" host "/series") {:as :json}))))

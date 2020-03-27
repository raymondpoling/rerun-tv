(ns file-meta.handler
  (:require [compojure.core :refer :all]
            [ring.middleware.json :as json]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [db.db :refer :all]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn make-catalog-id [prefix season episode]
  (format "%s%02d%03d"
    prefix
    (Integer/parseInt season)
    (Integer/parseInt episode)))

(defn id [record]
  (or (:generated_key record)
      (:id record)))

(defroutes app-routes
  (POST "/series/:name{[^/]+}/:season/:episode" [name season episode]
    (fn [request]
      (let [series_record? (find-series name)
          series (if (nil? (id series_record?)) (insert-series name) series_record?)
          record (merge (:body request) {:season season :episode episode})]
          (try
            (insert-record (id series) record)
            (clc/make-response 200 {:status :ok :catalog_ids
              [(make-catalog-id (:catalog_prefix series) season episode)]})
            (catch java.sql.SQLException e
              (logger/error e)
              (clc/make-response 500 {:status :failure}))))))
  (PUT "/series/:name{[^/]+}/:season/:episode" [name season episode]
    (fn [request]
      (let [series_record? (find-series name)
            series (if (nil? (id series_record?)) (insert-series name) series_record?)]

            (try
              (logger/debug (format "series: %s\n\tseason: %s\n\tepisode: %s\n\tbody:%s" name season episode (:body request)))
              (update-record (id series) season episode (:body request))
              (clc/make-response 200 {:status :ok :catalog_ids
                [(make-catalog-id (:catalog_prefix series) season episode)]})
            (catch java.sql.SQLException e
              (logger/error e)
              (clc/make-response 500 {:status :failure}))
            (catch Exception e
              (logger/error e))))))
  (PUT "/series/:name{[^/]+}" [name]
    (fn [request]
      (let [series_record? (find-series name)
            series (if (nil? (id series_record?)) (insert-series name) series_record?)]
            (try
              (doall (map (fn [t] (update-record (id series) (:season t) (:episode t) t)) (:records (:body request))))
              (logger/debug "Processing: " (:body request))
              (logger/debug "Records: " (:records (:body request)))
              (let [series? (:series (:body request))]
                (if series? (update-series name series?)))
              (clc/make-response 200 {:status :ok :catalog_ids
                (map (fn [t] (make-catalog-id (:catalog_prefix series) (str (:season t)) (str (:episode t)))) (:records (:body request)))})
            (catch java.sql.SQLException e
              (logger/error e)
              (clc/make-response 500 {:status :failure}))))))
  (GET "/series/:name{[^/]+}" [name catalog_id_only fields]
    (let [top-record (find-by-series name)
          series (first top-record)
          records (second top-record)
          catalog_ids (map (fn [t] (make-catalog-id (:catalog_prefix t) (str (:season t)) (str (:episode t)))) records)
          to_return (if catalog_id_only nil {:records series})]
          (if (nil? records)
            (clc/make-response 404 {:status :not_found})
            (clc/make-response 200 (merge to_return {:status :ok :catalog_ids catalog_ids} )))))
  (GET "/series/:name{[^/]+}/:season/:episode" [name season episode]
    (fn [request]
      (let [catalog_ids? (or (:catalog_id_only (:params request)))
            record (first (find-by-series-season-episode name season episode))
            catalog_id (make-catalog-id (:catalog_prefix record) season episode)
            fields (clojure.string/split (or (:fields (:params request)) "") #",")
            out_record (if (= fields (list "")) record (select-keys record (map keyword fields)))
            to_return (if catalog_ids? nil {:records [(dissoc out_record :catalog_prefix)]})]
            (if (nil? record)
              (clc/make-response 404 {:status :not_found})
              (clc/make-response 200 (merge to_return {:status :ok :catalog_ids [catalog_id]}))))))
  (DELETE "/series/:name{[^/]+}/:season/:episode" [name season episode]
    (let [catalog_id (delete-record name (Integer/parseInt season) (Integer/parseInt episode))]
    (clc/make-response 200 {:status :ok :catalog_ids [(make-catalog-id catalog_id season episode)]})))
  (GET "/catalog-id/:catalog_id" [catalog_id]
    (if (= 12 (count catalog_id))
      (fn [request]
        (let [record (first (find-by-catalog_id catalog_id))
              fields (clojure.string/split (or (:fields (:params request)) "") #",")
              out_record (if (= fields (list "")) record (select-keys record (map keyword fields)))]
              (if (nil? record)
                (clc/make-response 404 {:status :not_found})
                (clc/make-response 200 {:status :ok :catalog_ids [catalog_id] :records [(dissoc out_record :catalog_prefix)]}))))
        (clc/make-response 404 {:status :not_found})))
    (GET "/series" []
      (let [series (find-all-series)]
        (logger/debug "series " series)
        (clc/make-response 200 {:status :ok :results (map #(:name %) series)})))
  (route/not-found (clc/make-response 404 {:status "not found"})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "meta_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4004"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

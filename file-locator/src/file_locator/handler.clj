(ns file-locator.handler
  (:require [compojure.core :refer :all]
              [ring.middleware.json :as json]
              [compojure.route :as route]
              [ring.util.response :refer [response not-found header status]]
              [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
              [db.db :refer :all]
              [common-lib.core :as clc]
              [clojure.tools.logging :as logger]
              [file-locator.url :refer [make-url]]
              [org.httpkit.server :refer [run-server]])
    (:gen-class))

(defroutes app-routes
  (GET "/:protocol/:host/:catalog_id" [protocol host catalog_id]
    (try
      (let [url (fetch-url protocol host catalog_id)]
        (clc/make-response 200 {:status :ok :url (make-url {:protocol protocol :host host :url url})}))
      (catch Exception e
        (logger/error (str "could not get url for protocol/host/catalog_id '" protocol "/" host "/" catalog_id ": " (.getMessage e)))
        (clc/make-response 500 {:status :failed}))))
  (POST "/:protocol/:host/:catalog_id" [protocol host catalog_id]
    (fn [request]
      (try
        (let [path (:path (:body request))
              host-id (find-or-insert-host host)
              catalog-id (find-or-insert-catalog-id catalog_id)
              protocol-id (find-or-insert-protocol protocol)]
          (insert-url protocol-id host-id catalog-id path)
          (clc/make-response 200 {:status :ok}))
      (catch Exception e
        (logger/error (str "could not post url for protocol/host/catalog_id '" protocol "/" host "/" catalog_id ": " (.getMessage e)))
        (clc/make-response 500 {:status :failed})))))
  (route/not-found
    (fn [request]
      (do
        (logger/warn (str "document not found "  request)))
        (clc/make-response 404 {:status :not-found}))))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "locator_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4005"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

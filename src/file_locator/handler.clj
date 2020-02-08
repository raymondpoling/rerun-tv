(ns file-locator.handler
  (:require [compojure.core :refer :all]
              [ring.middleware.json :as json]
              [compojure.route :as route]
              [ring.util.response :refer [response not-found header status]]
              [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
              [db.db :refer :all]
              [file-locator.url :refer [make-url]]
              [org.httpkit.server :refer [run-server]])
    (:gen-class))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))


(defroutes app-routes
  (GET "/:protocol/:host/:catalog_id" [protocol host catalog_id]
    (let [url (fetch-url protocol host catalog_id)]
      (make-response 200 {:status :ok :url (make-url {:protocol protocol :host host :url url})})))
  (POST "/:protocol/:host/:catalog_id" [protocol host catalog_id]
    (fn [request]
      (let [path (:path (:body request))
            host-id (find-or-insert-host host)
            catalog-id (find-or-insert-catalog-id catalog_id)
            protocol-id (find-or-insert-protocol protocol)]
        (insert-url protocol-id host-id catalog-id path)
        (make-response 200 {:status :ok}))))
  (route/not-found {:status :not_found}))

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
    (println (str "Listening on port " port))))

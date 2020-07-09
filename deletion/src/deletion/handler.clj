(ns deletion.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [db.db :refer [initialize create-record get-outstanding
                           get-records reject execute]]
            [remote-call.remotes :refer [delete-item]]
            [clojure.tools.logging :as logger]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (POST "/nominate/:atype/:a-name" [atype a-name]
        (fn [{{:keys [user reason]} :body}]
          (try
            (let [_ (create-record atype a-name user reason)]
              (clc/make-response 200 {:status :ok}))
            (catch Exception e
              (clc/make-response 400 {:status :failed,
                                      :message (.getMessage e)})))))
  (GET "/nominate" []
       (clc/make-response 200 {:status :ok :outstanding (get-outstanding)}))
  (GET "/recent" []
       (clc/make-response 200 {:status :ok, :records (get-records)}))
  (POST "/reject/:atype/:a-name" [atype a-name]
        (fn [{{:keys [user reason]} :body}]
          (try
            (let [_ (reject atype a-name user reason)]
              (clc/make-response 200 {:status :ok}))
            (catch Exception e
              (clc/make-response 400 {:status :failed,
                                      :message (.getMessage e)})))))
  (POST "/execute/:atype/:a-name" [atype a-name]
        (fn [{{:keys [user reason]} :body}]
          (try
          (let [_ (execute atype a-name user reason delete-item)]
            (clc/make-response 200 {:status :ok}))
          (catch Exception e
            (clc/make-response 400 {:status :failed,
                                    :message (.getMessage e)})))))
  (route/not-found (clc/make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "deletion_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4016"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

(ns messages.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [ring.util.response :refer [not-found]]
            [db.db :refer :all]
            [clojure.tools.logging :as logger]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [java-time :as jt]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (POST "/" [author title information]
    (try
      (let
        [date (jt/java-date (jt/instant))
         t (save-event author date title information)]
         (logger/debug "Stored data: " author ": " date ": " title " - " information)
        (clc/make-response 200 {:status :ok}))
    (catch Exception e
      (logger/error "failed posting message: [" information "] " (.getMessage e))
      (clc/make-response 500 {:status "failed" :message "service failed"}))))
  (GET "/" [start step]
    (try
      (let
        [resp (get-events start step)]
        (logger/debug "responses for start/step [" start "/" step "]: " resp)
        (clc/make-response 200 {:status :ok :events resp}))
      (catch Exception e
        (logger/error "failed to fetch " start " step " step " due to \""(.getMessage e)"\"")
        (clc/make-response 500 {:status "failed" :message "service failed"}))))
  (route/not-found (clc/make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "event_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4010"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

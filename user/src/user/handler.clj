(ns user.handler
  (:require [compojure.core :refer [defroutes DELETE GET POST PUT]]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [db.db :refer [delete-user get-and-update
                           initialize insert-user]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (POST "/:user" [user]
    (let [created? (insert-user user)]
      (if (nil? created?)
        (do
          (logger/warn (str "could not create user '" user "'"))
          (clc/make-response 400 {:status :failed}))
        (clc/make-response 200 {:status "ok"}))))
  (DELETE "/:user" [user]
    (if (not (nil? (delete-user user)))
      (clc/make-response 200 {:status "ok"})
      (do
        (logger/warn (str "could not delete user '" user "'"))
        (clc/make-response 400 {:status :failed}))))
  (GET "/:user/:schedule" [user schedule preview]
    (let [value (get-and-update user schedule nil preview)]
      (if (nil? value)
          (do
            (logger/warn (str "user/schedule not found '" user "/" schedule "'"))
            (clc/make-response 404 {:status :not-found}))
          (do
            (logger/debug (str "index? " value " preview? " preview))
            (clc/make-response 200 {:status :ok :idx value})))))
  (PUT "/:user/:schedule/:index" [user schedule index]
    (if (get-and-update user schedule index false)
      (clc/make-response 200 {:status :ok})
      (clc/make-response 400 {:status :failed})))
  (route/not-found (clc/make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "user_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4002"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

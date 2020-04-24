(ns schedule.handler
  (:require [schedule.schedule-types :refer [make-schedule-from-json frame]]
            [ring.middleware.json :as json]
            [compojure.core :refer [DELETE GET POST PUT defroutes]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [db.db :as db]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (GET "/" []
    (try
      (let [schedules (db/find-schedules)]
        (clc/make-response 200 {:status :ok :schedules schedules}))
    (catch Exception e
      (logger/error (str "failed to get schedules: " (.getMessage e)))
      (clc/make-response 500 {:status :failed}))))
  (POST "/:name" [name]
      (fn [request]
        (try
          (if (nil? (db/find-schedule name))
            (let [sched (make-schedule-from-json (:body request))]
              (db/insert-schedule name sched)
              (clc/make-response 200 {"status" "ok"}))
            (clc/make-response 400 {:status :failed :message (str name " is already defined.")}))
          (catch Exception e
            (logger/error (str "Cannot save schedule '" name "': " (.getMessage e)))
            (clc/make-response 412 {:status :failed :message (ex-data e)})))))
  (GET "/:name" [name]
    (if (not (nil? (db/find-schedule name)))
      (clc/make-response 200 {:status :ok, :schedule (db/find-schedule name)})
      nil))
  (GET "/:name/:index" [name index]
    (clc/make-response 200 {:status :ok :items (frame (make-schedule-from-json (db/find-schedule name)) (Integer/parseInt index))}))
  (PUT "/:name" [name]
      (fn [request]
        (try
          (let [sched (make-schedule-from-json (:body request))]
            (db/update-schedule name sched)
            (clc/make-response 200 {"status" "ok"}))
            (catch Exception e
              (logger/error (str "Could not update schedule '" name "': " (.getMessage e)))
              (clc/make-response 412 {:status :failed :message (:message (ex-data e))})))))
  (DELETE "/:name" [name]
    (db/delete-schedule name)
    (clc/make-response 200 {"status" "ok"}))
  (route/not-found
    (clc/make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "schedule_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (db/initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4000"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

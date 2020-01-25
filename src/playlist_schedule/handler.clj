(ns playlist-schedule.handler
  (:require [schedule.schedule-types :refer [make-schedule-from-json frame]]
            [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [db.db :refer :all]))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(defroutes app-routes
  (POST "/:name" [name]
      (fn [request]
        (try
          (if (nil? (find-schedule name))
            (let [sched (make-schedule-from-json (:body request))]
              (insert-schedule name sched)
              (make-response 200 {"status" "ok"}))
            (make-response 400 {:message (str name " is already defined.")}))
          (catch Exception e
            (make-response 412 {:message (ex-data e)})))))
  (GET "/:name" [name]
    (if (not (nil? (find-schedule name)))
      (make-response 200 (find-schedule name))
      (route/not-found (str "Not Found " name))))
  (GET "/:name/:index" [name index]
    (make-response 200 (frame (make-schedule-from-json (find-schedule name)) (Integer/parseInt index))))
  (PUT "/:name" [name]
      (fn [request]
        (try
          (let [sched (make-schedule-from-json (:body request))]
            (update-schedule name sched)
            (make-response 200 {"status" "ok"}))
            (catch Exception e
              (make-response 412 {:message (:message (ex-data e))})))))
  (DELETE "/:name" [name]
    (delete-schedule name)
    (make-response 200 {"status" "ok"}))
  (route/not-found "Not Found"))

(initialize "schedule_user" "schedule")


(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

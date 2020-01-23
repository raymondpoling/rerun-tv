(ns playlist-schedule.handler
  (:require [schedule.schedule-types :refer [make-schedule-from-json frame]]
            [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def store (atom {}))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(defroutes app-routes
  (POST "/:name" [name]
      (fn [request]
        (try
          (if (nil? (get @store name))
            (let [sched (make-schedule-from-json (:body request))]
              (swap! store assoc name sched)
              (make-response 200 {"status" "ok"}))
            (make-response 400 {:message (str name " is already defined.")}))
          (catch Exception e
            (make-response 412 {:message (ex-data e)})))))
  (GET "/:name" [name]
    (if (not (nil? (get @store name)))
      (make-response 200 (get @store name))
      (route/not-found (str "Not Found " name))))
  (GET "/:name/:index" [name index]
    (make-response 200 (frame (get @store name) (Integer/parseInt index))))
  (PUT "/:name" [name]
      (fn [request]
        (try
          (let [sched (make-schedule-from-json (:body request))]
            (swap! store assoc name sched)
            (make-response 200 {"status" "ok"}))
            (catch Exception e
              (make-response 412 {:message (:message (ex-data e))})))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(ns playlist-playlist.handler
  (:require [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [db.db :refer :all]))

(defroutes app-routes
  (POST "/:name" [name]
    (fn [request]
      (try
      (let [data (:body request)]
        (insert-series name)
        (insert-playlist name (:playlist data))
      (response {:status "ok"}))
      (catch java.sql.SQLIntegrityConstraintViolationException e
        (-> (response {:status "invalid"})
          (status 412))))))
  (PUT "/:name" [name]
    (fn [request]
      (try
      (let [data (:body request)]
        (replace-playlist name (:playlist data))
      (response {:status "ok"}))
      (catch java.sql.SQLIntegrityConstraintViolationException e
        (-> (response {:status "invalid"})
          (status 412))))))
  (DELETE "/:name" [name]
    (delete-series name)
    (response {:status "ok"}))
  (GET "/:name/:idx" [name idx]
    (find-item name idx))
  (route/not-found "Not Found"))

(initialize "playlist_user" "playlist")

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

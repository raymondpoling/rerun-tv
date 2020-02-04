(ns playlist-playlist.handler
  (:require [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [db.db :refer :all]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (GET "/" []
    (response (get-all-playlists)))
  (POST "/:name" [name]
    (fn [request]
      (try
      (let [data (:body request)]
        (insert-series name)
        (insert-playlist name (:playlist data))
      (response {:status "ok"}))
      (catch java.sql.SQLIntegrityConstraintViolationException e
        (-> (response {:status "invalid"})
          (status 412)))
      (catch java.sql.SQLException e
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
          (status 412)))
      (catch java.sql.SQLException e
        (-> (response {:status "invalid"})
          (status 412))))))
  (DELETE "/:name" [name]
    (delete-series name)
    (response {:status "ok"}))
  (GET "/:name/:idx" [name idx]
    (find-item name idx))
  (route/not-found "Not Found"))


(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "playlist_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4001"))]
    (run-server app {:port port})
    (println (str "Listening on port " port))))

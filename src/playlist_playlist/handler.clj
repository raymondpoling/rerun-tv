(ns playlist-playlist.handler
  (:require [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [db.db :refer :all]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn invalid-request [operation playlist e]
  (logger/error (str "error processing request for '" operation " " playlist "': " (.getMessage e)))
  (clc/make-response 412 {:status "invalid"}))

(defroutes app-routes
  (GET "/" []
    (clc/make-response 200 {:status :ok, :playlists (get-all-playlists)}))
  (POST "/:name" [name]
    (fn [request]
      (try
        (let [data (:body request)]
          (insert-series name)
          (insert-playlist name (:playlist data))
          (clc/make-response 200 {:status "ok"}))
      (catch java.sql.SQLIntegrityConstraintViolationException e
        (invalid-request "POST" name e))
      (catch java.sql.SQLException e
        (invalid-request "POST" name e)))))
  (PUT "/:name" [name]
    (fn [request]
      (try
        (let [data (:body request)]
          (replace-playlist name (:playlist data))
          (clc/make-response 200 {:status "ok"}))
        (catch java.sql.SQLIntegrityConstraintViolationException e
          (invalid-request "PUT" name e))
        (catch java.sql.SQLException e
          (invalid-request "PUT " name e)))))
  (DELETE "/:name" [name]
    (delete-series name)
    (logger/warn (str "playlist '" name "' deleted"))
    (clc/make-response 200 {:status "ok"}))
  (GET "/:name" [name]
    (if-let [item (find-playlist name)]
      (clc/make-response 200 {:status "ok", :playlist item})
      nil)) ; let not-found catch it
  (GET "/:name/:idx" [name idx]
    (if-let [item (find-item name idx)]
      (clc/make-response 200 {:status "ok", :playlist item})
      nil)) ; let not-found catch it
  (route/not-found (clc/make-response 404 {:status :not-found})))


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

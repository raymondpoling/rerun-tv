(ns format.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [remote-call.fetch-records :refer [fetch]]
            [remote-call.user :refer [get-index]]
            [format.m3u :refer [m3u]]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
    (:gen-class))

(defn make-response [st schedule index resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/mpegurl")
      (header "Content-Disposition" (str "attachment; filename=\"" schedule "-" index ".m3u\""))))

(def ^:dynamic *playlist-host* "")
(def ^:dynamic *schedule-host* "")
(def ^:dynamic *user-host* "")
(def ^:dynamic *locator-host* "")
(def ^:dynamic *meta-host* "")

(defroutes app-routes
  (GET "/:user/:schedule-name" [user schedule-name]
    (let [
      index (get-index *user-host* user schedule-name)
      records (fetch *schedule-host* *playlist-host* *locator-host* *meta-host* user index schedule-name)]
      (make-response 200 schedule-name index (m3u schedule-name index records))))

  (route/not-found "Not Found"))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [playlist-host (or (System/getenv "PLAYLIST_HOST") "playlist")
        schedule-host (or (System/getenv "SCHEDULE_HOST") "schedule")
        user-host (or (System/getenv "USER_HOST") "playlist")
        locator-host (or (System/getenv "LOCATOR_HOST") "locator")
        meta-host (or (System/getenv "META_HOST") "meta")
        playlist-port (or (System/getenv "PLAYLIST_PORT") "4001")
        schedule-port (or (System/getenv "SCHEDULE_PORT") "4000")
        user-port (or (System/getenv "USER_PORT") "4002")
        locator-port (or (System/getenv "LOCATOR_PORT") "4006")
        meta-port (or (System/getenv "META_PORT") "4004")]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4009"))]
    (alter-var-root #'*playlist-host* (constantly (str playlist-host ":" playlist-port)))
    (alter-var-root #'*schedule-host* (constantly (str schedule-host ":" schedule-port)))
    (alter-var-root #'*user-host* (constantly (str user-host ":" user-port)))
    (alter-var-root #'*locator-host* (constantly (str locator-host ":" locator-port)))
    (alter-var-root #'*meta-host* (constantly (str meta-host ":" meta-port)))
    (run-server app {:port port})
    (println (str "Listening on port " port)))))

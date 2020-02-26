(ns schedule-builder.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [remote-call.playlist :refer [get-playlists get-playlist get-playlists-map]]
            [remote-call.schedule :refer [get-schedule post-schedule put-schedule]]
            [remote-call.validate :refer [validate-schedule]]
            [ring.middleware.json :as json]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(def ^:dynamic *playlist-host* "")
(def ^:dynamic *schedule-host* "")

(defroutes app-routes
  (GET "/playlists" []
    (make-response 200 (get-playlists *playlist-host*)))
  (GET "/playlists/:playlist" [playlist]
    (make-response 200 (get-playlist *playlist-host* playlist)))
  (PUT "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [playlists-map (get-playlists-map *playlist-host*)
            validate (validate-schedule playlists-map (:body request))]
        (if (= (:status validate) :ok)
          (make-response 200 (:body (put-schedule *schedule-host* schedule (:body request))))
          (make-response 200 validate)))))
  (POST "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [playlists-map (get-playlists-map *playlist-host*)
            validate (validate-schedule playlists-map (:body request))]
        (if (= (:status validate) :ok)
          (make-response 200 (:body (post-schedule *schedule-host* schedule (:body request))))
          (make-response 200 validate)))))
  (GET "/schedule/validate" []
    (fn [request]
      (let [playlists-map (get-playlists-map *playlist-host*)
            validate (validate-schedule playlists-map (:body request))]
      validate)))
  (GET "/schedule/validate/:schedule" [schedule]
    (let [schedule (get-schedule *schedule-host* schedule)
          playlists-map (get-playlists-map *playlist-host*)
          validate (validate-schedule playlists-map schedule)]
          (println "schedule " schedule)
          (println "playlist-map " playlists-map)
          (println "valid? " validate)
      (make-response 200 validate)))
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
        playlist-port (or (System/getenv "PLAYLIST_PORT") "4001")
        schedule-port (or (System/getenv "SCHEDULE_PORT") "4000")]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4003"))]
    (alter-var-root #'*playlist-host* (constantly (str playlist-host ":" playlist-port)))
    (alter-var-root #'*schedule-host* (constantly (str schedule-host ":" schedule-port)))
    (run-server app {:port port})
    (println (str "Listening on port " port)))))

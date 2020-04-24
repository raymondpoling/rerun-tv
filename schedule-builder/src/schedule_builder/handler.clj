(ns schedule-builder.handler
  (:require [compojure.core :refer [GET POST PUT defroutes]]
            [compojure.route :as route]
            [remote-call.playlist :refer [get-playlists
                                          get-playlist
                                          get-playlists-map]]
            [remote-call.schedule :refer [get-schedule
                                          post-schedule
                                          put-schedule]]
            [remote-call.validate :refer [validate-schedule]]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as logging]
            [common-lib.core :as clc]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(def hosts (clc/make-hosts ["playlist" 4001]
                           ["schedule" 4000]))

(defn hosts-or-valid [name body]
  (let [playlists-map (get-playlists-map (:playlist hosts))]
    (if (= :failure (:status playlists-map))
        {:status :failure :message (:message playlists-map)}
        (validate-schedule playlists-map name body))))

(defroutes app-routes
  (GET "/playlists" []
    (clc/make-response 200 (get-playlists (:playlist hosts))))

  (GET "/playlists/:playlist" [playlist]
    (clc/make-response 200 (get-playlist (:playlist hosts) playlist)))

  (POST "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [validate (hosts-or-valid schedule (:body request))]
        (if (= (:status validate) :ok)
            (let [response (post-schedule (:schedule hosts) schedule (:body request))]
              (if (= (:status response) :failure)
                (clc/make-response 200 {:status :failure :messages ["Cannot Create Schedule: Already Exists"]})
                (clc/make-response 200 (:body response))))
            (clc/make-response 200 validate)))))

  (PUT "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [validate (hosts-or-valid schedule (:body request))]
        (if (= (:status validate) :ok)
          (let [response (put-schedule (:schedule hosts) schedule (:body request))]
            (if (= (:status response) :failure)
              (clc/make-response 200 {:status :failure :messages ["Schedule Does Not Exist: cannot update schedule"]})
              (clc/make-response 200 (:body response))))
          (clc/make-response 200 validate)))))

  (GET "/schedule/validate" []
    (fn [request]
      (let [playlists-map (get-playlists-map (:playlist hosts))
            schedule (:body request)
            validate (validate-schedule playlists-map (:name schedule) schedule)]
      (logging/debug (str "Valid?: " validate "\nschedule: " schedule "\nrequests: " request))
      (clc/make-response 200 validate))))

  (GET "/schedule/validate/:schedule-name" [schedule-name]
    (let [schedule (get-schedule (:schedule hosts) schedule-name)
          playlists-map (get-playlists-map (:playlist hosts))
          validate (validate-schedule playlists-map schedule-name schedule)]
          (logging/debug "schedule " schedule)
          (logging/debug "playlist-map " playlists-map)
          (logging/debug "valid? " validate)
      (clc/make-response 200 validate)))
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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4003"))]
    (run-server app {:port port})
    (logging/info (str "Listening on port " port))))

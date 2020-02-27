(ns schedule-builder.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [remote-call.playlist :refer [get-playlists get-playlist get-playlists-map]]
            [remote-call.schedule :refer [get-schedule post-schedule put-schedule]]
            [remote-call.validate :refer [validate-schedule]]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as logging]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(defn make-host [prefix default-port]
  (let [upcase-prefix (clojure.string/upper-case prefix)
        host (or (System/getenv (str upcase-prefix "_HOST"))
                  prefix)
        port (or (System/getenv (str upcase-prefix "_PORT")) default-port)]
  (str host ":" port)))

(def hosts {:playlist (make-host "playlist" 4001)
            :schedule (make-host "schedule" 4000)})

(defn hosts-or-valid [name body]
  (let [playlists-map (get-playlists-map (:playlist hosts))]
    (if (= :failure (:status playlists-map))
        {:status :failure :message (:message playlists-map)}
        (validate-schedule playlists-map name body))))

(defroutes app-routes
  (GET "/playlists" []
    (make-response 200 (get-playlists (:playlist hosts))))

  (GET "/playlists/:playlist" [playlist]
    (make-response 200 (get-playlist (:playlist hosts) playlist)))

  (POST "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [validate (hosts-or-valid schedule (:body request))]
        (if (= (:status validate) :ok)
            (let [response (post-schedule (:schedule hosts) schedule (:body request))]
              (if (= (:status response) :failure)
                (make-response 200 {:status :failure :message "cannot create schedule"})
                (make-response 200 (:body response))))
            (make-response 200 validate)))))

  (PUT "/schedule/store/:schedule" [schedule]
    (fn [request]
      (let [validate (hosts-or-valid schedule (:body request))]
        (if (= (:status validate) :ok)
          (let [response (put-schedule (:schedule hosts) schedule (:body request))]
            (if (= (:status response) :failure)
              (make-response 200 {:status :failure :message "cannot update schedule"})
              (make-response 200 (:body response))))
          (make-response 200 validate)))))

  (GET "/schedule/validate" []
    (fn [request]
      (let [playlists-map (get-playlists-map (:playlist hosts))
            validate (validate-schedule playlists-map (:name (:body request)) (:body request))]
      (make-response 200 validate))))

  (GET "/schedule/validate/:schedule-name" [schedule-name]
    (let [schedule (get-schedule (:schedule hosts) schedule-name)
          playlists-map (get-playlists-map (:playlist hosts))
          validate (validate-schedule playlists-map schedule-name schedule)]
          (logging/debug "schedule " schedule)
          (logging/debug "playlist-map " playlists-map)
          (logging/debug "valid? " validate)
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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4003"))]
    (run-server app {:port port})
    (println (str "Listening on port " port))))

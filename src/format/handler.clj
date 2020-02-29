(ns format.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [remote-call.fetch-records :refer [fetch]]
            [remote-call.user :refer [get-index]]
            [format.m3u :refer [m3u]]
            [common-lib.core :as clc]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.tools.logging :as logger]
            [cheshire.core :refer :all]
            [org.httpkit.server :refer [run-server]])
    (:gen-class))

(defn make-m3u-response [st schedule index resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/mpegurl")
      (header "Content-Disposition" (str "attachment; filename=\"" schedule "-" index ".m3u\""))))

(def hosts (clc/make-hosts ["playlist" 4001]
                           ["schedule" 4000]
                           ["locator" 4006]
                           ["user" 4002]
                           ["meta" 4004]))

(defroutes app-routes
  (GET "/:user/:schedule-name" [user schedule-name]
    (let [user-host (:user hosts)
          schedule-host (:schedule hosts)
          playlist-host (:playlist hosts)
          locator-host (:locator hosts)
          meta-host (:meta hosts)
          index (get-index user-host user schedule-name)
          records (fetch schedule-host playlist-host locator-host meta-host user index schedule-name)
          failure (filter #(= "failure" (:status %)) [index records])]
      (logger/error "Failures? " failure)
      (logger/debug user schedule-name index "records: " (generate-string [index records]))
      (if (empty? failure)
        (make-m3u-response 200 schedule-name index (m3u schedule-name index records))
        (clc/make-response 502 (first failure)))))

  (route/not-found "Not Found"))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4009"))]
    (run-server app {:port port})
    (println (str "Listening on port " port))))

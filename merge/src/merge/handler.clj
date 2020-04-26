(ns merge.handler
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [remote-call.fetch-records :refer [fetch]]
            [remote-call.locator :refer [get-protocol-hosts]]
            [common-lib.core :as clc]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.tools.logging :as logger]
            [org.httpkit.server :refer [run-server]])
    (:gen-class))

(def hosts (clc/make-hosts ["playlist" 4001]
                           ["schedule" 4000]
                           ["locator" 4006]
                           ["omdb" 4011]))

(defroutes app-routes
  (GET "/:user/:schedule-name/:index" [user schedule-name index host protocol]
    (let [schedule-host (:schedule hosts)
          playlist-host (:playlist hosts)
          locator-host (:locator hosts)
          omdb-host (:omdb hosts)
          records (fetch schedule-host playlist-host locator-host
                         omdb-host index schedule-name
                         host protocol)
          failure (filter #(= "failure" (:status %)) [index records])]
      (logger/info "Failures? " failure)
      (logger/debug user schedule-name index "records: " (str [index records]))
      (if (empty? failure)
        (do
          (logger/debug "records: " records)
          (clc/make-response 200 {:status :ok :playlist records}))
        (clc/make-response 502 (first failure)))))
  (GET "/protocol-host" []
       (let [locator-host (:locator hosts)]
         (clc/make-response 200 (get-protocol-hosts locator-host))))

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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4013"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

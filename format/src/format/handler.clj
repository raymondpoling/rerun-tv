(ns format.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [remote-call.user :refer [get-index set-index]]
            [remote-call.merge :refer [get-merge
                                       fetch-protocol-host]]
            [format.m3u :refer [m3u]]
            [common-lib.core :as clc]
            [ring.util.response :refer [response header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.tools.logging :as logger]
            [org.httpkit.server :refer [run-server]])
    (:gen-class))

(defn make-m3u-response [st schedule index resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/mpegurl")
      (header "Content-Disposition" (str "attachment; filename=\"" schedule "-" index ".m3u\""))))

(def hosts (clc/make-hosts ["merge" 4012]
                           ["user" 4002]))

(defroutes app-routes
  (GET "/:user/:schedule-name"
       [user schedule-name index update host protocol format]
       (let [merge-host (:merge hosts)
             user-host (:user hosts)
             idx (or (when index (Integer/parseInt index))
                     (get-index user-host user schedule-name update))
             records (if (not (nil? (:status idx)))
                       {:status :failure :message "user service not available"}
                       (get-merge merge-host
                                schedule-name user idx host protocol))]
      (logger/debug user schedule-name idx index "records: "
                    (str [idx records]))
      (if (= "ok" (:status records))
        (let [response (case format
                         "m3u" (make-m3u-response 200
                                                  schedule-name
                                                  idx
                                                  (m3u schedule-name idx
                                                       (:playlist records)))
                         (clc/make-response 200 records))]
          (logger/debug (str "Update status for idx " idx " with an idx of "
                             (type idx) " of " update))
          (when update
            (set-index user-host user schedule-name (+ 1  idx)))
          response)
        (clc/make-response 502 {:status :failure
                                :message (:message records)}))))
  (GET "/formats" []
       (let [protocol-hosts (:formats (fetch-protocol-host (:merge hosts)))
             formats (sort (concat (map #(format "%s/m3u" %) protocol-hosts)
                             (map #(format "%s/json" %) protocol-hosts)))]
         (println "prot-hosts?" protocol-hosts)
         (clc/make-response 200 {:status :ok :formats formats})))
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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4009"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

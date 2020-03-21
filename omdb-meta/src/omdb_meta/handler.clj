(ns omdb-meta.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [remote-call.meta :refer :all]
            [omdb-meta.update :as update]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as logger]
            [common-lib.core :as clc]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(def hosts (clc/make-hosts ["omdb" 8888]
                           ["meta" 4004]))

(def apikey (System/getenv "APIKEY"))

(defroutes app-routes
  (POST "/series/:name/:season/:episode" [name season episode]
    (fn [request]
      (let [updated-response (update/update-episode (merge (:body request) {:season season :episode episode}) name (:omdb hosts) apikey)
            create (:body (create-episode (:meta hosts) name season episode updated-response))]
            (if (= "ok" (:status create))
              (clc/make-response 200 create)
              (clc/make-response 500 create)))))
  (PUT "/series/:name/:season/:episode" [name season episode]
    (fn [request]
      (let [updated (update/update-episode (merge (:body request) {:season season :episode episode}) name (:omdb hosts) apikey)
            update (:body (update-episode (:meta hosts) name season episode updated))]
        (if (= "ok" (:status update))
          (clc/make-response 200 update)
          (clc/make-response 500 update)))))
  (PUT "/series/:name" [name]
    (fn [request]
      (let [omdb (:omdb hosts)
            updated-series (update/update-series (:series (:body request)) name omdb apikey)
            updated-records (map #(update/update-episode % name omdb apikey) (:records (:body request)))
            updated-record {:series updated-series :records updated-records}
            result (:body (update-series (:meta hosts) name updated-record))]
        (logger/debug "Record to update: " (:records (:body request)))
        (logger/debug "Updating records: " updated-record)
        (logger/debug "Result: " result)
        (if (= "ok" (:status result))
          (clc/make-response 200 result)
          (clc/make-response 500 result)))))
  (GET "/series/:name" [name catalog_id_only]
    (let [response (fetch-series (:meta hosts) name catalog_id_only)]
      (if (= "ok" (:status response))
        (clc/make-response 200 response)
        (clc/make-response 500 response))))
  (GET "/series/:name/:season/:episode" [name season episode catalog_id_only]
    (let [response (get-episode (:meta hosts) name season episode catalog_id_only)
          updated-response (if catalog_id_only nil {:records [(update/update-episode (first (:records response)) name (:omdb hosts) apikey)]})]
        (update-episode (:meta hosts) name season episode (dissoc (first (:records updated-response)) :series))
        (clc/make-response 200 (merge response updated-response))))
  (DELETE "/series/:name/:season/:episode" [name season episode]
    (delete-episode (:meta hosts) name season episode))
  (GET "/catalog-id/:catalog_id" [catalog_id fields]
    (if fields
      (let [response (get-by-catalog-id (:meta hosts) catalog_id fields)]
        (if (= "ok" (:status response))
          (clc/make-response 200 response)
          (clc/make-response 400 response)))
      (let [response (get-by-catalog-id (:meta hosts) catalog_id nil)
            record (first (:records response))
            series (:series record)
            season (:season record)
            episode (:episode record)
            updated-response (update/update-episode record series (:omdb hosts) apikey)]
            (if (not= record updated-response)
              (update-episode (:meta hosts) series season episode (dissoc updated-response :series)))
            (if (= "not found" (:status response))
              (clc/make-response 404 {:status "not found"})
              (clc/make-response 200 (merge response {:records [updated-response]}))))))
  (GET "/series" []
    (let [response (get-all-series (:meta hosts))]
      (if (= "ok" (:status response))
        (clc/make-response 200 response)
        (clc/make-response 500 response))))
  (route/not-found (clc/make-response 404 {:status "not found"})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4011"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

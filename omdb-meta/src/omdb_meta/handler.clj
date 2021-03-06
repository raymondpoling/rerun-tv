(ns omdb-meta.handler
  (:require [compojure.core :refer [DELETE GET POST PUT defroutes]]
            [compojure.route :as route]
            [remote-call.meta :as meta]
            [remote-call.omdb :refer [imdb-id-lookup
                                      imdb-series-id-lookup]]
            [omdb-meta.update :as update]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as logger]
            [common-lib.core :as clc]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]]
            [clojure.string :as cls])
  (:gen-class))

(def hosts (clc/make-hosts ["omdb" 8888]
                           ["meta" 4004]))

(def apikey (System/getenv "APIKEY"))

(defroutes app-routes
  (POST "/series/:name{[^/]+}/:season/:episode" [name season episode]
        (fn [request]
          (let [updated-response
                (update/update-episode
                 (merge (:body request)
                        {:season season
                         :episode episode}) name (:omdb hosts) apikey)
                create (:body
                        (meta/create-episode
                         (:meta hosts) name season episode updated-response))]
            (if (= "ok" (:status create))
              (clc/make-response
               200
               (merge create {:records [updated-response]}))
              (clc/make-response 500 create)))))
  (PUT "/series/:name{[^/]+}/:season/:episode" [name season episode]
       (fn [request]
         (let [updated (update/update-episode
                        (merge (:body request)
                               {:season season
                                :episode episode})
                        name (:omdb hosts) apikey)
               update (:body
                       (meta/update-episode
                        (:meta hosts)
                        name season episode updated))]
           (if (= "ok" (:status update))
             (clc/make-response 200 update)
             (clc/make-response 500 update)))))
  (PUT "/series/:name{[^/]+}" [name]
       (fn [request]
         (try
           (let [result (:body (meta/update-series
                                (:meta hosts) name (:body request)))]
             (logger/debug "Record to update: " (:records (:body request)))
             (logger/debug "Result: " result)
             (if (= "ok" (:status result))
               (clc/make-response 200 result)
               (clc/make-response 500 result)))
           (catch Exception e
             (logger/error "Error processing bulk create: " (.getMessage e))))))
  (POST "/series/:name{[^/?]+}" [name]
        (fn [request]
          (try
            (let [omdb (:omdb hosts)
                  updated-series (update/update-series
                                  (:series (:body request)) name omdb apikey)
                  updated-records (map
                                   #(update/update-episode % name omdb apikey)
                                   (:records (:body request)))
                  updated-record {:series updated-series
                                  :records updated-records}
                  result (:body (meta/create-series
                                 (:meta hosts) name updated-record))]
              (logger/debug "Record to update: " (:records (:body request)))
              (logger/debug "Updating records: " updated-record)
              (logger/debug "Result: " result)
              (if (= "ok" (:status result))
                (clc/make-response 200 result)
                (clc/make-response 500 result)))
            (catch Exception e
              (logger/error "Error processing bulk create: "
                            (.getMessage e))))))
  (GET "/series/:name{[^/]+}" [name catalog_id_only]
       (let [response (meta/fetch-series (:meta hosts) name catalog_id_only)]
         (if (= "ok" (:status response))
           (clc/make-response 200 response)
           (clc/make-response 500 response))))
  (GET "/series/:name{[^/]+}/:season/:episode"
       [name season episode catalog_id_only]
       (let [response (meta/get-episode
                       (:meta hosts)
                       name season episode catalog_id_only)
             updated-response (if catalog_id_only
                                nil
                                {:records
                                 [(update/update-episode
                                   (first (:records response))
                                   name (:omdb hosts) apikey)]})]
         (meta/update-episode (:meta hosts)
                              name season episode
                              (dissoc (first (:records updated-response)) :series))
         (clc/make-response 200 (merge response updated-response))))
  (DELETE "/series/:name{[^/]+}/:season/:episode" [name season episode]
          (meta/delete-episode (:meta hosts) name season episode))
  (GET "/catalog-id/:catalog_id" [catalog_id fields]
       (if fields
         (let [response (meta/get-by-catalog-id
                         (:meta hosts) catalog_id fields)]
           (if (= "ok" (:status response))
             (clc/make-response 200 response)
             (clc/make-response 400 response)))
         (let [response (meta/get-by-catalog-id (:meta hosts) catalog_id nil)
               record (first (:records response))
               series (:series record)
               season (:season record)
               episode (:episode record)
               updated-response (update/update-episode
                                 record series (:omdb hosts) apikey)]
           (when (not= record updated-response)
             (meta/update-episode (:meta hosts)
                                  series season episode
                                  (dissoc updated-response :series)))
           (if (= "not found" (:status response))
             (clc/make-response 404 {:status "not found"})
             (clc/make-response
              200
              (merge response {:records [updated-response]}))))))
  (GET "/series" []
       (let [response (meta/get-all-series (:meta hosts))]
         (if (= "ok" (:status response))
           (clc/make-response 200 response)
           (clc/make-response 500 response))))
  (GET "/imdbid/series/:imdbid" [imdbid]
       (let [response (imdb-series-id-lookup (:omdb hosts) apikey imdbid)
             conversion-map (into {} (map
                                      (fn [[a b _]] [b a])
                                      update/series-default-map))
             converted (->> response
                            (map (fn [[k v]]
                                   (vector (or (k conversion-map)
                                               k)
                                           v)))
                            (map (fn [[k v]]
                                   (vector
                                    (keyword
                                     (cls/lower-case (name k)))
                                    v)))
                            (filter (fn [[a _]] (some #(= a %)
                                                      [:title
                                                       :thumbnail
                                                       :summary
                                                       :imdbid
                                                       :name])))
                            (into {}))]
         (logger/debug "Conversion map: " conversion-map)
         (logger/debug "response is: " response)
         (logger/debug "converted is: " converted)
         (if (= "True" (:Response response))
           (clc/make-response 200 {:status :ok :records [converted]})
           (clc/make-response 500 response))))
  (GET "/imdbid/:imdbid" [imdbid]
       (let [response (imdb-id-lookup (:omdb hosts) apikey imdbid)
             conversion-map (into {} (map
                                      (fn [[a b _]] [b a])
                                      update/episode-default-map))
             converted (->> response
                            (map (fn [[k v]]
                                   (vector (or (k conversion-map)
                                               k)
                                           v)))
                            (map (fn [[k v]]
                                   (vector
                                    (keyword
                                     (cls/lower-case (name k)))
                                    v)))
                            (filter (fn [[a _]] (some #(= a %)
                                                      [:title
                                                       :episode_name
                                                       :season
                                                       :episode
                                                       :thumbnail
                                                       :summary
                                                       :imdbid])))
                            (into {}))]
         (logger/debug "Conversion map: " conversion-map)
         (logger/debug "response is: " response)
         (logger/debug "converted is: " converted)
         (if (= "True" (:Response response))
           (clc/make-response 200 {:status :ok :records [converted]})
           (clc/make-response 500 response))))
  (GET "/summary" []
       (clc/make-response 200 (meta/get-summary (:meta hosts))))
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

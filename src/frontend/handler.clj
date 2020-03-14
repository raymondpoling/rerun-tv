(ns frontend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [remote-call.schedule :refer :all]
            [remote-call.schedule-builder :refer [validate-schedule send-schedule]]
            [remote-call.validate :refer [validate-user]]
            [remote-call.format :refer [fetch-playlist]]
            [remote-call.user :refer [fetch-index]]
            [remote-call.playlist :refer [get-playlists fetch-catalog-id]]
            [remote-call.meta :refer [get-all-series bulk-update-series get-meta-by-catalog-id]]
            [ring.util.response :refer [response not-found header status redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [html.preview :refer [make-preview-page]]
            [html.login :refer [login]]
            [html.index :refer [make-index]]
            [html.schedule-builder :refer [schedule-builder]]
            [html.schedule-builder-get :refer [schedule-builder-get]]
            [html.bulk-update :refer [bulk-update]]
            [cheshire.core :refer [parse-string generate-string]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))


(def hosts (clc/make-hosts ["auth" 4007]
                           ["schedule" 4000]
                           ["format" 4009]
                           ["user" 4002]
                           ["playlist" 4001]
                           ["builder" 4003]
                           ["meta" 4004]))

(defn wrap-redirect [function]
  (fn [request]
    (if (not (or (:user (:session request))
            (= "/login" (:uri request))
            (= "/login.html" (:uri request))))
      (do
        (logger/info "Going to login ->")
        (logger/info "uri: " (:uri request) " is matching? " (= "/login" (:uri request)))
        (redirect "/login.html"))
      (do
        (logger/info "continuing request")
        (logger/info "- uri: " (:uri request) " is matching? " (= "/login" (:uri request)))
        (logger/info "- user: " (:user (:session request)))

        (function request)))))

(defn sb [schedule-name schedule-body preview type]
  (let [playlists (get-playlists (:playlist hosts))
        sched (parse-string schedule-body true)
        schedule-name (or (:name sched) schedule-name)
        got-sched (get-schedule (:schedule hosts) schedule-name)
        schedule (if-let [body sched]
          body
          (or got-sched {:name schedule-name, :playlists []}))
        validate (if preview
                    (validate-schedule (:builder hosts) schedule)
                    (send-schedule (:builder hosts) type schedule-name schedule))]
        (println "did it validate? " validate)
        (println "got-sched" got-sched)
    (if (and (= type "Create") got-sched)
      (redirect (str "/schedule-builder.html?message=Schedule with name '" schedule-name "' already exists"))
      (schedule-builder schedule playlists validate type))))

(defroutes app-routes
  (GET "/preview.html" [schedule index idx update reset]
    (fn [request]
      (let [user (:user (:session request))
            schedule-list (get-schedules (:schedule hosts))
            sched (or schedule (first schedule-list))
            idx (or (if reset (fetch-index (:user hosts) user sched true))
                    (not-empty index)
                    idx
                    (fetch-index (:user hosts) user sched true))
            items (get-schedule-items (:schedule hosts) sched idx)
            catalog_ids (map #(fetch-catalog-id (:playlist hosts) (:name %) (:index %)) items)
            meta (flatten (map #(:records (get-meta-by-catalog-id (:meta hosts) (:item %))) catalog_ids))
            records (map merge meta items)]
            (logger/debug (str "with user: " user " index: " idx " and schedule: " sched))
            (println "records: " records)
      (make-preview-page sched schedule-list idx records update))))
  (GET "/login.html" []
     (login))
  (GET "/index.html" []
    (make-index))
  (GET "/schedule-builder.html" [message]
    (let [schedule-names (get-schedules (:schedule hosts))]
      (schedule-builder-get schedule-names message)))
  (POST "/schedule-builder.html" [schedule-name schedule-body preview type]
    (sb schedule-name schedule-body preview type))
  (POST "/login" [username password]
    (if (= "ok" (:status (validate-user (:auth hosts) username password)))
      (-> (redirect "/index.html ")
          (assoc :session {:user username}))
      (redirect "/login.html")))
  (GET "/logout" []
    (-> (redirect "/login.html")
        (assoc :session {:user nil})))
  (GET "/format/:name" [name index update]
    (fn [request]
      (let [username (:user (:session request))
            content-type (:content-type (:header request))]
            (logger/debug (:session request))
            (if (not (nil? username))
              (let [params {:index index :update update}
                    resp (fetch-playlist (:format hosts) username name params)]
                (logger/debug "RESP IS ***" resp "*** RESP IS")
                (logger/debug "header? " (get (:headers resp) "Content-Type"))
                (-> (response (:body resp))
                    (status 200)
                    (header "content-type" (get (:headers resp) "Content-Type"))
                    (header "content-disposition" (get (:headers resp) "Content-Disposition"))))
              (clc/make-response 400 {:status :failure})))))
  (GET "/bulk-update.html" []
    (let [series (get-all-series (:meta hosts))]
      (bulk-update series nil)))
  (POST "/bulk-update.html" [series update]
    (println "update? " update)
      (let [lines (clojure.string/split update #"\n")
            to-map (fn [line]
                      (let [split-line (clojure.string/split line #"\|")]
                            {:season (first split-line)
                              :episode (second split-line)
                              :episode_name (nth split-line 2)
                              :summary (nth split-line 3)}))
            series-list (get-all-series (:meta hosts))
            maps (map to-map lines)
            result (bulk-update-series (:meta hosts) series maps)]
        (println "results - " result)
        (bulk-update series-list result)))
  (route/files "public")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true})
       (wrap-redirect))
     (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4008"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

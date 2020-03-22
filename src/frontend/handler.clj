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
            [remote-call.messages :refer [get-messages add-message]]
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
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))


(def hosts (clc/make-hosts ["auth" 4007]
                           ["schedule" 4000]
                           ["format" 4009]
                           ["user" 4002]
                           ["playlist" 4001]
                           ["builder" 4003]
                           ["omdb" 4011]
                           ["messages" 4010]))

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
  (fn [request]
    (let [playlists (get-playlists (:playlist hosts))
          sched (try (parse-string schedule-body true) (catch Exception e nil))
          schedule-name (or (:name sched) schedule-name)
          got-sched (get-schedule (:schedule hosts) schedule-name)
          schedule (if-let [body sched]
            schedule-body
            (or (not-empty schedule-body) (generate-string (or (not-empty sched) got-sched {:name schedule-name, :playlists []}) {:pretty true})))
            validate  (if preview
                          (validate-schedule (:builder hosts) schedule)
                          (let
                            [result (send-schedule (:builder hosts) type schedule-name schedule)]
                              (if (= "ok" (:status result))
                                (add-message (:messages hosts)
                                   "System"
                                   (str "Schedule " schedule-name " " type "d!")
                                   (str "A schedule has been " (clojure.string/lower-case type) "d by " (:user (:session request)) ", "
                                        (html [:a {:href (str "/preview.html?schedule=" schedule-name)} " check it out!"]))))
                            result))]
          (println "did it validate? "  sched)
          (println "got-sched" got-sched)
      (if (and (= type "Create") got-sched)
        (redirect (str "/schedule-builder.html?message=Schedule with name '" schedule-name "' already exists"))
        (schedule-builder schedule playlists validate type)))))

(defn fetch-preview-frame [schedule-name index]
  (let [items (get-schedule-items (:schedule hosts) schedule-name index)
        catalog_ids (map #(fetch-catalog-id (:playlist hosts) (:name %) (:index %)) items)
        meta (flatten (map #(:records (get-meta-by-catalog-id (:omdb hosts) (:item %))) catalog_ids))]
        (map merge meta items)))

(defroutes app-routes
  (GET "/preview.html" [schedule index idx update reset download]
    (fn [request]
      (let [user (:user (:session request))
            schedule-list (get-schedules (:schedule hosts))
            sched (or schedule (first schedule-list))
            idx (Integer/parseInt (str
              (or (if reset (fetch-index (:user hosts) user sched true))
                (not-empty index)
                idx
                (fetch-index (:user hosts) user sched true))))]
            (logger/debug (str "with user: " user " index: " idx " and schedule: " sched))
      (if download
        (let [params {:index index :update update}
              resp (fetch-playlist (:format hosts) user sched params)]
          (logger/debug "RESP IS ***" resp "*** RESP IS")
          (logger/debug "header? " (get (:headers resp) "Content-Type"))
          (-> (response (:body resp))
              (status 200)
              (header "content-type" (get (:headers resp) "Content-Type"))
              (header "content-disposition" (get (:headers resp) "Content-Disposition"))))
        (let [current (fetch-preview-frame sched idx)
              previous (fetch-preview-frame sched (- idx 1))
              next (fetch-preview-frame sched (+ idx 1))]
          (make-preview-page sched schedule-list idx update previous current next))))))
  (GET "/login.html" []
     (login))
  (GET "/index.html" [start]
    (let [events (get-messages (:messages hosts) start)
          adjusted-dates (map #(merge % {:posted (jt/format "YYYY-MM-dd HH:mm:ssz" (jt/with-zone-same-instant (jt/zoned-date-time (:posted %)) (jt/zone-id)))}) (:events events))]
      (println "events host " (:messages hosts) " events? " adjusted-dates)
      (make-index adjusted-dates)))
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
  (GET "/bulk-update.html" []
    (let [series (get-all-series (:omdb hosts))]
      (bulk-update series nil)))
  (POST "/bulk-update.html" [series update]
    (fn [request]
    (println "update? " update)
        (let [lines (clojure.string/split update #"\n")
              to-map (fn [line]
                        (let [split-line (clojure.string/split line #"\|")]
                              {:season (first split-line)
                                :episode (second split-line)
                                :episode_name (nth split-line 2)
                                :summary (nth split-line 3)}))
              series-list (get-all-series (:omdb hosts))
              maps (map to-map lines)
              result (bulk-update-series (:omdb hosts) series {:records maps})]
          (println "results - " result)
          (if (= "ok" (:status result)) (println "message response: "(add-message (:messages hosts) "System"
              (str (:user (:session request)) " updated " series " with more data!")
              (html [:ol (map (fn [i] [:li (str "S" (:season i) "E" (:episode i) " " (:episode_name i))]) maps)]))))
          (bulk-update series-list result))))
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

(ns frontend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [helpers.routes :refer [hosts with-authorized-roles write-message]]
            [ring.middleware.json :as json]
            [route-logic.message :refer [set-message get-message]]
            [route-logic.bulk-update :as rlbu]
            [route-logic.preview-logic :as rlpl]
            [remote-call.identity :refer [fetch-user fetch-users fetch-roles user-update create-user]]
            [remote-call.schedule :refer :all]
            [remote-call.schedule-builder :refer [validate-schedule send-schedule]]
            [remote-call.locator :refer [get-locations save-locations]]
            [remote-call.validate :refer [validate-user create-auth]]
            [remote-call.format :refer [fetch-playlist]]
            [remote-call.user :refer [fetch-index]]
            [remote-call.playlist :refer [get-playlists fetch-catalog-id]]
            [remote-call.meta :refer [get-all-series
                                      bulk-update-series
                                      get-meta-by-catalog-id
                                      get-series-episodes
                                      save-episode
                                      get-meta-by-imdb-id
                                      get-series-by-imdb-id
                                      create-episode
                                      create-series]]
            [remote-call.messages :refer [get-messages add-message]]
            [ring.util.response :refer [response not-found header status redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [html.series-update :as hsu]
            [html.login :refer [login]]
            [html.index :refer [make-index]]
            [html.schedule-builder :refer [schedule-builder]]
            [html.schedule-builder-get :refer [schedule-builder-get]]
            [html.user-management :refer [user-management]]
            [html.library :refer [make-library]]
            [html.update :refer [make-update-page side-by-side]]
            [html.bulk-update :refer [bulk-update]]
            [helpers.schedule-builder :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn wrap-redirect [function]
  (fn [request]
    (if (not (or (:user (:session request))
            (= "/login" (:uri request))
            (= "/login.html" (:uri request))))
      (do
        (logger/info "Going to login ->")
        (logger/info "uri: " (:uri request) " is matching? " (= "/login" (:uri request)))
        (logger/info "going to login.html")
        (redirect "login.html"))
      (do
        (logger/info "continuing request")
        (logger/info "- uri: " (:uri request) " is matching? " (= "/login" (:uri request)))
        (logger/info "- user: " (:user (:session request)))
        (function request)))))

(defroutes app-routes
  (GET "/message.html" {{:keys [role]} :session}
       (get-message role))
  (POST "/message.html" [title message]
        (set-message title message))
  (GET "/preview.html" [schedule index idx update reset download]
       (fn [{{:keys [user role]} :session}]
         (rlpl/create-preview schedule role user index
                              idx update reset download)))
  (GET "/login.html" []
       (logger/info "getenv host is "  (System/getenv "AUTH_PORT"))
     (login))
  (GET "/index.html" [start]
    (fn [{{:keys [role]} :session}]
      (let [events (get-messages (:messages hosts) start)
            adjusted-dates (map #(merge % {:posted (jt/format "YYYY-MM-dd HH:mm:ssz" (jt/with-zone-same-instant (jt/zoned-date-time (:posted %)) (jt/zone-id)))}) (:events events))]
        (logger/error "events host " (:messages hosts) " events? " adjusted-dates)
        (make-index adjusted-dates role))))
  (GET "/schedule-builder.html" [message]
    (fn [{{:keys [role]} :session}]
      (with-authorized-roles ["admin","media"]
        (let [schedule-names (get-schedules (:schedule hosts))]
          (schedule-builder-get schedule-names message role)))))
  (POST "/schedule-builder.html" [schedule-name schedule-body preview mode]
    (with-authorized-roles ["admin","media"]
      (fn [{{:keys [user role]} :session}]
        (let [playlists (get-playlists (:playlist hosts))
              validity (if preview
                #(validate-schedule (:builder hosts) %)
                #(send-schedule (:builder hosts) mode schedule-name %))
              got-sched (get-schedule (:schedule hosts) schedule-name)
              sched (if (not-empty schedule-body)
                        (make-schedule-string schedule-body validity)
                        (make-schedule-map got-sched validity))]
          (if (and (= "ok" (:status (valid? sched))) (not preview))
            (write-message
               {:author "System"
                :title (str "Schedule " schedule-name " " mode "d!")
                :message (str "A schedule has been " (clojure.string/lower-case mode) "d by " user ", "
                    (html [:a {:href (str "/preview.html?schedule=" schedule-name)} " check it out!"]))}))
          (if (and (= mode "Create") got-sched)
            (redirect (str "/schedule-builder.html?message=Schedule with name '" schedule-name "' already exists"))
            (schedule-builder sched schedule-name playlists mode role))))))
  (POST "/login" [username password]
        (logger/debug "hosts map is? " hosts)
        (let [auth (validate-user
                    (:auth hosts)
                    username
                    password)]
          (logger/debug "auth is: " auth)
          (if (= "ok" (:status auth))
            (let [user (fetch-user (:identity hosts) username)]
              (logger/info "Logged in " username)
              (-> (redirect "/index.html ")
                  (assoc :session (dissoc user :status))))
            (redirect "/login.html"))))
  (GET "/logout" []
    (-> (redirect "/login.html")
        (assoc :session {:user nil})))
  (GET "/bulk-update.html" []
    (with-authorized-roles ["admin","media"]
      (fn [{{:keys [role]} :session}]
        (let [series (get-all-series (:omdb hosts))]
          (bulk-update series nil role)))))
  (POST "/bulk-update.html" [series update create?]
        (with-authorized-roles ["admin","media"]
          (rlbu/bulk-update-logic series update create?)))
  (GET "/user-management.html" []
       (fn [{{:keys [role]} :session}]
         (with-authorized-roles ["admin"]
           (user-management
            (:users (fetch-users (:identity hosts)))
            (:roles (fetch-roles (:identity hosts)))
            role))))
  (POST "/user" [new-user new-email new-role new-password]
        (logger/debug (format "Adding user: %s E-Mail: %s Role: %s" new-user new-email new-role))
        (with-authorized-roles ["admin"]
          (logger/debug (create-user (:identity hosts) new-user new-email new-role))
          (logger/debug (str "Creating "
                        new-user
                        " got "
                        (create-auth (:auth hosts) new-user new-password)))
          (redirect "/user-management.html")))
  (POST "/role" [update-user update-role]
        (logger/debug (format "Updating user: %s Role: %s" update-user update-role))
        (with-authorized-roles ["admin"]
          (logger/debug (user-update (:identity hosts) update-user update-role))
          (redirect "/user-management.html")))
  (GET "/library.html" [series-name]
       (with-authorized-roles ["admin","media","user"]
         (fn [{{:keys [role]} :session}]
           (let [series (get-all-series (:omdb hosts))
                 s-name (or series-name (first series))
                 episodes (get-series-episodes (:omdb hosts) s-name)
                 items (flatten (map #(:records (get-meta-by-catalog-id (:omdb hosts) %)) (:catalog_ids episodes)))
                 items-with-ids (map merge items
                                     (map (fn [c] {:catalog-id c})
                                          (:catalog_ids episodes)))
                 record {:series (first (:records episodes))
                         :records items-with-ids} ]
             (logger/debug "series " s-name " episodes " items)
             (make-library series s-name record role)))))
  (GET "/update-series.html" [series-name]
       (with-authorized-roles ["admin","media"]
         (fn [{{:keys [role]} :session}]
           (let [series
                 (assoc (first
                         (:records
                          (get-series-episodes (:omdb hosts) series-name)))
                        :name series-name)]
             (hsu/make-series-update-page series role)))))
  (POST "/update-series.html" [name imdbid thumbnail summary mode]
        (with-authorized-roles ["admin","media"]
          (fn [{{:keys [role]} :session}]
            (let [series-update {:series {:name name
                                 :imdbid imdbid
                                 :thumbnail thumbnail
                                 :summary summary}}]
              (if (= mode "Save")
                (do
                           (bulk-update-series (:omdb hosts)
                                               name
                                               series-update)
                  (hsu/make-series-update-page (:series series-update) role))
                (let [omdb (first (:records
                                   (get-series-by-imdb-id (:omdb hosts)
                                                  imdbid)))]
                  (hsu/series-side-by-side (:series series-update) omdb role)
              ))))))
  (GET "/update.html" [catalog-id]
       (with-authorized-roles ["admin","media"]
         (fn [{{:keys [role]} :session}]
           (let [episode (first (:records (get-meta-by-catalog-id (:omdb hosts) catalog-id)))
                 files (clojure.string/join "\n" (get-locations (:locator hosts) catalog-id))]
             (make-update-page episode files catalog-id role)))))
  (POST "/update.html"
        [catalog-id series episode_name episode season
         summary imdbid thumbnail files mode]
        (with-authorized-roles ["admin","media"]
          (let [record {:episode_name episode_name
                        :episode episode
                        :season season
                        :summary summary
                        :imdbid imdbid
                        :thumbnail thumbnail}]
            (fn [{{:keys [role]} :session}]
              (if (= "Save" mode)
                (do
                  (logger/debug "RECORD? " record)
                  (logger/debug "SAVE? " (save-episode (:omdb hosts) series record))
                  (save-locations (:locator hosts)
                                  catalog-id
                                  (map clojure.string/trim
                                       (clojure.string/split files #"\n")))
                  (redirect (str "/update.html?catalog-id=" catalog-id)))
                  (let [omdb-record (first (:records (get-meta-by-imdb-id (:omdb hosts) imdbid)))]
                  (side-by-side (assoc record :series series) omdb-record files catalog-id role)))))))
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

(ns frontend.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [cheshire.core :refer :all]
            [helpers.routes :refer [hosts with-authorized-roles write-message]]
            [ring.middleware.json :as json]
            [route-logic.message :refer [set-message get-message]]
            [route-logic.bulk-update :as rlbu]
            [route-logic.preview-logic :as rlpl]
            [remote-call.exception :as except]
            [remote-call.pubsub :as pubsub]
            [remote-call.identity :refer [fetch-user
                                          fetch-users
                                          fetch-roles
                                          user-update
                                          create-user]]
            [remote-call.schedule :refer [get-schedule get-schedules]]
            [remote-call.schedule-builder :refer [validate-schedule
                                                  send-schedule]]
            [remote-call.tags :refer [fetch-all-tags
                                      fetch-tags
                                      add-tags
                                      delete-tags]]
            [remote-call.locator :refer [get-locations save-locations]]
            [remote-call.validate :refer [validate-user create-auth]]
            [remote-call.playlist :refer [get-playlists]]
            [remote-call.meta :refer [get-all-series
                                      bulk-update-series
                                      get-meta-by-catalog-id
                                      get-series-episodes
                                      save-episode
                                      get-meta-by-imdb-id
                                      get-series-by-imdb-id
                                      get-summary]]
            [remote-call.messages :refer [get-messages]]
            [ring.util.response :refer [redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.tools.logging :as logger]
            [html.series-update :as hsu]
            [html.login :refer [login]]
            [html.index :refer [make-index]]
            [html.exception :refer [exception-page]]
            [html.schedule-builder :refer [schedule-builder]]
            [html.schedule-builder-get :refer [schedule-builder-get]]
            [html.user-management :refer [user-management]]
            [html.library :refer [make-library]]
            [html.update :refer [make-update-page side-by-side]]
            [html.bulk-update :refer [bulk-update]]
            [helpers.schedule-builder :refer [make-schedule-map
                                              make-schedule-string
                                              valid?]]
            [hiccup.core :refer [html]]
            [java-time :as jt]
            [taoensso.carmine.ring :refer [carmine-store]]
            [org.httpkit.server :refer [run-server]]
            [clojure.string :as cls]
            [clojure.data :as cld])
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
        (logger/info "session? " (:session request))
        (logger/info "request? " request)
        (function request)))))

(defroutes app-routes
  (GET "/message.html" {{:keys [role]} :session}
       (get-message role))
  (POST "/message.html" [title message]
        (set-message title message))
  (GET "/preview.html" [schedule index idx update reset
                        download select-format]
       (fn [{{:keys [user role]} :session}]
         (rlpl/create-preview schedule role user index
                              idx update reset download
                              select-format)))
  (GET "/login.html" []
       (logger/info "getenv host is "  (System/getenv "AUTH_PORT"))
       (login))
  (GET "/index.html" [start]
       (fn [{{:keys [role]} :session}]
         (let [schedules (count (get-schedules (:schedule hosts)))
               summary (merge (get-summary (:omdb hosts))
                              {:schedules schedules})
               events (get-messages (:messages hosts) start)
               adjusted-dates (map
                               #(merge % {:posted
                                          (jt/format "YYYY-MM-dd HH:mm:ssz"
                                                     (jt/with-zone-same-instant
                                                       (jt/zoned-date-time
                                                        (:posted %))
                                                       (jt/zone-id)))})
                               (:events events))]
           (logger/error "events host " (:messages hosts) " events? " adjusted-dates)
           (make-index adjusted-dates role summary))))
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
                      (if (nil? got-sched)
                        (make-schedule-map {:name schedule-name
                                               :playlists
                                               []} validity)
                        (make-schedule-map got-sched validity)))]
          (when (and (= "ok" (:status (valid? sched))) (not preview))
            (write-message
               {:author "System"
                :title (str "Schedule " schedule-name " " mode "d!")
                :message (str "A schedule has been " (cls/lower-case mode) "d by " user ", "
                              (html [:a {:href
                                         (str "/preview.html?schedule="
                                              schedule-name)}
                                     " check it out!"]))}))
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
         (fn [{{:keys [role user]} :session}]
           (let [series
                 (assoc (first
                         (:records
                          (get-series-episodes (:omdb hosts) series-name)))
                        :name series-name)
                 tags (fetch-tags (:tags hosts) (:catalog_id series)
                                  :author? user
                                  :type? "SERIES")]
             (logger/debug "series is: " series)
             (hsu/make-series-update-page series tags role)))))
  (POST "/update-series.html" [name imdbid thumbnail summary
                               mode catalog_id tags]
        (with-authorized-roles ["admin","media"]
          (fn [{{:keys [role user]} :session}]
            (let [series-update {:series {:name name
                                          :imdbid imdbid
                                          :thumbnail thumbnail
                                          :summary summary}}
                  left (set (fetch-tags (:tags hosts) catalog_id
                                   :author? user
                                   :type? "SERIES"))
                  right (if tags (set (map cls/trim (cls/split tags #",")))
                            #{})
                  diffs (cld/diff left right)
                  patterns [#".{7}" #".{7}.{2}" #".{7}.{2}.{3}"]
                  catalog-ids (filter some?
                                      (map #(re-find % catalog_id)
                                           patterns))]
              (if (= mode "Save")
                (do
                  (bulk-update-series (:omdb hosts)
                                      name
                                      series-update)
                  (when (second diffs)
                    (add-tags    (:tags hosts) user (second diffs) catalog-ids))
                  (when (first diffs)
                    (delete-tags (:tags hosts) user (first diffs) catalog_id))
                  (redirect (str "/update-series.html?series-name=" name)))
                (let [omdb (first (:records
                                   (get-series-by-imdb-id (:omdb hosts)
                                                          imdbid)))]
                  (hsu/series-side-by-side (:series series-update) omdb tags role)
                  ))))))
  (GET "/update.html" [catalog-id]
       (with-authorized-roles ["admin","media"]
         (fn [{{:keys [role user]} :session}]
           (let [episode (first (:records (get-meta-by-catalog-id
                                           (:omdb hosts)
                                           catalog-id)))
                 files (cls/join "\n"
                                 (get-locations (:locator hosts)
                                                catalog-id))
                 tags (fetch-tags (:tags hosts) catalog-id
                                  :author? user
                                  :type? "EPISODE")]
             (make-update-page episode files catalog-id tags role)))))
  (GET "/exception.html" []
       (with-authorized-roles ["admin","media"]
         (fn [{{:keys [role]} :session}]
           (let [exception-host (:exception hosts)
                 all-tests (except/get-all-tests
                            exception-host)
                 all-results (map
                              #(vector
                                %
                                (except/get-test-results exception-host %))
                              all-tests)]
             (println "Results: " all-tests)
             (exception-page all-results role)))))
  (POST "/exception.html" [test args]
        (with-authorized-roles ["admin","media"]
          (fn [{{:keys [role]} :session}]
            (let [exception-host (:exception hosts)
                  reverse-map (into {} (map (fn [[a b]] [b a]) pubsub/tests))
                  test_key (get reverse-map test)
                  arguments (or (filter not-empty
                                        (map cls/trim
                                             (cls/split args #",")))
                                [])
                  all-tests (except/get-all-tests
                             exception-host)
                  all-results (map
                               #(vector
                                 %
                                 (except/get-test-results exception-host %))
                               all-tests)]
              (println "Reverse map: " reverse-map)
              (println "Results: " test test_key arguments)
              (pubsub/run-tests [test_key arguments])
              (redirect "/exception.html")))))
  (POST "/update.html"
        [catalog-id series episode_name episode season
         summary imdbid thumbnail files mode tags]
        (with-authorized-roles ["admin","media"]
          (fn [{{:keys [role user]} :session}]
            (let [record {:episode_name episode_name
                          :episode episode
                          :season season
                          :summary summary
                          :imdbid imdbid
                          :thumbnail thumbnail}
                  left (set (fetch-tags (:tags hosts) catalog-id
                                        :author? user
                                        :type? "EPISODE"))
                  right (if tags (set (map cls/trim (cls/split tags #",")))
                            #{})
                  diffs (cld/diff left right)
                  patterns [#".{7}" #".{7}.{2}" #".{7}.{2}.{3}"]
                  catalog-ids (filter some?
                                      (map #(re-find % catalog-id)
                                           patterns))]
              (logger/debug "diffs: " left right "\n" diffs)
              (if (= "Save" mode)
                (do
                  (logger/debug "RECORD? " record)
                  (logger/debug "SAVE? " (save-episode (:omdb hosts) series record))
                  (save-locations (:locator hosts)
                                  catalog-id
                                  (map cls/trim
                                       (cls/split files #"\n")))
                  (pubsub/run-tests [:root_locations [series]]
                                    [:locations [series]])
                   (when (second diffs)
                    (add-tags    (:tags hosts) user (second diffs) catalog-ids))
                  (when (first diffs)
                    (delete-tags (:tags hosts) user (first diffs) catalog-id))
                  (redirect (str "/update.html?catalog-id=" catalog-id)))
                (let [omdb-record (first (:records
                                          (get-meta-by-imdb-id
                                           (:omdb hosts)
                                           imdbid)))]
                  (side-by-side (assoc record :series series)
                                omdb-record
                                files
                                catalog-id
                                tags
                                role)))))))
  (route/files "public")
  (route/not-found "Not Found"))

(def redis-var "REDIS_URI")

(def server-conn1 {:pool {} :spec {:uri  (System/getenv redis-var)}})

(def app
  (wrap-defaults
   (->
    app-routes
    (json/wrap-json-response)
    (json/wrap-json-body {:keywords? true})
    (wrap-redirect))
   (->
    site-defaults
    (assoc-in [:security :anti-forgery] false)
    (#(if (not-empty (System/getenv redis-var))
        (assoc-in %
                  [:session :store]
                  (carmine-store server-conn1))
        %)))))


(defn -main []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4008"))]
    (run-server app {:port port})
    (logger/info "Connecting to redis server: "
             (System/getenv redis-var))
    (logger/info (str "Listening on port " port))))

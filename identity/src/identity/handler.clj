(ns identity.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [ring.util.response :refer [not-found]]
            [db.db :refer :all]
            [clojure.tools.logging :as logger]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [common-lib.core :as clc]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defroutes app-routes
  (POST "/user/:user" [user]
    (fn [{{:keys [role email] :or {role "user"}} :body}]
      (let [role_id (find-role role)]
        (try
          (if (and email role_id)
            (do
              (new-user user role email)
              (clc/make-response 200 {:status :ok}))
            (let [message (map #(and (not %1) %2) [email role_id]
                ["user must have an email address"
                 (str "role [" role "] does not exist")])]
            (clc/make-response 400 {:status "failed" :message (first (filter #(not= false %) message))})))
        (catch java.sql.SQLException e
          (logger/error (str "could not create user '" user "': " (.getMessage e)))
          (clc/make-response 400 {:status "failed" :message "user already exists"}))))))
  (GET "/user/:user" [user]
    (let [user (find-user user)]
      (if user
        (clc/make-response 200 (merge {:status :ok} user))
        nil))) ; if not found, not found falls through
  (PUT "/user/:user" [user]
    (fn [{{:keys [role] :or {role "user"}} :body}]
      (let [role_id (find-role role)]
        (try
          (if role_id
            (do
              (update-user user role)
              (clc/make-response 200 {:status :ok}))
            (clc/make-response 400 {:status :failed :message (str "role [" role "] does not exist")}))
        (catch java.sql.SQLException e
          (logger/error (str "could not update '" user "': " (.getMessage e)))
          (clc/make-response 400 {:status "failed" :message "user could not be updated"}))))))
  (POST "/role/:role" [role]
    (try
      (add-role role)
      (clc/make-response 200 {:status :ok})
    (catch java.sql.SQLException e
      (logger/error (str "could not create role '" role "': " (.getMessage e)))
      (clc/make-response 400 {:status "failed" :message "cannot create role"}))))
  (GET "/role" []
    (try
      (let [roles (find-roles)]
        (clc/make-response 200 {:status :ok :roles roles}))
    (catch java.sql.SQLException e
      (logger/error (str "could not get list of roles: " (.getMessage e)))
      (clc/make-response 500 {:status "failed" :message "server error"}))))
  (route/not-found (clc/make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "identity_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4012"))]
    (run-server app {:port port})
    (logger/info (str "Listening on port " port))))

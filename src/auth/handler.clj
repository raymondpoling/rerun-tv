(ns auth.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [ring.util.response :refer [response not-found header status]]
            [db.db :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(defroutes app-routes
  (POST "/new/:user" [user]
    (fn [request]
      (let [password (:password (:body request))
            valid? (not (nil? password))]
          (try
            (new-user user password)
            (make-response 200 {:status :ok})
          (catch java.sql.SQLException e
            (make-response 400 {:status :could-not-create}))))))
  (POST "/validate/:user" [user]
    (fn [request]
      (let [password (:password (:body request))
            valid? (verify-password user password)]
            (if valid?
              (make-response 200 {:status :ok})
              (make-response 400 {:status :invalid-credentials})))))
  (POST "/update/:user" [user]
    (fn [request]
      (let [password (:old-password (:body request))
            new-password (:new-password (:body request))
            valid? (and (verify-password user password) (not (nil? new-password)))]
            (if valid?
              (do
                (change-password user new-password)
                (make-response 200 {:status :ok}))
              (make-response 400 {:status :invalid-credentials})))))

  (route/not-found (make-response 404 {:status :not-found})))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "auth_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4007"))]
    (run-server app {:port port})
    (println (str "Listening on port " port))))

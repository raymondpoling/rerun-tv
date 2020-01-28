(ns user.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.json :as json]
            [db.db :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))


(defroutes app-routes
  (POST "/:user" [user]
    (let [created? (insert-user user)]
      (if (nil? created?)
        (make-response 400 {:status :failed})
        (make-response 200 {:status "ok"}))))
  (DELETE "/:user" [user]
    (if (not (nil? (delete-user user)))
      (make-response 200 {:status "ok"})
      (make-response 400 {:status :failed})))
  (GET "/:user/:schedule" [user schedule]
    (let [value (get-and-update user schedule)]
      (if (nil? value)
        (make-response 404 {:status "not found"})
        (make-response 200 {:idx value}))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults
    (->
       app-routes
       (json/wrap-json-response)
       (json/wrap-json-body {:keywords? true}))
      (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main []
  (let [user (or (System/getenv "DB_USER") "user_user")
        password (or (System/getenv "DB_PASSWORD") "")
        host (or (System/getenv "DB_HOST") "localhost")
        port (Integer/parseInt (or (System/getenv "DB_PORT") "3306"))]
        (initialize user password host port))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "4002"))]
    (run-server app {:port port})
    (println (str "Listening on port " port))))

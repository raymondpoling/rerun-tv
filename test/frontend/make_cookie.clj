(ns frontend.make-cookie
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all])
  (:use clj-http.fake))

(defn extract [request key]
  (-> request
      :body
      slurp
      parse-string
      (get key)))

(defn make-cookie [role]
  (with-fake-routes-in-isolation
    {(str "http://auth:4007/validate/" role)
     (fn [_]
       {:headers {:content-type
                  "application/json"}
        :body (generate-string
               {:status :ok})})
     (str "http://identity:4012/user/" role)
     (fn [_]
       {:headers {:content-type
                  "application/json"}
        :body (generate-string
               {:status :ok
                :user role
                :email "auth@localhost"
                :role role})})}
    (let [response (app (-> (mock/request :post "/login")
                            (mock/body {:password ""
                                        :username role})))
          cookie-string (first (get (:headers response) "Set-Cookie"))
          [_ cookie] (re-find #"([^;]*);.*" cookie-string)]
      (fn [request] (mock/header request "Cookie" cookie)))))

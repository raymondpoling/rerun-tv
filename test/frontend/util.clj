(ns frontend.util
  (:require
   [ring.mock.request :as mock]
   [clojure.test :refer [is testing]]
   [cheshire.core :refer [generate-string parse-string]]
   [frontend.handler :refer [app]]
   [clojure.tools.logging :as logger]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]
   [clojure.string :as cls]))

(defmacro testing-with-log-markers [string & body]
  `(testing ~string
     (logger/debug "starting " ~string)
     ~@body
     (logger/debug "ending " ~string)))

(defn make-response [response]
  {:headers {:content-type "application/json"}
   :body (generate-string response)})

(defn basic-matcher [match body]
  (re-matches
   (re-pattern
    (str "(?s).*"
         match
         ".*"))
   body))

(defn each-line-and-combined [text & body]
  (doall (map #(is (basic-matcher % text)) body))
  (reduce #(do
             (is (basic-matcher (format "%s.*%s" %1 %2) text))
             %2)
          body)
  (is (basic-matcher (cls/join ".*" body) text)))

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

(ns db.db
  (:require [clojure.java.jdbc :as j]))

(def database (atom {:dbtype "mysql"
               :dbname "auth"
               :user nil
               :password nil}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "hsql" :dbname "auth"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))

(defn new-user [user password]
  (j/insert! @database "auth.authorize" {:user user :password password}))

(defn verify-password [user password]
  (let [r (j/query @database
            ["SELECT authorize.user
            FROM auth.authorize
            WHERE authorize.user = ? AND password = ?"
             user password ])]
  (= 1 (count r))))

(defn change-password [user password]
  (j/update! @database "auth.authorize" {:password password} ["authorize.user = ?" user]))

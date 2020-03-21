(ns db.db
  (:require [clojure.java.jdbc :as j])
  (:import java.security.MessageDigest))

(def database (atom {:dbtype "mysql"
               :dbname "auth"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "auth"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))

(defn sha256 [salt]
  (fn [password]
    (let [string (str password salt)
        digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
        (apply str (map (partial format "%02x") digest)))))

(def salt-password (sha256 (or (System/getenv "SALT") "SALTED")))

(defn new-user [user password]
  (let [pass (salt-password password)]
    (j/insert! @database "auth.authorize" {:user user :password pass})))

(defn verify-password [user password]
  (let [pass (salt-password password)
        r (j/query @database
            ["SELECT authorize.user
            FROM auth.authorize
            WHERE authorize.user = ? AND password = ?"
             user pass ])]
  (= 1 (count r))))

(defn change-password [user password]
  (let [pass (salt-password password)]
    (j/update! @database "auth.authorize" {:password pass} ["authorize.user = ?" user])))

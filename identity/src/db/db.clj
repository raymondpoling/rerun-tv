(ns db.db
  (:require [clojure.java.jdbc :as j])
  (:import java.security.MessageDigest))

(def database (atom {:dbtype "mysql"
               :dbname "identity"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "identity"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))

(defn find-role [role]
  (:id (first (j/query @database ["SELECT id FROM identity.role WHERE role = ?" role]))))

(defn new-user [user role email]
  (let [role_id (find-role role)]
    (j/insert! @database "identity.identity" {:user user :role_id role_id :email email})))

(defn find-user [user]
  (first (j/query @database [
    "SELECT user, role, email
    FROM identity.identity
    JOIN identity.role
    ON role.id = identity.role_id
    WHERE user = ?" user])))

(defn find-users []
  (j/query @database [
    "SELECT user, role, email
    FROM identity.identity
    JOIN identity.role
    ON role.id = identity.role_id"]))

(defn update-user [user role]
  (let [role_id (find-role role)]
    (j/update! @database "identity.identity" {:role_id role_id} ["user = ?" user])))

(defn add-role [role]
  (j/insert! @database "identity.role" {:role role}))

(defn find-roles []
  (map :role (j/query @database ["SELECT role FROM identity.role"])))

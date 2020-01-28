(ns db.db
  (:require [clojure.java.jdbc :as j]))

(def database (atom {:dbtype "mysql"
               :dbname "user"
               :user nil
               :password nil}))

(defn initialize
 ([name password]
   (swap! database merge {:user name :password password}))
 ([name password host port]
   (swap! database merge {:user name :password password :host host :port port})))

(defn insert-user [username]
  (j/insert! @database "user.user" {:username username}))

(defn delete-user [username]
  (j/delete! @database "user.user" ["username = ?" username]))

(defn get-and-update [username schedule]
  (j/with-db-transaction [db @database]
    (let [value (:idx (first (j/query db [(str "SELECT idx FROM user.index "
                                               "WHERE schedule = ? AND "
                                               "user_id = (SELECT id FROM "
                                               "user.user WHERE username = ?)")
                                               schedule username])))]
    (if (nil? value)
      (let [user_id (:id (first (j/query db ["SELECT id FROM user.user WHERE username = ?"
                              username])))]

        (try
          (j/insert! db "user.index" {:user_id user_id :schedule schedule :idx 1})
          0
        (catch java.sql.SQLIntegrityConstraintViolationException e
          nil)))
      (do
        (j/update! db "user.index" {:idx (+ 1 value)}
                  [(str "schedule = ? AND user_id = "
                        "(SELECT id FROM user.user "
                        "WHERE username = ?)") schedule username])
        value)))))

(ns db.db
  (:require [clojure.java.jdbc :as j]))

(def database (atom {:dbtype "mysql"
               :dbname "user2"
               :user nil
               :password nil}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "hsql" :dbname "playlist"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))


(defn insert-user [username]
  (j/insert! @database "user2.user" {:username username}))

(defn delete-user [username]
  (j/delete! @database "user2.user" ["username = ?" username]))

(defn get-and-update [username schedule]
  (j/with-db-transaction [db @database]
    (let [value (:idx (first (j/query db [(str "SELECT idx FROM user2.index "
                                               "WHERE schedule = ? AND "
                                               "user_id = (SELECT id FROM "
                                               "user2.user WHERE username = ?)")
                                               schedule username])))]
    (if (nil? value)
      (let [user_id (:id (first (j/query db ["SELECT id FROM user2.user WHERE username = ?"
                              username])))]

        (try
          (j/insert! db "user2.index" {:user_id user_id :schedule schedule :idx 1})
          0
        (catch java.sql.SQLIntegrityConstraintViolationException e
          nil)))
      (do
        (j/update! db "user2.index" {:idx (+ 1 value)}
                  [(str "schedule = ? AND user_id = "
                        "(SELECT id FROM user2.user "
                        "WHERE username = ?)") schedule username])
        value)))))

(defn get-index [username schedule]
  (:idx (first (j/query @database [(str "SELECT idx FROM user2.index "
                                       "WHERE schedule = ? AND "
                                       "user_id = (SELECT id FROM "
                                       "user2.user WHERE username = ?)")
                                       schedule username]))))

(defn update-user-schedule-index [username schedule index]
  (= 1 (first (j/update! @database "user2.index" {:idx index}
            [(str "schedule = ? AND user_id = "
                  "(SELECT id FROM user2.user "
                  "WHERE username = ?)") schedule username]))))

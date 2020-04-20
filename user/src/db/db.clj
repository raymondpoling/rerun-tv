(ns db.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as logger]))

(def database (atom {:dbtype "mysql"
               :dbname "user2"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "user2"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))


(defn insert-user [username]
  (j/insert! @database "user2.user" {:username username}))

(defn delete-user [username]
  (j/delete! @database "user2.user" ["username = ?" username]))

(defn get-and-update [username schedule index preview]
  (j/with-db-transaction [db @database]
    (let [value (:idx (first (j/query db [(str "SELECT idx FROM user2.index "
                                               "WHERE schedule = ? AND "
                                               "user_id = (SELECT id FROM "
                                               "user2.user WHERE username = ?)")
                                               schedule username])))]
   (if preview
     (or value index 0)
     (if (nil? value)
       (let [user_id (:id
                      (first
                       (j/query
                        db
                        ["SELECT id FROM user2.user WHERE username = ?"
                         username])))]
         (try
           (logger/debug
            "inserting new schedule use?"
            (j/insert! db "user2.index" {:user_id user_id
                                         :schedule schedule
                                         :idx (or index 1)}))
           (or index 0)
           (catch java.sql.SQLIntegrityConstraintViolationException e
             (logger/error "sql error: " e))))
       (do
         (j/update! db "user2.index" {:idx (or index (+ 1 value))}
                    [(str "schedule = ? AND user_id = "
                          "(SELECT id FROM user2.user "
                          "WHERE username = ?)") schedule username])
         (or value)))))))


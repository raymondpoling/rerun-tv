(ns user.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]
    [cheshire.core :refer :all]
    [cheshire.generate :refer [add-encoder encode-str remove-encoder]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA user2"])
  (j/execute! @database (j/create-table-ddl "user2.user"
    [[:id :SERIAL]
    [:username "VARCHAR(20)"]]
    "PRIMARY KEY (id)"))
  (j/execute! @database ["CREATE INDEX by_user ON user2.user(username)"])
  (j/execute! @database (j/create-table-ddl "user2.index"
    [[:user_id "BIGINT(20) UNSIGNED NOT NULL"]
    [:idx "INTEGER UNSIGNED NOT NULL"]
    [:schedule "VARCHAR(20) NOT NULL"]
    ["PRIMARY KEY (user_id, schedule)"]
    ["FOREIGN KEY (user_id) REFERENCES user2.user(id) ON DELETE CASCADE"]])))

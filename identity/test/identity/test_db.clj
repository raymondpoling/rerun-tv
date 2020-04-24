(ns identity.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA IDENTITY"])
  (j/execute! @database (j/create-table-ddl "identity.role"
    [[:id :SERIAL]
    [:role "CHAR(10) NOT NULL UNIQUE"]]))
  (j/execute! @database (j/create-table-ddl "identity.identity"
    [[:id :SERIAL]
     [:user "VARCHAR(20) NOT NULL UNIQUE"]
     [:email "VARCHAR(255) NOT NULL UNIQUE"]
     [:role_id "BIGINT(20) UNSIGNED NOT NULL"]
     ["FOREIGN KEY (role_id) REFERENCES identity.role(id) ON DELETE CASCADE"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_user ON identity.identity(user)"])
  (j/insert-multi! @database "identity.role" [:role] [["admin"]["user"]["media"]]))

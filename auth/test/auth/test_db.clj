(ns auth.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA AUTH"])
  (j/execute! @database (j/create-table-ddl "auth.authorize"
    [[:id :SERIAL]
    [:user "VARCHAR(20)"]
    [:password "CHAR(64)"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_user ON auth.authorize(user)"]))

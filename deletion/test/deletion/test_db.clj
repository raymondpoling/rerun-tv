(ns deletion.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA DELETION"])
  (j/execute!
   @database
   (j/create-table-ddl
    "deletion.record"
    [[:id :SERIAL]
     [:name "VARCHAR(150) NOT NULL"]
     [:type "VARCHAR(20) NOT NULL"]
     [:maker "VARCHAR(20) NOT NULL"]
     [:checker "VARCHAR(20)"]
     [:reason1 "VARCHAR(150)"] ; Maker reason
     [:reason2 "VARCHAR(150)"] ; Checker reason
     [:status "CHAR(3) NOT NULL"]]))
  (j/execute! @database ["CREATE INDEX by_status ON deletion.record(status)"]))

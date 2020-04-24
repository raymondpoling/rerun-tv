(ns messages.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA MESSAGES"])
  (j/execute! @database (j/create-table-ddl "messages.message"
    [[:message_number :SERIAL]
    [:author "VARCHAR(20)"]
    [:posted :DATETIME]
    [:title "VARCHAR(200)"]
    [:information "VARCHAR(100)"]]))
  (j/execute! @database ["CREATE INDEX inverse ON messages.message(message_number DESC)"]))

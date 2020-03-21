(ns schedule.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]
    [cheshire.core :refer :all]
    [cheshire.generate :refer [add-encoder encode-str remove-encoder]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA SCHEDULE"])
  (j/execute! @database (j/create-table-ddl "schedule.schedule"
    [[:id :SERIAL]
    [:name "VARCHAR(20)"]
    [:schedule "VARCHAR(200)"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_name ON schedule.schedule(name)"]))

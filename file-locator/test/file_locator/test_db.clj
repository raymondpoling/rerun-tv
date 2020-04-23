(ns file-locator.test-db
  (:require [clojure.java.jdbc :as j]
    [db.db :refer [database]]))

(defn create-h2-mem-tables []
  (j/execute! @database ["CREATE SCHEMA FILE_LOCATOR"])
  (j/execute! @database (j/create-table-ddl "file_locator.hosts"
    [[:id :SERIAL]
    [:host "VARCHAR(20) NOT NULL"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_host ON file_locator.hosts(host)"])
  (j/execute! @database (j/create-table-ddl "file_locator.protocols"
    [[:id :SERIAL "PRIMARY KEY"]
     [:protocol "VARCHAR(10)"]]))
  (j/execute! @database (j/create-table-ddl "file_locator.catalog_ids"
    [[:id :SERIAL "PRIMARY KEY"]
     [:catalog_id "CHAR(12) NOT NULL"]]))
  (j/execute! @database ["CREATE UNIQUE INDEX by_catalog_id ON file_locator.catalog_ids(catalog_id)"])
  (j/execute! @database (j/create-table-ddl "file_locator.urls"
    [[:id :SERIAL "PRIMARY KEY"]
     [:host_id "BIGINT(20) UNSIGNED NOT NULL"]
     [:protocol_id "BIGINT(20) UNSIGNED NOT NULL"]
     [:catalog_id "BIGINT(20) UNSIGNED NOT NULL"]
     [:path "VARCHAR(256) NOT NULL"]
     ["FOREIGN KEY (host_id) REFERENCES file_locator.hosts(id)"]
     ["FOREIGN KEY (protocol_id) REFERENCES file_locator.protocols(id)"]
     ["FOREIGN KEY (catalog_id) REFERENCES file_locator.catalog_ids(id)"]]))
   (j/execute! @database ["CREATE UNIQUE INDEX by_host_protocol_catalog ON file_locator.urls(catalog_id,host_id,protocol_id)"]))

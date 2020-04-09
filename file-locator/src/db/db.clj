(ns db.db
  (:require
    [clojure.java.jdbc :as j]
    [cheshire.core :refer :all]))

(def database (atom {:dbtype "mysql"
               :dbname "file_locator"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
 ([]
   (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "file_locator"}))
 ([name password host port]
   (swap! database merge {:user name :password password :host host :port port})))

(defn get-id [record]
 (or (:generated_key record)
     (:id record)))

(defn find-or-insert-host [host]
  (let [id (get-id (first (j/query @database ["SELECT id FROM file_locator.hosts WHERE host = ?" host])))]
    (if (nil? id)
        (get-id (first (j/insert! @database "file_locator.hosts" {:host host})))
        id)))

(defn find-or-insert-protocol [protocol]
  (let [id (get-id (first (j/query @database ["SELECT id FROM file_locator.protocols WHERE protocol = ?" protocol])))]
    (if (nil? id)
        (get-id (first (j/insert! @database "file_locator.protocols" {:protocol protocol})))
        id)))

(defn find-or-insert-catalog-id [catalog-id]
  (let [id (get-id (first (j/query @database ["SELECT id FROM file_locator.catalog_ids WHERE catalog_id = ?" catalog-id])))]
    (if (nil? id)
        (get-id (first (j/insert! @database "file_locator.catalog_ids" {:catalog_id catalog-id})))
        id)))

(defn insert-url [protocol_id host_id catalog_id path]
  (j/insert! @database "file_locator.urls"
             {:protocol_id protocol_id
              :host_id host_id
              :catalog_id catalog_id
              :path path}))

(defn fetch-url [protocol host catalog_id]
  (:path (first (j/query @database ["SELECT path
                                    FROM file_locator.urls
                                    JOIN file_locator.hosts
                                    ON urls.host_id = hosts.id
                                    JOIN file_locator.protocols
                                    ON urls.protocol_id = protocols.id
                                    JOIN file_locator.catalog_ids
                                    ON urls.catalog_id = catalog_ids.id
                                    WHERE catalog_ids.catalog_id =?
                                    AND hosts.host = ?
                                    AND protocols.protocol = ?" catalog_id host protocol]))))

(defn get-by-catalog-id [catalog-id]
  (map :url
       (j/query @database ["SELECT concat(protocol,'://',host,path) AS url
                           FROM file_locator.urls
                           JOIN file_locator.hosts
                           ON urls.host_id = hosts.id
                           JOIN file_locator.protocols
                           ON urls.protocol_id = protocols.id
                           JOIN file_locator.catalog_ids
                           ON urls.catalog_id = catalog_ids.id
                           WHERE catalog_ids.catalog_id =?" catalog-id])))
                                    
(defn insert-or-update-url [protocol_id host_id catalog_id path]
  (j/with-db-transaction [db @database]
    (let [exists? (not-empty
                   (j/query db
                            ["SELECT path FROM file_locator.urls WHERE protocol_id = ? AND host_id = ? AND catalog_id = ?"
                             protocol_id host_id catalog_id]))]
      (if exists?
        (j/update! db "file_locator.urls" {:path path} ["host_id = ? AND protocol_id = ? AND catalog_id = ?" host_id protocol_id catalog_id])
        (insert-url protocol_id host_id catalog_id path)))))


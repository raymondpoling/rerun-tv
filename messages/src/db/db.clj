(ns db.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as logger]))

(def database (atom {:dbtype "mysql"
               :dbname "messages"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
  ([]
    (swap! database (fn [_ s] s) {:dbtype "hsql" :dbname "messages"}))
  ([name password host port]
    (swap! database merge {:user name :password password :host host :port port})))

(defn save-event [author posted title information]
  (logger/debug "author: " author " posted: " posted " title " title " information " information)
  (j/insert! @database "messages.message"
    {:author author :posted posted :title title :information information}))

(defn get-events [start number]
  (println "What is start? " start " type " (type start))
  (j/query @database
    (if start
      ["SELECT message_number, author, posted, title, information
        FROM messages.message
        WHERE ? > message_number
        ORDER BY message_number DESC
        LIMIT ?"
        (Integer/parseInt start)
        (Integer/parseInt number)]
      ["SELECT message_number, author, posted, title, information
        FROM messages.message
        ORDER BY message_number DESC
        LIMIT ?"
        (Integer/parseInt number)])))

(ns db.db
  (:require [clojure.java.jdbc :as j]
    [cheshire.core :refer :all]))

(def database (atom {:dbtype "mysql"
               :dbname "schedule"
               :user nil
               :password nil}))

(defn initialize [name password]
  (swap! database assoc :user name)
  (swap! database assoc :password password))

(defn find-schedule [name]
  (let [sched
          (j/query @database
            ["SELECT schedule FROM schedule.schedule WHERE name = ?" name])]
  (parse-string (:schedule (first sched)) true)))

(defn insert-schedule [name schedule]
  (j/insert! @database "schedule.schedule"
    {:name name :schedule (generate-string schedule)}))

(defn update-schedule [name schedule]
  (j/update! @database "schedule.schedule"
    {:schedule (generate-string schedule)} ["name = ?" name]))

(defn delete-schedule [name]
  (j/delete! @database "schedule.schedule" ["name = ?" name]))

;"{\"name\":\"test-found\",\"playlists\":[{\"name\":\"cats\",\"type\":\"playlist\",\"length\":15},{\"name\":\"dogs\",\"type\":\"playlist\",\"length\":17}]}"
;"{\"name\":\"test-found\",\"playlists\":[{\"name\":\"cats\",\"length\":12,\"type\":\"playlist\"},{\"name\":\"dogs\",\"length\":13,\"type\":\"playlist\"}]}"

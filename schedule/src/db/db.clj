(ns db.db
  (:require [clojure.java.jdbc :as j]
    [cheshire.core :refer [generate-string parse-string]]))

(def database (atom {:dbtype "mysql"
               :dbname "schedule"
               :user nil
               :password nil
               :serverTimezone "America/New_York"}))

(defn initialize
([]
  (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "schedule"}))
([name password host port]
  (swap! database merge {:user name :password password :host host :port port})))

(defn find-schedule [name]
  (let [sched
          (j/query @database
            ["SELECT schedule FROM schedule.schedule WHERE name = ?" name])]
  (parse-string (:schedule (first sched)) true)))

(defn find-schedules []
  (let [sched
          (j/query @database
            ["SELECT name FROM schedule.schedule"])]
  (map #(:name %) sched)))

(defn insert-schedule [name schedule]
  (j/insert! @database "schedule.schedule"
    {:name name :schedule (generate-string schedule)}))

(defn update-schedule [name schedule]
  (j/update! @database "schedule.schedule"
    {:schedule (generate-string schedule)} ["name = ?" name]))

(defn delete-schedule [name]
  (j/delete! @database "schedule.schedule" ["name = ?" name]))

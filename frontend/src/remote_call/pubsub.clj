(ns remote-call.pubsub
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cheshire.core :refer :all]))

(def tests {:root_locations "ROOT LOCATIONS"
            :remote_locations "REMOTE LOCATIONS"
            :schedule_validity "SCHEDULE VALIDITY"
            :series_playlist "SERIES PLAYLIST"
            :playlist_ids "PLAYLIST IDS"
            :ensure_tags "ENSURE TAGS"})

(def redis-var "REDIS_URI")

(def server-conn {:pool {} :spec {:uri  (System/getenv redis-var)}})

(defn run-tests [& test-pairs]
  (future
    (println "Doing: " test-pairs)
    (try
      (dorun
       (map
        #(let [[key args] %
               test (key tests)
               json (generate-string {:test test
                                      :args args})]
           (println "Checking test: " json)
           (wcar server-conn
                 (car/publish "exception"
                              json)))
        test-pairs))
      (catch Throwable e
        (.printStackTrace e)))))


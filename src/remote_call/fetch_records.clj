(ns remote-call.fetch-records
  (:require
    [clojure.tools.logging :as logger]
    [remote-call.locator :refer [get-file-url]]
    [remote-call.playlist :refer [get-catalog-id]]
    [remote-call.schedule :refer [get-schedule]]
    [remote-call.meta :refer [get-meta]]))

(defn- my-merge [a b acc]
  (if (empty? a)
    acc
    (recur
      (drop 1 a)
      (drop 1 b)
      (cons (merge {:url (first a)} (first b)) acc))))

(defn fetch [schedule-host playlist-host locator-host meta-host user index schedule-name]
  (let [playlist (get-schedule schedule-host schedule-name index)
        catalog_ids (map
                      (fn [item]
                        (get-catalog-id playlist-host (:name item) (:index item)))
                          playlist)
        locations (map
                    (fn [item]
                      (get-file-url locator-host item)) catalog_ids)
        meta (map
                (fn [item]
                  (get-meta meta-host item ["season", "episode_name", "series", "episode"])) catalog_ids)
        failures (filter #(= "failure" (:status %)) (flatten [playlist catalog_ids locations meta]))]
  (if (empty? failures)
    (reverse (my-merge locations meta []))
    (do
      (logger/error "failures are: " failures)
      (first failures)))))

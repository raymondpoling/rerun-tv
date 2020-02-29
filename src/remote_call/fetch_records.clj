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

;; This should be cleaned up.
(defn fetch [schedule-host playlist-host locator-host meta-host user index schedule-name]
  (if-let [playlist (get-schedule schedule-host schedule-name index)]
        (let [catalog_ids (map
                      (fn [item]
                        (get-catalog-id playlist-host (:name item) (:index item)))
                          playlist)]
                          (if (some nil? catalog_ids)
                            {:status "failure" :message "playlist service not available"}
          (let [locations (map
                      (fn [item]
                        (get-file-url locator-host item)) catalog_ids)]
                        (if (some nil? locations)
                          {:status "failure" :message "locator service not available"}
            (let [meta (map
                    (fn [item]
                      (get-meta meta-host item ["season", "episode_name", "series", "episode"])) catalog_ids)]
                      (if (some nil? meta)
                        {:status "failure" :message "meta service not available"}
                          (reverse (my-merge locations meta []))))))))
        {:status "failure" :message "schedule service not available"}))

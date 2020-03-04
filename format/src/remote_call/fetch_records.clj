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

(defn if-valid-return [service validator remote-call]
  (fn [input]
    (let [ret (remote-call input)]
      (if (validator ret) ; If validator is TRUE a failed condition occurred
        {:status "failure" :message (str service " service not available")}
        ret))))

(defn run-while-valid
  ([input] input)
  ([input & body]
    (if (= "failure" (:status input))
      input
      (apply run-while-valid (cons ((first body) input) (rest body))))))


;; This should be cleaned up.
(defn fetch [schedule-host playlist-host locator-host meta-host user index schedule-name]
  (run-while-valid index
    (if-valid-return "schedule" nil? #(get-schedule schedule-host schedule-name %))
    (if-valid-return "playlist"
                      #(some nil? %)
                      #(map
                        (fn [item]
                          (get-catalog-id playlist-host (:name item) (:index item)))
                          %))
    (if-valid-return "locator"
                      #(some (fn [t] (nil? (:url t))) %)
                      #(map
                        (fn [item]
                          {:url (get-file-url locator-host item) :catalog_id item}) %))
    (if-valid-return "meta"
                    #(some (fn [t] (>= 1 (count t))) %)
                    #(map
                      (fn [item]
                        (assoc (get-meta meta-host (:catalog_id item) ["season", "episode_name", "series", "episode"]) :url (:url item))) %))))

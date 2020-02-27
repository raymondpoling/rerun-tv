(ns remote-call.validate)

(defn- playlists-in-schedule-playlists [acc schedule]
  (condp = (:type schedule)
    "playlist" (cons schedule acc)
    "merge" (merge acc (map #(playlists-in-schedule-playlists [] %) (:playlists schedule)))
    "multi" (merge acc [(playlists-in-schedule-playlists [] (:playlist schedule))])
    "complex" (merge acc (map #(playlists-in-schedule-playlists [] %) (:playlists schedule)))))

(defn- playlists-in-schedule [schedule]
  (flatten (map #(playlists-in-schedule-playlists [] %) (:playlists schedule))))

(defn- validate-valid-schedule [name schedule]
  (and (= (:name schedule) name) (some? (:playlists schedule)) (vector? (:playlists schedule)) name))

(defn validate-schedule [playlist-map name schedule]
  (if (validate-valid-schedule name schedule)
    (let [sched-playlists (playlists-in-schedule schedule)
          checklist (map (fn [t] [(= (get playlist-map (:name t)) (:length t)) (:name t)])
                    sched-playlists)]
          (if (every? #(first %) checklist)
              {:status :ok}
              {:status :invalid
                :message (str "failed validations: [" (clojure.string/join ", " (map second (filter #(not (first %)) checklist))) "]")}))
    {:status :invalid :message "invalid schedule"}))

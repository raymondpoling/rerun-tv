(ns remote-call.validate)

(defn- playlists-in-schedule-playlists [acc schedule]
  (condp = (:type schedule)
    "playlist" (cons schedule acc)
    "merge" (merge acc
                   (map
                    #(playlists-in-schedule-playlists [] %)
                    (:playlists schedule)))
    "multi" (merge
             acc
             [(playlists-in-schedule-playlists [] (:playlist schedule))])
    "complex" (merge acc
                     (map #(playlists-in-schedule-playlists [] %)
                          (:playlists schedule)))))

(defn- playlists-in-schedule [schedule]
  (flatten (map #(playlists-in-schedule-playlists [] %)
                (:playlists schedule))))

(defn- validate-valid-schedule [name schedule]
  (filter #(not (nil? %))
          [(when (not= (:name schedule) name)
             "Invalid Schedule: name must match")
           (when (empty? (:playlists schedule))
             "Invalid Schedule: must provide a playlists")
           (when (not (vector? (:playlists schedule)))
             "Invalid Schedule: playlists must be an array")
           (when (not name) "Invalid Schedule: name must be defined")]))

(defn validate-schedule [playlist-map name schedule]
  (if-let [failures (not-empty (validate-valid-schedule name schedule))]
    {:status :invalid :messages failures}
    (let [sched-playlists (playlists-in-schedule schedule)
          checklist (map
                     (fn [t] [(= (get playlist-map (:name t)) (:length t))
                              (:name t)])
                    sched-playlists)]
          (if (every? #(first %) checklist)
              {:status :ok}
              {:status :invalid
               :messages (map #(str "Failed Validation: " %)
                              (map second
                                   (filter #(not (first %)) checklist)))}))))

(ns format.m3u)

(def empty-line "")

(defn- extinf [series season episode title]
  (str "#EXTINF:0,, " series " S" season "E" episode (if (some? title) (str ": " title))))

(defn- item-group [group]
  [(extinf (:series group) (:season group) (:episode group) (:episode_name group))
    (:url group)
    empty-line])

(defn m3u [schedule-name index items]
  (clojure.string/join "\n"
    (concat ["#EXTM3U" (str "#PLAYLIST: " schedule-name " - " index) empty-line]
      (flatten (map item-group items)))))

(ns format.m3u
  (:require
   [clojure.tools.logging :as logger]
   [clojure.string :as cls]))

(def empty-line "")

(defn- extinf [series season episode title]
  (str "#EXTINF:0,, " series " S" season "E" episode
       (when (some? title) (str ": " title))))

(defn- item-group [group]
  (logger/debug "group? " group)
  [(extinf (:series group)
           (:season group)
           (:episode group)
           (:episode_name group))
    (:locations group)
    empty-line])

(defn m3u [schedule-name index items]
  (logger/debug "items?" items)
  (cls/join "\n"
            (concat ["#EXTM3U" (str "#PLAYLIST: "
                                    schedule-name " - " index) empty-line]
      (flatten (map item-group items)))))

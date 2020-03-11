(ns html.schedule-builder-get
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.tools.logging :as logger]
            [cheshire.core :refer [generate-string]]))

(defn schedule-options [schedule-names]
  (concat
    (list [:option ""])
    (map (fn [name] [:option name]) schedule-names)))

(defn schedule-builder-get [schedule-names message]
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/builder.css"}]
      [:title "ReRun TV - Build a Schedule"]]
    [:body
      [:div {:id "content"}
        (header "Build a Schedule")
        [:div
          [:h2 "Update Playlist"]
          [:form {:method "post" :action "schedule-builder.html"}
            (vec (concat (list :select {:name "schedule-name"}) (schedule-options schedule-names)))
            [:input {:type "submit" :name "type" :value "Update"}]]]
        [:div
          [:form {:method "post" :action "schedule-builder.html"}
            [:input {:type "text" :name "schedule-name"}]
            [:input {:type "submit" :name "type" :value "Create"}]]]
        [:div {:id "message"} (if message message)]]]))

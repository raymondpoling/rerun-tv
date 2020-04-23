(ns html.schedule-builder
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.tools.logging :as logger]
            [helpers.schedule-builder :as hsb]))

(defn playlist-drop-down [playlists]
    [:select {:id "playlist"}
      (map #(vector :option (str (:name %) " " (:length %))) playlists)])

(defn padding [rows]
  (if (not= (count rows) 0)
    (let [longest (apply max (map first rows))]
      [longest (map #(conj (second %)
        (when (not= 0 (- longest (first %)))
          [:td {:colspan (- longest (first %)) :class "filler"}]))
        rows)])
    0))

(defn convert [rows]
  (case (keyword (:type rows))
    :playlist (hsb/->Playlist (:name rows) (:length rows))
    :merge    (hsb/->Merge (map convert (:playlists rows)))
    :multi    (hsb/->Multi (convert (:playlist rows)) (:step rows))
    :complex  (hsb/->Complex (map convert (:playlists rows)))))

(defn median [& values]
  (if (not= 0 (count values))
    (nth (sort values) (/ (count values) 2))
    0))

(defn schedule-builder [schedule schedule-name playlists mode role]
  (let [converted (map convert (:playlists (hsb/parsed schedule)))
        small (apply median (map hsb/length converted))]
  (logger/debug "validity? " (hsb/valid? schedule))
    (html5 {:lang "en" :dir "ltr"}
      [:head
        [:meta {:charset "utf-8"}]
        [:link {:rel "stylesheet" :href "/css/master.css"}]
        [:link {:rel "stylesheet" :href "/css/builder.css"}]
        [:title "ReRun TV - Build a Schedule"]]
      [:body
        [:div {:id "content"}
          (header "Build a Schedule" role)
          (let [padded (padding (map #(hsb/render % true small 1) converted))]
            [:div {:id "table-holder"}
              [:table {:class "schedule"}
                [:thead
                  [:tr [:th {:scope "col" :class "first"} "Type: Length" [:br] "RR (Repitition Rate)"]
                        [:th {:colspan (if (= 0 small) 1 (first padded))} "Playlists"]]]
                [:tbody (if (= 0 small)
                  [:tr [:td {:class "empty"} "Empty"]]
                  (second padded))]]])
          [:div {:class "playlists"} (playlist-drop-down playlists)]
          [:div [:form {:method "post" :action "schedule-builder.html"}
                 [:textarea {:class (:status (hsb/valid? schedule))
                             :name "schedule-body"} (hsb/string schedule)]
            (if (= "ok" (:status (hsb/valid? schedule)))
              [:div {:class "ok"} "OK!"]
              [:div {:class "invalid"}
               [:ol (map #(vector :li %)
                         (:messages (hsb/valid? schedule)))]])
                 [:input {:type "checkbox" :checked "checked"
                          :id "preview" :value "preview"
                          :name "preview"}]
                 [:label {:for "preview"} "Preview"]
            [:input {:type "hidden" :name "mode" :value mode}]
            [:input {:type "hidden" :name "schedule-name" :value schedule-name}]
            [:input {:type "submit" :value "Submit"}]]]]])))

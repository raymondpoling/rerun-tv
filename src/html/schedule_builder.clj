(ns html.schedule-builder
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.tools.logging :as logger]
            [cheshire.core :refer [generate-string]]))

(defn playlist-drop-down [playlists]
    [:select {:id "playlist"}
      (map #(vector :option (str (:name %) " " (:length %))) playlists)])

(defn pretty-divide [upper lower]
  (format "%.2f" (float (/ upper lower))))

(defprotocol ScheduleType
  (render [self row? small])
  (length [self]))

(defrecord Playlist [name length]
  ScheduleType
  (render [self row? small] (if row?
    [:tr {:class "playlist"}
      [:td {:class "first"}
        "Playlist: " length [:br]
        "RR: " (pretty-divide length small)]
      [:td [:span {:class "name"} name] [:br] [:span {:class "count"} "Count: " length]]]
    (list [:td [:span {:class "name"} name] [:br] [:span {:class "count"} "Count: " length]])))
  (length [self] length))

(defrecord Merge [playlists]
  ScheduleType
  (render [self row? small]
    (if row?
      (vec
        (concat
          [:tr {:class "merge"}
            [:td {:class "first"}
              "Merge: " (length self) [:br]
              "RR: " (pretty-divide (length self) small)]]
          (map #(render % false small) playlists)))
      (concat
        (list
          [:td {:class "first"}
            "Merge: " (length self) [:br]
            "RR: " (pretty-divide (length self) small)])
            (map #(render % false small) playlists))))
  (length [self] (reduce + (map #(length %) playlists))))

(defrecord Multi [playlist step]
  ScheduleType
  (render [self row? small]
    (if row?
      (vec
        (concat
          [:tr {:class "multi"}
            [:td {:class "first"}
              "Multi: " (length self) [:br]
              "Step: " step [:br]
              "RR: " (pretty-divide (length self) small)]]
          (render playlist false small)))
      (concat
        (list
          [:td {:class "first"}
            "Multi: " (length self) [:br]
            "Step: " step [:br]
            "RR: " (pretty-divide (length self) small)])
        (render playlist false small))))
  (length [self] (float (/ (length playlist) step))))

(defn padding [rows]
  (println  "row counts? " (map count rows) " rows? " rows)
  (if (not= (count rows) 0)
    (let [longest (apply max (map count rows))]
      (map #(conj %
        (map
          (fn [_] [:td])
          (range (- longest (count %)))))
        rows))
    0))

(defn convert [rows]
  (case (keyword (:type rows))
    :playlist (->Playlist (:name rows) (:length rows))
    :merge    (->Merge (map convert (:playlists rows)))
    :multi    (->Multi (convert (:playlist rows)) (:step rows))))

(defn median [& values]
  (if (not= 0 (count values))
    (nth (sort values) (/ (count values) 2))
    0))


(defn schedule-builder [schedule playlists validate type]
  (let [converted (map convert (:playlists schedule))
        small (apply median (map length converted))]
  (logger/debug "converted: " validate)
    (html5 {:lang "en" :dir "ltr"}
      [:head
        [:meta {:charset "utf-8"}]
        [:link {:rel "stylesheet" :href "/css/master.css"}]
        [:link {:rel "stylesheet" :href "/css/builder.css"}]
        [:title "ReRun TV - Build a Schedule"]]
      [:body
        [:div {:id "content"}
          (header "Build a Schedule")
          [:table {:class "schedule"}
            [:tr [:th "Type" [:br] "Length" [:br] "RR (Repitition Rate)"]]
            (if (= 0 small)
              [:tr [:td {:class "empty"} "Empty"]]
              (padding (map #(render % true small) converted)))]
          [:div {:class "playlists"} (playlist-drop-down playlists)]
          [:div [:form {:method "post" :action "schedule-builder.html"}
            [:textarea {:class (:status validate) :name "schedule-body"} (generate-string schedule {:pretty true})]
            (if (= "ok" (:status validate))
              [:div {:class "ok"} "OK!"]
              [:div {:class "invalid"} (:message validate)])
            [:input {:type "checkbox" :checked "checked" :id "preview" :value "preview" :name "preview"}][:label {:for "preview"} "Preview"]
            [:input {:type "text" :name "schedule-name" :value (:name schedule)}]
            [:ul
             [:li
              [:label {:for "update"} "Update"]
              [:input {:type "radio" :name "type" :id "update" :value "Update" :checked (if (= "Update" type) "checked")}]]
            [:li
              [:label {:for "create"} "Create"]
              [:input {:type "radio" :name "type" :id "create" :value "Create" :checked (if (not= "Update" type) "checked")}]]]
            [:input {:type "submit" :value "Submit"}]]]]])))

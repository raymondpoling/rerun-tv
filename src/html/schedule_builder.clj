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

(defn make-row [class length rr & extra]
  (list :tr {:class class}
    (vec
      (concat
        (list :th {:scope "row" :class "first"} (clojure.string/capitalize class) ": " length [:br] "RR: " rr)
        (if extra (list [:br] (clojure.string/join " " extra)))))))

(defprotocol ScheduleType
  (render [self row? small divisor])
  (length [self]))

(defrecord Playlist [name length]
  ScheduleType
  (render [self row? small divisor] (if row?
    (let [render (render self false small divisor)
          len (int (Math/floor (/ (first render) divisor)))]
      [len (vec (concat (make-row "playlist" length (pretty-divide length small)) (second render)))])
    (let [len (int (Math/floor (/ length divisor)))]
      [len (list
        [:td {:colspan len}
          [:span {:class "name"} name]
          [:br]
          [:span {:class "count"} "Count: " length]])])))
  (length [self] length))

(defrecord Merge [playlists]
  ScheduleType
  (render [self row? small divisor]
    (if row?
      (let [render (map #(render % false small divisor) playlists)
            len (reduce + (map first render))]
        [len (vec
          (concat
            (make-row "merge" (length self) (pretty-divide (length self) small))
            (map second render)))])
      (let [render (map #(render % false small divisor) playlists)]
        [(reduce + (map first render)) (vec (reduce concat (map second render)))])))
  (length [self] (reduce + (map #(length %) playlists))))

(defrecord Multi [playlist step]
  ScheduleType
  (render [self row? small divisor]
    (if row?
      (let [render (render playlist false small (* step divisor))
            len (first render)]
        [len (vec
          (concat
            (make-row "multi" (length self) (pretty-divide (length self) small) "Step:" step)
          (second render)))])
      (let [render (render playlist false small (* step divisor))]
        [(first render)
        (concat
          (list
            [:td {:class "first"}
              "Multi: " (length self) [:br]
              "Step: " step [:br]
              "RR: " (pretty-divide (length self) small)])
              (second render))])))
  (length [self] (float (/ (length playlist) step))))

;; The change means this won't work anymore
;; colspan can't be counted
(defn padding [rows]
  (if (not= (count rows) 0)
    (let [longest (apply max (map first rows))]
      [longest (map #(conj (second %)
        (if (not= 0 (- longest (first %)))
          [:td {:colspan (- longest (first %)) :class "filler"}]))
        rows)])
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
          (let [padded (padding (map #(render % true small 1) converted))]
            [:table {:class "schedule"}
              [:thead
                [:tr [:th {:scope "col" :class "first"} "Type: Length" [:br] "RR (Repitition Rate)"]
                      [:th {:colspan (first padded)} "Playlists"]]]
              [:tbody (if (= 0 small)
                [:tr [:td {:class "empty"} "Empty"]]
                (second padded))]])
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

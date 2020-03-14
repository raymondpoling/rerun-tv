(ns html.preview
  (:require [hiccup.core :refer [html]]
            [html.header :refer [header]]
            [hiccup.page :refer [html5]]))

(defn stylesheet [path]
  [:link {:rel "stylesheet" :type "text/css" :href path}])

(defn make-options [schedule schedules]
  (map #(vector :option (if (= schedule %) {:selected "selected"}) %) schedules))

(defn form [schedule options idx update]
  [:form {:action "preview.html" :method "get"}
    [:select {:name "schedule"}
      options]
    [:input {:type "hidden" :name "idx" :value idx}]
    [:input {:type "text" :name "index" :size 5 :value idx}]
    [:label {:for "update"} "Update?"]
    [:input {:type "checkbox" :id "update" :value "update" :name "update" :checked (if update "checked")}]
    [:input {:type "submit" :value "Preview"}]
    [:input {:type "submit" :name "reset" :value "Reset"}]
    [:input {:type "submit" :name "download" :value "Download"}]])

(defn preview-column [schedule divs idx & outline]
  [:div {:class "column" :style (if (first outline) "border:solid black 1px;border-radius: 0.5em")}
    [:h2 schedule ": " idx]
    divs])

(defn make-divs [items]
  (map #(vector :div {:class "item" }
    [:b (:name %)
    [:br] "Index: " (:index %)]
    [:br] (:series %) (str " S" (:season %) "E" (:episode %))
    [:br] [:em (:episode_name %)]) items))

(defn make-preview-page [schedule schedules idx update previous current next]
  (let [options (make-options schedule schedules)
        prev-items (make-divs previous)
        curr-items (make-divs current)
        next-items (make-divs next)]
    (html5 {:lang "en" :dir "ltr"}
      [:head
        [:title "Schedule Preview"]
        (stylesheet "css/master.css")
        (stylesheet "css/preview.css")]
      [:body
        [:div {:id "content"}
          (header "Schedule Preview")
          (form schedule options idx update)
          (preview-column schedule prev-items (- idx 1))
          (preview-column schedule curr-items idx true)
          (preview-column schedule next-items (+ 1 idx))]])))

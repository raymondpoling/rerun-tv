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
    [:input {:type "text" :name "index" :size 5}]
    [:label {:for "update"} "Update?"]
    [:input {:type "checkbox" :id "update" :value "update" :name "update" :checked (if update "checked")}]
    [:input {:type "submit" :value "Preview"}]])

(defn preview-column [schedule divs idx update]
  [:div {:class "column"}
    [:h2 schedule ": " idx]
    divs
    [:br]
    [:br]
    [:div {:class "dl-button"}
      [:a {:href (str "format/" schedule (if-let [idx (str "?index="  idx)]
                    (str idx (if update (str "&update=update")))
                    (if update (str "?update=update"))))}
        "Download this List"]]])

(defn make-divs [items]
  (map #(vector :div {:class "item" } (:name %) [:br] "Index: " (:index %)) items))

(defn make-preview-page [schedule schedules idx items update]
  (let [options (make-options schedule schedules)
        divs (make-divs items)]
    (html5 {:lang "en" :dir "ltr"}
      [:head
        [:title "Schedule Preview"]
        (stylesheet "css/master.css")
        (stylesheet "css/preview.css")]
      [:body
        [:div {:id "content"}
          (header "Schedule Preview")
          (form schedule options idx update)
          (preview-column schedule divs idx update)]])))

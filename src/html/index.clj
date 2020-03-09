(ns html.index
  (:require [hiccup.page :refer [html5]]
            [html.header :refer [header]]))

(defn make-index []
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:title "ReRun TV"]]
    [:body
      [:div {:id "content"}
        (header "ReRun TV")
        [:article
          [:h2 "Welcome! This is a stub article because this is just starting"]]]]))

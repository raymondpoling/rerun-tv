(ns html.index
  (:require [hiccup.page :refer [html5]]
            [html.header :refer [header]]))

(defn- make-event [event]
  [:article
  [:h2 (:title event)]
  [:h3 "By " (:author event) " on " (:posted event)]
  (:information event)])

(defn make-index [events]
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:title "ReRun TV"]]
    [:body
      [:div {:id "content"}
        (header "ReRun TV")
        (map make-event events)
        (if (= 10 (count events))
          [:a {:id "previous"
              :href (str "index.html?start=" (:message_number (last events)))}
              "Previous"])]]))

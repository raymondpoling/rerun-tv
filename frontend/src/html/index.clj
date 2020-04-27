(ns html.index
  (:require [hiccup.page :refer [html5]]
            [html.header :refer [header]]))

(defn- make-event [event]
  [:article
  [:h2 (:title event)]
  [:h3 "By " (:author event) " on " (:posted event)]
  (:information event)])

(defn make-index [events role summary]
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:title "ReRun TV"]]
    [:body
      [:div {:id "content"}
       (header "ReRun TV" role)
       [:article {:style "background-color:#cfffe5"}
        [:h2 "About the system!"]
        [:p (format "There are %s shows, and %s episodes across %s total seasons, for an average of %s seasons per series and %s episodes per season and %s schedules."
                    (:series summary)
                    (:episodes summary)
                    (:seasons summary)
                    (format "%.2f" (float (/ (:seasons summary)
                                             (:series summary))))
                    (format "%.2f" (float (/ (:episodes summary)
                                             (:seasons summary))))
                    (:schedules summary))]]
        (map make-event events)
        (when (= 10 (count events))
          [:a {:id "previous"
              :href (str "index.html?start=" (:message_number (last events)))}
              "Previous"])]]))

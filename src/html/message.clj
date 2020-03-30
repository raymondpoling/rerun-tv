(ns html.message
  (:require
   [html.header :refer [header]]
   [hiccup.page :refer [html5]]))

(defn make-message-page [role]
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/message.css"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:title "Admin Message - ReRun TV"]]
    [:body
     [:div {:id "content"}
      (header "Post Message" role)
      [:form {:action "/message.html" :method "post"}
       [:label {:for "title"} "Title"][:br][:input {:type "text" :name "title" :value ""}][:br]
       [:label {:for "message"} "Message"][:br][:textarea {:name "message" :value ""}][:br]
       [:input {:type "submit" :name "Submit"}]]]]))

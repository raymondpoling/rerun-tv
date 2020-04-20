(ns html.login
  (:require [hiccup.page :refer [html5]]))

(defn login []
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/login.css"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:title "Login to use ReRun TV"]]
    [:body
      [:div {:class "login"}
        [:form {:action "login" :method "post"}
          [:label {:for "username"} "Username"][:br][:input {:type "text" :name "username" :value ""}][:br]
          [:label {:for "password"} "Password"][:br][:input {:type "password" :name "password" :value ""}][:br]
          [:input {:type "submit" :name "Submit"}]]]]))

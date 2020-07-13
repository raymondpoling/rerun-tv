(ns html.nomination
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]))

(defn nomination-page [atype a-name message role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder-select.css"}]
    [:title "ReRun TV - Nomination"]]
   [:body
    [:div {:id "content"}
     (header "Nominate for Deletion" role)
     [:form {:action "/nominate.html"
             :method :post
             :class "box"}
      [:h2 {:class "box-center"}
       (format "Nominating %s of type %s" a-name atype)]
      [:input {:type "hidden" :name "a-name" :id "a-name" :value a-name}]
      [:input {:type "hidden" :name "atype" :id "atype" :value atype}]
      [:textarea {:id "reason" :name "reason"}]
      [:input {:type "submit" :value "Nominate"}]]
     [:div {:id "message"
            :style (when (not message) "display:none")}
      (when message message)]]]))

(ns html.deletion
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.string :as cls]))

(defn recent-row [item]
  [:tr
   [:td (:type item)]
   [:td (:name item)]
   [:td (:maker item)]
   [:td (:reason1 item)]
   [:td (:checker item)]
   [:td (:reason2 item)]
   [:td (:status item)]])

(defn recent-table [items]
  [:table
   [:caption "Recent Activity"]
   [:thead [:tr [:th "Type"] [:th "Name"] [:th "Maker"] [:th "Reason"]
            [:th "Checker"] [:th "Reason"] [:th "Status"]]]
   [:tbody
    (map #(recent-row %) items)]])

(defn deletion-form [atype a-name]
  [:form {:method :post
          :action "/deletion.html"}
   [:input {:type "hidden" :name "a-name" :value a-name}]
   [:input {:type "hidden" :name "atype" :value atype}]
   [:input {:type "text" :name "reason" :size "22"}]
   [:input {:type "submit" :name "exe-or-rej" :value "Reject"}]
   [:input {:type "submit" :name "exe-or-rej" :value "Delete"}]])

(defn nomination-row [item]
  [:tr
   [:td (:type item)]
   [:td (:name item)]
   [:td (:maker item)]
   [:td (:reason1 item)]
   [:td (:status item)]
   [:td (deletion-form (:type item) (:name item))]])

(defn nomination-table [items]
  [:table
   [:caption "Nominations for Deletion"]
   [:thead [:tr [:th "Type"] [:th "Name"] [:th "Maker"] [:th "Reason"]
            [:th "Status"] [:th "Reject or Delete"]]]
   [:tbody
    (map #(nomination-row %) items)]])

(defn deletion-page [nominations recent message role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder-select.css"}]
    [:link {:rel "stylesheet" :href "/css/user.css"}]
    [:title "ReRun TV - Nomination"]]
   [:body
    [:div {:id "content"}
     (header "Execute/Reject Deletion" role)
     (nomination-table nominations)
     (recent-table recent )
     [:div {:id "message"
            :style (when (not message) "display:none")}
      (when message message)]]]))

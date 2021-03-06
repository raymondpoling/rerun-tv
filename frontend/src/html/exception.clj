(ns html.exception
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.string :as cls]
            [cheshire.core :refer [parse-string]]))

(defn make-links [tests]
  (let [sorted (sort tests)]
    [:div {:class "navigation"}
     (map #(vector :a {:href (str "#" %)} %) sorted)]))

(defn run-new-form [test]
  [:form
   {:action "exception.html"
    :method :post
    :id test}
   [:h3 (str test ": Run")]
   [:input {:name "args"
            :type :text}]
   [:input {:name "test"
            :type "hidden"
            :value test}]
   [:input {:name "Submit"
            :type "Submit"}]])

(defn rerun-form [test args]
  [:form
   {:action "exception.html"
    :method :post}
   [:input {:name "args"
            :value (cls/join ", " (parse-string args))
            :type :hidden}]
   [:input {:name "test"
            :type "hidden"
            :value test}]
   [:input {:type :submit
            :value "Rerun"}]])

(defn make-row [test result]
  [:tr
   [:td (:date result)]
   [:td (:passFail result)]
   [:td (:remediationSucceeded result)]
   [:td (:statusMessage result)]
   [:td (:args result)]
   [:td (rerun-form test (:args result))]])

(defn make-test-results [test results]
  [:table
   [:caption (str "Test: " test)]
   [:thead
    [:th "Date"]
    [:th "Pass?"]
    [:th "Remediation?"]
    [:th "Status Message"]
    [:th "Arguments"]
    [:th "Rerun"]]
   [:tbody
    (map #(make-row test %)  results)]])

(defn exception-page [test-set role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/user.css"}]
    [:link {:rel "stylesheet" :href "/css/exception.css"}]
    [:title "ReRun TV - Exceptions"]]
   [:body
    [:div {:id "content"}
     (header "ReRun TV - Exceptions" role)
     (make-links (map first test-set))
     (map #(let [[test results] %]
              [:div {:class "exception-holder"}
               (run-new-form test)
               [:br]
               (make-test-results test results)])
     (sort-by first test-set))]]))

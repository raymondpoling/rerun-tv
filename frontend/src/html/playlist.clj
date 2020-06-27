(ns html.playlist
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.string :as cls]
            [cheshire.core :refer [parse-string]]))

(defn make-form [name items mode]
  [:form {:action "playlist-builder.html"
          :method :post}
   [:input {:name "name" :id "name" :value name :type :text}]
   [:textarea
    {:name "items" :id "values"}
    items]
   [:input {:type :submit
            :name "mode"
            :id "mode"
            :value mode}]])

(defn playlist-page [name items mode role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder.css"}]
    [:link {:rel "stylesheet" :href "/css/playlist.css"}]
    [:title "ReRun TV - Playlist Builder"]]
   [:body
    [:div {:id "content"}
     (header "ReRun TV - Playlist Playlist Builder" role)
     (make-form name items mode)]]))

(defn playlist-get [playlist-names role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder.css"}]
    [:link {:rel "stylesheet" :href "/css/playlist.css"}]
    [:title "ReRun TV - Playlist Builder"]]
   [:body
    [:div {:id "content"}
     (header "ReRun TV - Playlist Playlist Builder" role)
     [:form {:method "post" :action "playlist-builder.html" :class "box"}
      [:h2 {:class "box-center"} "Update Playlist"]
      (vec (concat (list :select
                         {:name "name"
                          :class "box-center"})
                   (map #(vector :option %) playlist-names)))
      [:input {:type "hidden"
               :name "preview"
               :value "true"}]
      [:input {:type "submit"
               :name "mode"
               :value "Update"
               :class "box-center"}]]
     [:form {:method "post"
             :action "playlist-builder.html"
             :class "box"}
      [:h2 {:class "box-center"} "Create Playlist"]
      [:input {:type "text"
               :name "name"
               :class "box-center"}]
      [:input {:type "hidden"
               :name "preview"
               :value "true"}]
      [:input {:type "submit"
               :name "mode"
               :value "Create"
               :class "box-center"}]]]]))

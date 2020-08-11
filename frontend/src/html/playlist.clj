(ns html.playlist
  (:require [html.header :refer [header]]
            [ring.util.codec :refer [url-encode]]
            [hiccup.page :refer [html5]]
            [clojure.string :as cls]
            [cheshire.core :refer [parse-string]]))

(defn make-title [i]
  (str (:series i) " S" (:season i) "E" (:episode i)))

(defn make-divs [items]
  (map #(vector :article {:class "item" }
                [:img {:src (if (not (or (empty? (:thumbnail %))
                                         (= "N/A" (:thumbnail %))))
                              (:thumbnail %)
                              "/image/not-available.svg")}]
                [:ul {:class "textbox"}
                 [:li {:class "index"}
                  (get-in % [:playlist :name]) ": "
                  (get-in % [:playlist :index])]
                 [:li (if (seq (:series %))
                        [:a {:href (str "/library.html?series-name="
                                        (url-encode (:series %))
                                        "#" (format "S%sE%s"
                                                    (:season %)
                                                    (:episode %)))
                             :target "_blank"} (make-title %)]
                        (make-title %))]
                 [:li [:em (:episode_name %)]]]
                [:div {:class "summary"}
                 [:hr]
                 [:p (:summary %)]])
       items))

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

(defn make-search [name items mode tags search-results]
  [:form {:action "playlist-builder.html"
          :method :post}
   [:input {:name "name" :id "name" :type "hidden" :value name}]
   [:input {:name "items" :id "items" :type "hidden" :value items}]
   [:input {:name "mode" :id "mode" :type "hidden" :value mode}]
   [:input {:name "tags" :id "tags" :type "text" :value tags}]
   [:select {:name "type?" :id "type?"}
    [:option "EPISODE"]
    [:option "SEASON"]
    [:option "SERIES"]]
   [:input {:name "search" :id "search" :type "submit" :value "submit"}]
   [:ol (map #(vector :li [:input {:name "search-items"
                                   :id "search-items"
                                   :type "checkbox"
                                   :value (first (:catalog_ids %))}]
                      (make-divs (:records %)))
             search-results)]])

(defn playlist-page [name items mode tags search-results role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder.css"}]
    [:link {:rel "stylesheet" :href "/css/preview.css"}]
    [:link {:rel "stylesheet" :href "/css/playlist.css"}]
    [:title "ReRun TV - Playlist Builder"]]
   [:body
    [:div {:id "content"}
     (header "ReRun TV - Playlist Playlist Builder" role)
     (make-search name items mode tags search-results)
     (make-form name items mode)]]))

(defn playlist-get [playlist-names role]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :href "/css/master.css"}]
    [:link {:rel "stylesheet" :href "/css/builder-select.css"}]
    [:title "ReRun TV - Playlist Builder"]]
   [:body
    [:div {:id "content"}
     (header "ReRun TV - Playlist Playlist Builder" role)
     [:form {:method "post" :action "playlist-builder.html" :class "box"}
      [:h2 {:class "box-center"} "Update Playlist"]
      (vec (concat (list :select
                         {:name "name"
                          :class "box-center"})
                   (map #(vector :option %)
                        (filter #(not
                                  (cls/includes? % ":SYSTEM"))
                                playlist-names))))
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
               :class "box-center"}]]
     (when (= role "media")
       [:form {:method "get" :action "/nominate.html" :class "box"}
        [:h2 {:class "box-center"} "Delete Playlist"]
        (vec (concat (list :select
                           {:name "a-name"
                            :class "box-center"})
                     (map #(vector :option %) playlist-names)))
        [:input {:type "hidden" :name "atype" :id "atype" :value "playlist"}]
        [:input {:type "submit" :value "Nominate"}]])]]))

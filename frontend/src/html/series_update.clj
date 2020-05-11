(ns html.series-update
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.string :as cls]))

(defn show-summary [text]
  (vector :li
          [:label {:for "summary"} "SUMMARY"][:br]
          [:textarea {:id "summary" :name "summary"} text]))

(defn show-imdb [imdbid]
  (vector :li
          [:label {:for "imdbid"} "IMDBID"]
          [:input {:id "imdbid" :name "imdbid" :value imdbid}]
          [:input {:value "IMDB Lookup" :type "submit" :name "mode"}]
          ))

(defn show-regular [k v]
  (let [n (name k)]
    (vector :li
            [:label {:for n} (cls/upper-case n)]
            [:input {:id n :name n :value v}])))

(defn if-image [img]
  (if (and (not-empty img) (not= img "N/A"))
    [:img {:src img}]
    [:img {:src "/image/not-available.svg"}]))

(defn show-thumbnail [url]
  (vector :li
          [:label {:for "thumbnail"} "THUMBNAIL"]
          [:input {:id "thumbnail" :name "thumbnail" :value url}]
          [:br]
          (if-image url)))

(defn make-list [series]
  [:ol
    (map (fn [[k v]]
           (condp = k
             :summary (show-summary v)
             :thumbnail (show-thumbnail v)
             :imdbid (show-imdb v)
             (show-regular k v)))
         series)])

(defn return-to-library [series]
  [:a {:href (str "/library.html?series-name=" series)}
   "Go Back to Library"])

(defn make-series-update-page [series tags role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/update.css"}]
      [:title "ReRun TV - Update Series"]]
    [:body
      [:div {:id "content"}
       (header "Update Series" role)
       [:form {:method "post" :action "/update-series.html"}
        (make-list series)
        [:label {:for "tags"} "Your Tags"]
        [:textarea {:name "tags" :id "tags"} (cls/join ", " tags)]
        [:input {:value "Save" :type "submit" :name "mode"}]
        [:div {:class "spacer"}]
        (return-to-library (:name series))]]]))

(defn should-default? [omdb-v]
  (or (= omdb-v "N/A") (nil? omdb-v)))

(defn make-reg-comp [k v omdb]
  (let [n (name k)
        default? (should-default? (k omdb))]
   [:ul
     [:li [:label {:for n} "Current"]
      [:input {:id n :name n :checked (when default? :checked)
               :type "radio" :value v} v]]
    (when (not default?)
      [:li
       [:label {:for n} "OMDB"]
       [:input {:id n :name n :checked :checked
                :type "radio" :value (k omdb)}
        (k omdb)]])]))

(defn make-thumb-comp [k v omdb]
  (let [n "thumbnail"
        default? (should-default? (k omdb))]
    [:ul
     [:li
      [:label {:for n} "Current"]
      [:input {:id n :name n :checked (when default? :checked)
               :type "radio" :value v}]
      [:br]
      (if-image v)]
     (when (not default?)
       [:li
        [:label {:for n} "OMDB"]
        [:input {:id n :name n :checked :checked
                 :type "radio" :value (k omdb)}]
        [:br]
        (if-image (k omdb))])]))

(defn make-options [series omdb]
  [:ol
   (map (fn [[k v]]
          (let [n (name k)]
            (vector :li (cls/upper-case n)
                    (condp = k
                      :thumbnail (make-thumb-comp k v omdb)
                      (make-reg-comp k v omdb)))))
        series)])

(defn series-side-by-side [series omdb tags role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/update.css"}]
      [:title "ReRun TV - Update Series"]]
    [:body
      [:div {:id "content"}
       (header "Update Series" role)
       [:form {:method "post" :action "/update-series.html"}
        (make-options series omdb)
        [:input {:value "Save" :type "submit" :name "mode"}]
        [:input {:value tags :type "hidden" :name "tags"}]
        [:div {:class "spacer"}]
        (return-to-library (:name series))]]]))

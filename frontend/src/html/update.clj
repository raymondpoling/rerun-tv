(ns html.update
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

(defn make-list [episode catalog-id]
  [:ol
   [:li
    [:label {:for "catalog-id"} "Catalog ID"]
    [:input {:value catalog-id
             :type "text"
             :name "catalog-id"
             :readonly "readonly"}]]
   (map (fn [[k v]]
          (condp = k
            :summary (show-summary v)
            :thumbnail (show-thumbnail v)
            :imdbid (show-imdb v)
            (show-regular k v)))
        episode)])

(defn return-to-library [series season episode]
  [:a {:href (format "/library.html?series-name=%s#S%sE%s"
                     series season episode)}
   "Go Back to Library"])

(defn make-update-page [episode files catalog-id tags role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/update.css"}]
      [:title "ReRun TV - Update Episode"]]
    [:body
      [:div {:id "content"}
       (header "Update Episode" role)
       [:form {:method "post" :action "/update.html"}
        (make-list episode catalog-id)
        [:label {:for "tags"} "Your Tags"]
        [:textarea {:name "tags" :id "tags"} (cls/join ", " tags)]
        [:label {:for "files"} "File URLs"]
        [:textarea {:name "files" :id "files"} files]
        [:input {:value "Save" :type "submit" :name "mode"}]
        [:div {:class "spacer"}]
        (return-to-library (:series episode)
                           (:season episode)
                           (:episode episode))]]]))

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

(defn make-options [episode omdb catalog-id]
  [:ol
   [:li
    [:label {:for "catalog-id"} "Catalog ID"]
    [:input {:value catalog-id
             :type "text"
             :name "catalog-id"
             :readonly "readonly"}]]
   (map (fn [[k v]]
          (let [n (name k)]
            (vector :li (cls/upper-case n)
                    (condp = k
                      :thumbnail (make-thumb-comp k v omdb)
                      (make-reg-comp k v omdb)))))
        episode)])

(defn side-by-side [episode omdb files catalog-id tags role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/update.css"}]
      [:title "ReRun TV - Update Episode"]]
    [:body
      [:div {:id "content"}
       (header "Update Episode" role)
       [:form {:method "post" :action "/update.html"}
        (make-options episode omdb catalog-id)
        [:label {:for "files"} "File URLs"]
        [:textarea {:name "files" :id "files"} files]
        [:input {:value "Save" :type "submit" :name "mode"}]
        [:input {:type "hidden" :name "tags" :value tags}]
        [:div {:class "spacer"}]
        (return-to-library (:series episode)
                           (:season episode)
                           (:episode episode))]]]))

(ns html.library
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]))

(defn series-select [series series-name]
  [:form {:class "series" :method "get" :action "???"}
    [:label {:for "series-name"}]
    [:select {:name "series-name" :id "series-name"}
     (map #(vector :option (when (= series-name %) {:selected :selected}) %)
          series)]
    [:input {:type "submit"}]])

(defn make-title [i]
  (str (:series i) " S" (:season i) "E" (:episode i)))

(defn make-episodes [episodes series-img role]
  (map #(vector :article {:class "item"
                          :id (format "S%sE%s" (:season %) (:episode %))}
    [:img {:src (if (not (or (empty? (:thumbnail %)) (= "N/A" (:thumbnail %)))) (:thumbnail %) series-img)}]
    [:ul {:class "textbox"}
      [:li [:b (:name %)]]
      [:li (if (seq (:imdbid %))
          [:a {:href (str "http://imdb.com/title/" (:imdbid %)) :target "_blank"} (make-title %)]
          (make-title %))]
      [:li (if (= role "media")
             [:a {:href (str "/update.html?catalog-id=" (:catalog-id %))} [:em (:episode_name %)]]
          [:em (:episode_name %)])]]
    [:div {:class "summary"} [:hr] [:p (:summary %)]]) episodes))

(defn make-library [series series-name records role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/library.css"}]
      [:title "ReRun TV - Library View"]]
    [:body
      [:div {:id "content"}
        (header "Library" role)
       (series-select series series-name)
       [:div {:class "series-header"}
        [:h2 series-name]
        (when (and (not= (:thumbnail (:series records)) "N/A")
                (not-empty (:thumbnail (:series records))))
          [:img {:src (:thumbnail (:series records))}])
        [:p (:summary (:series records))[:br]
         (let [seasons (apply max (cons 0 (map :season (:records records))))
               episodes (count (:records records))
               avg-episodes (format "%.2f"  (/ (double episodes) (double seasons)))]
           (format "There are %s episodes over %s seasons, for an average of %s episodes per seasons" episodes seasons avg-episodes))[:br]
         (when (and (not= (:imdbid (:series records)) "N/A")
                    (not-empty (:imdbid (:series records))))
           [:a {:href (str "http://imdb.com/title/" (:imdbid (:series records)))
                :target "_blank"} "IMDB"])]
        [:a {:href (str "/update-series.html?series-name=" series-name)} "Edit Series"]]
       [:div {:class "episodes"}
        (make-episodes (:records records)
                       (if (and (not= (:thumbnail (:series records)) "N/A")
                                (not-empty (:thumbnail (:series records))))
                         (:thumbnail (:series records))
                         "/image/not-available.svg")
                       role)]]]))

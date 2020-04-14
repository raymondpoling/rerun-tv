(ns html.bulk-update
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]
            [clojure.tools.logging :as logger]
            [cheshire.core :refer [generate-string]]))

(defn bulk-update [series results role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
     [:link {:rel "stylesheet" :href "/css/builder.css"}]
     [:link {:rel "stylesheet" :href "/css/bulk.css"}]
      [:title "ReRun TV - Update Series"]]
    [:body
      [:div {:id "content"}
      (header "Update Series" role)
      [:form {:action "/bulk-update.html" :method "post"}
        [:label {:for "series"} "Series"]
       (vec (concat (list :select {:id "series" :name "series"})
                    (if (= "failed" (:status series))
                      (list [:option  "not available"])
                      (map (fn [opt] [:option opt]) series))))
       [:label {:for "create?"} "Create new series entry?"]
       [:input {:type :checkbox
                :value :create?
                :name "create?"
                :id "create?"}]
        ; [:label {:for "summary"} "Series Summary"]
        ; [:textarea {:id "summary" :name "summary"}]
        [:div {:id "explanation"}
          [:p "Use this format for following section:"]
         [:p
          "{\"series\": {\"name\":name, \"summary\":summary, \"imdbid\":imdbid,
   \"thumbnail\":thumbnail}
\"records\":[{\"episode_name\":episode_name, \"episode\":episode, \"season\":season, \"summary\":summary, \"thumbnail\":thumbnail, \"imdbid\":imdbid, \"locations\":[\"protocol://host//path\"]}]}]}"]]
        [:label {:for "update"} "Update"]
       [:textarea {:id "update" :name "update"}
        "{\"series\":
  {\"name\":name,
   \"summary\":summay,
   \"imdbid\":imdbid,
   \"thumbnail\":thumbnail},
   \"records\":
     [{\"episode_name\":episode_name,
       \"episode\":episode,
       \"season\":season,
       \"summary\":summary,
       \"thumbnail\":thumbnail,
       \"imdbid\":imdbid
       \"locations\":[\"protocol://host//path\"]}]}"]
        [:input {:type "submit" :value "Submit"}]]
       [:div {:class "succ"
              :style (if (not= "ok" (:status results)) "display:none")}
        [:h3 "Updated Catalog Ids"]
        [:ol (map (fn [id] [:li id]) (filter not-empty
                                             (:catalog_ids results)))]]
       [:div {:class "fail"
              :style (if (empty? (:failures results)) "display:none")}
        [:h3 "Failed Catalog Ids"]
        [:ol (map (fn [id] [:li id]) (:failures results))]]
       [:div {:class "fail"
              :style (if (or (nil? results)
                              (= "ok" (:status results))) "display:none")}
        [:h3 "Failure"]
        [:p (:message results)]]]]))

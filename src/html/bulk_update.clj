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
      [:title "ReRun TV - Update Series"]]
    [:body
      [:div {:id "content"}
      (header "Update Series" role)
      [:form {:action "/bulk-update.html" :method "post"}
        [:label {:for "series"} "Series"]
        (vec (concat (list :select {:id "series" :name "series"}) (map (fn [opt] [:option opt]) series)))
        ; [:label {:for "summary"} "Series Summary"]
        ; [:textarea {:id "summary" :name "summary"}]
        [:div {:id "explanation"}
          [:p "Use this format for following section:"]
          [:p "season number|episode number|episode name|summary"]]
        [:label {:for "update"} "Update"]
        [:textarea {:id "update" :name "update"}]
        [:input {:type "submit" :value "Submit"}]]
        [:div {:style (if (not= "ok" (:status results)) "display:none")}
          [:h3 "Updated Catalog Ids"]
          [:ol (map (fn [id] [:li id]) (:catalog_ids results))]]]]))

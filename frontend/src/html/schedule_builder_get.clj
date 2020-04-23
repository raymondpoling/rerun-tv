(ns html.schedule-builder-get
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]))

(defn schedule-options [schedule-names]
  (concat
    (list [:option ""])
    (map (fn [name] [:option name]) schedule-names)))

(defn schedule-builder-get [schedule-names message role]
  (html5 {:lang "en" :dir "ltr"}
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/builder-select.css"}]
      [:title "ReRun TV - Build a Schedule"]]
    [:body
      [:div {:id "content"}
        (header "Build a Schedule" role)
        [:form {:method "post" :action "schedule-builder.html" :class "box"}
          [:h2 {:class "box-center"} "Update Schedule"]
         (vec (concat (list :select
                            {:name "schedule-name"
                             :class "box-center"})
                      (schedule-options schedule-names)))
          [:input {:type "hidden" :name "preview" :value "true"}]
          [:input {:type "submit" :name "mode" :value "Update" :class "box-center"}]]
        [:form {:method "post" :action "schedule-builder.html" :class "box"}
          [:h2 {:class "box-center"} "Create Schedule"]
          [:input {:type "text" :name "schedule-name" :class "box-center"}]
          [:input {:type "hidden" :name "preview" :value "true"}]
          [:input {:type "submit" :name "mode" :value "Create" :class "box-center"}]]
       [:div {:id "message" :style (when (not message) "display:none")}
        (when message message)]]]))

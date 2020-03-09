(ns html.header)

(defn nav []
  [:nav
    [:a {:href "index.html"} "TOP"]
    [:a {:href "preview.html"} "Preview Schedule"]
    [:a {:href "schedule-builder.html"} "Build Schedule"]
    [:a {:href "/logout"} "Logout"]
  ])

(defn header [page-title]
  [:div {:class "header"}
    [:h1 page-title]
    (nav)])

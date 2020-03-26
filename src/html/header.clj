(ns html.header)

(defn nav [role]
  [:nav
    [:a {:href "index.html"} "TOP"]
    (if (some #(= role %) ["admin"])
      [:a {:href "user-management.html"} "Users"])
    [:a {:href "library.html"} "Library"]
    (if (some #(= role %) ["admin" "media"])
      [:a {:href "bulk-update.html"} "Update Series"])
    [:a {:href "preview.html"} "Preview Schedule"]
    (if (some #(= role %) ["admin" "media"])
      [:a {:href "schedule-builder.html"} "Build Schedule"])
    [:a {:href "/logout"} "Logout"]
  ])

(defn header [page-title role]
  [:div {:class "header"}
    [:h1 page-title]
    (nav role)])

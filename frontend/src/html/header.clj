(ns html.header)

(defn- role-matches [role roles & body]
  (when (some #(= role %) roles)
    body))

(defn nav [role]
  [:nav
   [:a {:href "index.html"} "TOP"]
   (role-matches
    role
    ["admin"]
    [:a {:href "message.html"} "Message"])
   (role-matches
    role
    ["admin"]
    [:a {:href "user-management.html"} "Users"])
    [:a {:href "library.html"} "Library"]
   (role-matches
    role
    ["admin" "media"]
      [:a {:href "bulk-update.html"} "Update Series"])
    [:a {:href "preview.html"} "Preview Schedule"]
   (role-matches
    role
    ["admin" "media"]
    [:a {:href "schedule-builder.html"} "Build Schedule"])
   (role-matches
    role
    ["admin" "media"]
    [:a {:href "playlist-builder.html"} "Build Playlist"])
   (role-matches
    role
    ["admin" "media"]
    [:a {:href "exception.html"} "Exceptions"])
   [:a {:href "/logout"} "Logout"]
  ])

(defn header [page-title role]
  [:div {:class "header"}
    [:h1 page-title]
    (nav role)])

(ns html.user-management
  (:require [html.header :refer [header]]
            [hiccup.page :refer [html5]]))

(defn make-user-rows [users]
  (map (fn [{:keys [user email role]}]
    [:tr [:td user] [:td email] [:td role]]) users))

(defn make-role-options [prefix roles]
  (let [role-name (str prefix "role")]
    (list [:label {:for role-name} "Role: "]
      [:select {:name role-name :id role-name}
       (map #(vector :option (when (= "user" %) {:selected :selected}) %)
            roles)])))

(defn make-update [users roles]
  [:form {:action "/role" :method "post"}
    [:h2 "Change User Role"]
    [:ul
      [:li
        [:label {:for "update-user"} "User: "]
        [:select {:name "update-user" :id "update-user"}
        (map #(vector :option(:user %)) users)]]
      [:li
        (make-role-options "update-" roles)]
      [:li [:input {:type "submit"}]]]])

(defn add-user [roles]
  [:form {:action "/user" :method "post"}
    [:h2 "Add User"]
    [:ul
     [:li [:label {:for "new-user"} "User: "]
      [:input {:name "new-user" :id "new-user"}]]
     [:li [:label {:for "new-email"} "E-Mail: "]
      [:input {:name "new-email" :id "new-email"}]]
     [:li [:label {:for "new-password"} "Password: "]
      [:input {:name "new-password" :id "new-password"}]]
     [:li (make-role-options "new-" roles)]
     [:li [:input {:type "submit"}]]]])

(defn user-management [users roles role]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "stylesheet" :href "/css/master.css"}]
      [:link {:rel "stylesheet" :href "/css/user.css"}]
      [:title "ReRun TV - User Update"]]
    [:body
      [:div {:id "content"}
        (header "User Management" role)
        [:div {:class "row-layout"}
          [:div {:class "form-holder"}
            (make-update users roles)
            (add-user roles)]
          [:div {:class "table-holder"}
            [:table
              [:thead [:th "User"] [:th "E-Mail"] [:th "Role"]]
              [:tbody
                (make-user-rows users)]]
            [:div {:class "spacer"}]]]]]))

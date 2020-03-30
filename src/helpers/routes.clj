(ns helpers.routes
  (:require
   [ring.util.response :refer [redirect]]
   [remote-call.messages :refer [add-message]]
   [common-lib.core :as clc]))

(def hosts (clc/make-hosts ["auth" 4007]
                           ["identity" 4012]
                           ["schedule" 4000]
                           ["format" 4009]
                           ["user" 4002]
                           ["playlist" 4001]
                           ["builder" 4003]
                           ["omdb" 4011]
                           ["messages" 4010]
                           ["locator" 4005]))

(defn write-message [{:keys [author title message]}]
  (add-message (:messages hosts)
               author
               title
               message))

(defmacro with-authorized-roles [roles & body]
  (let [sym (gensym)]
    `(fn [~sym]
       (if (not (some #(= (:role (:session ~sym)) %) ~roles))
         (redirect "/index.html")
         (do
           ~@body)))))

(ns playlist-playlist.handler
  (:require [ring.middleware.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response not-found header status]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

  (def app
    (wrap-defaults
      (->
         app-routes
         (json/wrap-json-response)
         (json/wrap-json-body {:keywords? true}))
        (assoc-in site-defaults [:security :anti-forgery] false)))

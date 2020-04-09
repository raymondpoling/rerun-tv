(ns route-logic.message
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as json]
            [ring.util.response :refer [redirect]]
            [remote-call.messages :refer [add-message]]
            [helpers.routes :refer [hosts with-authorized-roles]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [html.message :refer [make-message-page]]
            [cheshire.core :refer [parse-string generate-string]]
            [java-time :as jt]))

(defn get-message [role]
       (with-authorized-roles ["admin"]
         (make-message-page role)))

(defn set-message [title message]
  (fn [{{:keys [role user]} :session}]
    (with-authorized-roles ["admin"]
      (try
        (add-message (:messages hosts)
                     user
                     title
                     message)
        (redirect "/index.html")
        (catch Exception e
          (logger/error "Adding message got: " (.getMessage e))
          (make-message-page role))))))

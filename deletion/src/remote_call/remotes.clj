(ns remote-call.remotes
  (:require [diehard.core :as dh]
            [common-lib.core :as clc]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                                :delay-ms 1000})

(def hosts (clc/make-hosts ["playlist" 4001]
                           ["schedule" 4000]
                           ["meta" 4004]))

(defn- url [atype a-name]
  (case atype
    "playlist" (str "http://" (:playlist hosts) "/" a-name)
    "schedule" (str "http://" (:schedule hosts) "/" a-name)
    "episode" (str "http://" (:meta hosts) "/catalog-id/" a-name)
    "season" (str "http://" (:meta hosts) "/catalog-id/" a-name)
    "series" (str "http://" (:meta hosts) "/catalog-id/" a-name)))

(defn delete-item [atype a-name]
  (clc/log-on-error
   {:status :failure :message "could not execute deletion"}
   (dh/with-circuit-breaker ckt-brkr
     (let [url (url atype a-name)]
       (parse-string
        (:body
         (client/delete url)) true)))))


(ns remote-call.tags
  (:require [diehard.core :as dh]
            [cheshire.core :refer [generate-string]]
            [common-lib.core :as clc]
            [clj-http.client :as client]
            [clojure.string :as cls]
            [clojure.tools.logging :as logger]))

(declare ckt-brkr)

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                                :delay-ms 1000})

(def standard-error [])

(defn include-author? [options author?]
  (merge options (when author? {:author author?})))

(defn fetch-all-tags [host & {:keys [author?] :or {author? nil}}]
  (clc/log-on-error
   standard-error
   (dh/with-circuit-breaker ckt-brkr
     (:tags
      (:body
       (client/get (str "http://" host "/all-tags")
                   {:as :json
                    :query-params (include-author? {} author?)}))))))

(defn fetch-tags [host catalog_id & {:keys [author? type?]
                                     :or {author? nil
                                          type? nil}}]
  (clc/log-on-error
   standard-error
   (dh/with-circuit-breaker ckt-brkr
     (let [url (str "http://" host "/find-by-catalog-id/" catalog_id)
           params (include-author?
                   (when type? {:type type?})
                   author?)]
       (logger/trace "looking up: " url params)
     (:tags
      (:body
       (client/get url
                   (merge {:as :json
                           :query-params params}))))))))

(defn add-tags [host author tags catalog_ids]
  (clc/log-on-error
   standard-error
   (let [url  (str "http://" host "/add/" (cls/join "/" catalog_ids))
         params {:tags (cls/join "," tags)
                 :author author}]
     (logger/trace "Adding: " url params)
   (dh/with-circuit-breaker ckt-brkr
     (:tags (:body
             (client/put
             url
             {:as :json
              :query-params params})))))))

(defn delete-tags [host author tags catalog_id]
  (clc/log-on-error
   standard-error
   (let [url (str "http://" host "/delete/" catalog_id)
         params {:tags (cls/join "," tags)
                 :author author}]
     (logger/trace "Deleting: " url params)
   (dh/with-circuit-breaker ckt-brkr
     (:tags (:body
             (client/delete
              url
              {:as :json
               :query-params params})))))))

(defn find-by-tags [host tags & {:keys [author? type?]
                                 :or {author? nil
                                      type? nil}}]
  (clc/log-on-error
   standard-error
   (let [url (str "http://" host "/find-by-tags")
         params [[:tags (cls/join "," tags)]
                 (when author? [:author author?])
                 (when type? [:type type?])]]
     (dh/with-circuit-breaker ckt-brkr
       (logger/debug "finding by: " tags " type: " type " author: " author)
       (:catalog_ids (:body (client/get
                             url
                             {:as :json
                              :query-params (into {} params)})))))))

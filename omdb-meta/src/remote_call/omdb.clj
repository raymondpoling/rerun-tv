(ns remote-call.omdb
  (:require [diehard.core :as dh]
            [diehard.circuit-breaker :refer [state]]
            [cheshire.core :refer :all]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [clj-http.client :as client]))

(dh/defcircuitbreaker ckt-brkr {:failure-threshold-ratio [8 10]
                              :delay-ms 1000})

(defn lookup-episode [host apikey series season episode]
  (logger/debug (format "Lookup up: http://%s/?apikey=%s&t=%s&season=%S&episode=%S"
    host apikey series season episode))
  (clc/log-on-error {:status "failed" :message "omdb service not available"}
    (let [resp (:body (client/get (str "http://" host "/" )
                { :as :json
                  :query-params {:apikey apikey
                                 :t series
                                 :Season season
                                 :Episode episode
                                 :type "episode"}}))]
      (logger/debug "omdb response: " resp)
      resp)))

(defn lookup-series [host apikey series]
  (clc/log-on-error {:status "failed" :message "omdb service not available"}
    (:body (client/get (str "http://" host "/" )
                         {:as :json
                         :query-params {:apikey apikey
                                        :t series
                                        :type "series"}}))))

(defn imdb-id-lookup [host apikey imdbid]
  (clc/log-on-error
   {:status "failed" :message "omdb service not available"}
   (:body (client/get (str "http://" host "/")
                           {:as :json
                            :query-params {:apikey apikey
                                           :i imdbid
                                           :type "episode"}}))))
(ns route-logic.bulk-update
  (:require [ring.middleware.json :as json]
            [ring.util.response :refer [redirect]]
            [remote-call.meta :refer [get-all-series
                                      bulk-update-series
                                      create-series
                                      create-episode]]
            [remote-call.locator :refer [save-locations]]
            [helpers.routes :refer [hosts write-message]]
            [common-lib.core :as clc]
            [clojure.tools.logging :as logger]
            [html.bulk-update :refer [bulk-update]]
            [cheshire.core :refer [parse-string generate-string]]
            [hiccup.core :refer [html]]))

(defn extract-cat-id [id]
  (let [[_ prefix season episode]
        (re-find #"([A-Z0-5]{5}\d{2})(\d{2})(\d{3})"
                 id)]
    {:prefix prefix
     :season (Integer/parseInt season)
     :episode (Integer/parseInt episode)}))

(defn bulk-update-logic [series update create?]
  (fn [{:keys [session]}]
    (let [series-list (get-all-series (:omdb hosts))]
      (try
        (let [as-map (parse-string update true)
              series-name (or (:name (:series as-map)) series)
              series (:series as-map)
              location-free (map #(dissoc % :locations) (:records as-map))
              record {:series series :records location-free}]
          (if (not create?)
            (let [result (bulk-update-series (:omdb hosts) series-name record)]
              (println "bulk update series result: " result)
              (if (= "ok" (:status result))
                (let [valid-map (map extract-cat-id (:catalog_ids
                                                     result))
                      to-locate (filter
                                 (fn [a]
                                   (some
                                    (fn [t1] (and
                                              (= (:episode a)
                                                 (:episode t1))
                                              (= (:season a)
                                                 (:season t1))))
                                    valid-map)) (:records as-map))]
                  (dorun (map
                          #(save-locations (:locator hosts)
                                           %1 %2)
                          (map #(format "%s%02d%03d"
                                        (:prefix (first valid-map))
                                        (:season %)
                                        (:episode %)) to-locate)
                          (map :locations to-locate)))
                  (println "to locate? " to-locate)
                  (println "validity map? " valid-map)
                  (if (not-empty to-locate)
                    (write-message
                     {:author "System"
                      :title (str (:user session)
                                  " updated "
                                  series-name
                                  " with more data!")
                      :message
                      (html [:ol
                             (map
                              (fn [i] [:li
                                       (str "S" (:season i)
                                            "E" (:episode i)
                                            " " (:episode_name i))])
                              to-locate)])}))))
              (bulk-update series-list result (:role session)))
            (let [series_update (create-series (:omdb hosts) series-name series)
                  all-created (doall
                               (map #(create-episode
                                      (:omdb hosts)
                                      series-name %)
                                    location-free))
                  catalog_ids (map #(first (:catalog_ids %)) all-created)]
              (if (every? #(= "ok" (:status %)) all-created)
                (do
                  (map #(save-locations (:locator hosts) %1 %2) catalog_ids (:locations (:records as-map)))
                  (write-message {:author "System"
                                  :title (str (:user session) " added " series-name "!")
                                  :message (html [:ol (map (fn [i] [:li (str "S" (:season i) "E" (:episode i) " " (:episode_name i))]) (map #(first (:records %)) all-created))])})))
              (bulk-update series-list {:status "ok" :catalog_ids catalog_ids} (:role session)))))
        (catch com.fasterxml.jackson.core.JsonParseException e
          (bulk-update series-list
                       {:status "failed" :message (.getMessage e)}
                       (:role session)))))))

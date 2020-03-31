(ns route-logic.bulk-update
  (:require [ring.middleware.json :as json]
            [ring.util.response :refer [redirect]]
            [remote-call.meta :refer [get-all-series
                                      bulk-update-series
                                      bulk-create-series
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
  (let [[_ key]
        (re-find #"[A-Z0-5]{5}\d{2}(\d{5})" id)]
    key))

(defn make-record-map [records]
  (into {} (map #(let [id (format "%02d%03d"
                               (:season %)
                               (:episode %))]
                   [id %]) records)))

(defn make-locatable [location-map catalog-ids]
  (map #(vector % (get location-map (subs % 7))) catalog-ids))

(defn save-pairs [pairs]
  (doall (map #(let [[catalog-id record] %]
                 (save-locations (:locator hosts)
                                 catalog-id
                                 (:locations record))) pairs)))

(defn make-or-update [series-name series-list records report update? session]
  (let [series (:series records)
        location-free (map #(dissoc % :locations) (:records records))
        records-no-location {:series series :records location-free}
        result (if update?
                 (bulk-update-series (:omdb hosts)
                                     series-name
                                     records-no-location)
                 (bulk-create-series (:omdb hosts)
                                     series-name
                                     records-no-location))
        record-map (make-record-map (:records records))
        location-pairs (make-locatable record-map (:catalog_ids result))]
    (println "result? " result)
    (save-pairs location-pairs)
    (if (not-empty location-pairs)
      (write-message
       {:author "System"
        :title (str (:user session)
                    report)
        :message
        (html [:ol
               (map
                (fn [i] [:li
                         (str "S" (:season i)
                              "E" (:episode i)
                              " " (:episode_name i))])
                (map second location-pairs))])}))
    (bulk-update series-list result (:role session))))

(defn bulk-update-logic [series update create?]
  (fn [{:keys [session]}]
    (let [series-list (get-all-series (:omdb hosts))]
      (try
        (let [series-name (or (:name (:series update)) series)
              records (parse-string update true)]
          (if (not create?)
            (make-or-update series-name series-list
                            records (str "Updated series '" series-name "'!")
                            true session)
            (make-or-update series-name series-list
                            records (str "Added new series '" series-name "'!")
                            false session)))
        (catch com.fasterxml.jackson.core.JsonParseException e
          (bulk-update series-list
                       {:status "failed" :message (.getMessage e)}
                       (:role session)))))))

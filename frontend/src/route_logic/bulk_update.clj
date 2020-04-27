(ns route-logic.bulk-update
  (:require [remote-call.meta :refer [get-all-series
                                      bulk-update-series
                                      bulk-create-series
                                      get-meta-by-catalog-id]]
            [remote-call.locator :refer [save-locations]]
            [helpers.routes :refer [hosts write-message]]
            [html.bulk-update :refer [bulk-update]]
            [cheshire.core :refer [parse-string]]
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
  (map #(vector % (get location-map (subs % 7)))
       (filter not-empty catalog-ids)))

(defn save-pairs [pairs]
  (map #(let [[catalog-id record] %]
          (save-locations (:locator hosts)
                          catalog-id
                          (:locations record))) pairs))

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
        location-pairs (make-locatable record-map (:catalog_ids result))
        saved? (save-pairs location-pairs)
        get-saved (map #(first
                         (:records
                          (get-meta-by-catalog-id
                           (:omdb hosts)
                           (first %)))) location-pairs)]
    (when (every? #(= "ok" (:status %)) saved?)
      (write-message
       {:author "System"
        :title (str (:user session)
                    report)
        :message
        (html [:ol
               (map
                (fn [i] [:li
                         (str (:series i)
                          "S" (:season i)
                          "E" (:episode i)
                          " " (:episode_name i))])
                get-saved)])}))
    (bulk-update series-list result (:role session))))

(defn bulk-update-logic [series update create?]
  (fn [{:keys [session]}]
    (let [series-list (get-all-series (:omdb hosts))]
      (try
        (let [records (parse-string update true)
              series-name (or (:name (:series records)) series)]
          (if (not create?)
            (make-or-update series-name series-list
                            records (str " updated series '" series-name "'!")
                            true session)
            (make-or-update series-name series-list
                            records (str " added new series '" series-name "'!")
                            false session)))
        (catch com.fasterxml.jackson.core.JsonParseException e
          (bulk-update series-list
                       {:status "failed" :message (.getMessage e)}
                       (:role session)))))))

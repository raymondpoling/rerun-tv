(ns route-logic.preview-logic
  (:require
   [helpers.routes :refer [hosts]]
   [remote-call.format :refer [fetch-playlist]]
   [remote-call.schedule :refer [get-schedule-items
                                 get-schedules]]
   [remote-call.meta :refer [get-meta-by-catalog-id]]
   [remote-call.playlist :refer [fetch-catalog-id]]
   [remote-call.user :refer [fetch-index]]
   [html.preview :refer [make-preview-page]]
   [clojure.tools.logging :as logger]
   [ring.util.response :refer [response not-found header status redirect]]))

(defn- fetch-preview-frame [schedule-name index]
  (let [items (get-schedule-items (:schedule hosts) schedule-name index)
        catalog_ids (map #(fetch-catalog-id (:playlist hosts)
                                            (:name %)
                                            (:index %))
                         items)
        meta (flatten (map #(:records (get-meta-by-catalog-id
                                       (:omdb hosts)
                                       (:item %)))
                           catalog_ids))]
    (map merge meta items)))

(defn fetch-or-zero [user sched]
  (let [idx (fetch-index (:user hosts) user sched true)]
    (if (= :failed (:status idx))
      "0"
      idx)))

(defn create-preview [schedule role user index idx update reset download]
      (let [schedule-list (get-schedules (:schedule hosts))
            sched (or schedule (first schedule-list))
            idx (Integer/parseInt (str
              (or (if reset (fetch-or-zero user sched))
                (not-empty index)
                idx
                (fetch-or-zero user sched))))]
            (logger/debug (str "with user: " user " index: " idx " and schedule: " sched))
      (if download
        (let [params {:index index :update update}
              resp (fetch-playlist (:format hosts) user sched params)]
          (logger/debug "RESP IS ***" resp "*** RESP IS")
          (logger/debug "header? " (get (:headers resp) "Content-Type"))
          (-> (response (:body resp))
              (status 200)
              (header "content-type" (get (:headers resp) "Content-Type"))
              (header "content-disposition" (get (:headers resp) "Content-Disposition"))))
        (let [current (fetch-preview-frame sched idx)
              previous (fetch-preview-frame sched (- idx 1))
              next (fetch-preview-frame sched (+ idx 1))]
          (make-preview-page sched schedule-list idx update previous current next role)))))

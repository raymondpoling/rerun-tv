(ns route-logic.preview-logic
  (:require
   [helpers.routes :refer [hosts]]
   [remote-call.format :refer [fetch-playlist
                               fetch-formats]]
   [remote-call.schedule :refer [get-schedules]]
   [remote-call.user :refer [fetch-index]]
   [html.preview :refer [make-preview-page]]
   [clojure.tools.logging :as logger]
   [ring.util.response :refer [header status response]]
   [cheshire.core :refer [parse-string]]
   [clojure.string :as cls]))

(defn fetch-or-zero [user sched]
  (let [idx (fetch-index (:user hosts) user sched true)]
    (if (= :failed (:status idx))
      "0"
      idx)))

(defn create-preview [schedule role user index idx update
                      reset download select-format]
  (let [schedule-list (get-schedules (:schedule hosts))
        sched (or schedule (first schedule-list))
        [protocol host format] (if (not-empty select-format)
                                 (cls/split select-format #"/")
                                 [nil nil nil])
        idx (Integer/parseInt (str
                               (or (when reset (fetch-or-zero user sched))
                                   (not-empty index)
                                   idx
                                   (fetch-or-zero user sched))))]
    (logger/debug (str "with user: " user " index: " idx " and schedule: " sched))
    (logger/debug "select-format" select-format
                  "protocol" protocol
                  "host" host)
    (if download
      (let [params {:index index :update update
                    :host host :protocol protocol :format format}
            resp (fetch-playlist (:format hosts) user sched params)]
        (logger/debug "RESP IS ***" resp "*** RESP IS")
        (logger/debug "params?" params)
        (logger/debug "header? " (get (:headers resp) "Content-Type"))
        (case format
          "m3u" (-> (response (:body resp))
                    (status 200)
                    (header "content-type" (get (:headers resp)
                                                "Content-Type"))
                    (header "content-disposition"
                            (get (:headers resp)
                                 "Content-Disposition")))
          (-> (response (:body resp))
              (status 200)
              (header "content-type" "application/json")
              (header "content-disposition"
                      (str "attachment; filename=\"" sched "-"
                           idx ".json\"")))))
      (let [frames (map #(:playlist
                          (parse-string
                           (:body (fetch-playlist (:format hosts)
                                                  user
                                                  sched
                                                  {:index %})) true))
                        (range (- idx 1) (+ idx 2)))
            r (:body (fetch-formats (:format hosts)))
            formats (when (= (:status r) "ok")
                      (:formats r))]
        (make-preview-page sched schedule-list (- idx 1)
                           update frames formats select-format role)))))

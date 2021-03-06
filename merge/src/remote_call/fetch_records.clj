(ns remote-call.fetch-records
  (:require
    [remote-call.locator :refer [get-locations get-protocol-host]]
    [remote-call.playlist :refer [get-catalog-id]]
    [remote-call.schedule :refer [get-schedule]]
    [remote-call.meta :refer [get-meta]]))

(defn if-valid-return [service validator remote-call]
  (fn [input]
    (let [ret (remote-call input)]
      (if (validator ret) ; If validator is TRUE a failed condition occurred
        {:status "failure" :message (str service " service not available")}
        ret))))

(defn run-while-valid
  ([input] input)
  ([input & body]
    (if (= "failure" (:status input))
      input
      (apply run-while-valid (cons ((first body) input) (rest body))))))


;; This should be cleaned up.
(defn fetch [schedule-host playlist-host locator-host
             meta-host index schedule-name host protocol]
  (run-while-valid
   index
   (if-valid-return "schedule" nil?
                    #(get-schedule schedule-host schedule-name %))
   (if-valid-return "playlist"
     #(some (fn [t] (nil? (:catalog_id t))) %)
     #(map
       (fn [item]
         {:catalog_id (get-catalog-id playlist-host
                                      (:name item)
                                      (:index item))
          :playlist item})
       %))
   (if-valid-return "locator"
     #(some (fn [t] (nil? (:locations t))) %)
     #(map
       (fn [item]
         (if (and host protocol)
           (merge item
                  {:locations (get-protocol-host locator-host
                                                 protocol host
                                                 (:catalog_id item))})
           (merge item
                  {:locations (get-locations locator-host
                                             (:catalog_id item))}))) %))
   (if-valid-return "meta"
     #(some (fn [t]
              (= 0 (count (dissoc t :locations :playlist)))) %)
     #(map
       (fn [item]
         (assoc (get-meta meta-host (:catalog_id item))
                :playlist (:playlist item)
                :locations (:locations item))) %))))

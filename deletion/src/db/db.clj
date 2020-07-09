(ns db.db
  (:require [clojure.java.jdbc :as j]))

(def database (atom {:dbtype "mysql"
                     :dbname "deletion"
                     :user nil
                     :password nil
                     :serverTimezone "America/New_York"}))

(defn initialize
  ([]
   (swap! database (fn [_ s] s) {:dbtype "h2:mem" :dbname "deletion"}))
  ([name password host port]
   (swap! database merge {:user name :password password :host host :port port})))

(defn- get-nominations [t1]
  (j/query t1
           [(str "SELECT type, name "
                 "FROM deletion.record "
                 "WHERE status = 'NOM' "
                 "FOR UPDATE")]))

(defn create-record [atype a-name user reason]
  (j/with-db-transaction [t1 @database]
    (let [values (get-nominations t1)
          record {:name a-name
                  :maker user
                  :type atype
                  :reason reason
                  :status "NOM"}]
      (if (not-any? #(= {:type atype :name a-name} %) values)
        (j/insert! t1 "deletion.record"
                   record)
        (throw (Exception. "Nomination already exists."))))))

(defn get-outstanding []
  (get-nominations @database))

(defn get-records []
  (map #(dissoc % :id)
       (j/query @database
                ["SELECT * FROM deletion.record ORDER BY id DESC LIMIT 10"])))

(defn reject [atype a-name user reason]
  (j/with-db-transaction [t1 @database]
    (let [record (first (j/query t1 [(str "SELECT id, reason, maker "
                                          "FROM deletion.record "
                                          "WHERE type = ? "
                                          "AND name = ? "
                                          "AND status = 'NOM' "
                                          "FOR UPDATE") atype a-name]))]
      (when (= (:maker record) user)
        (throw (Exception. "Nominator cannot also reject.")))
      (if (not-empty record)
        (j/update! t1 "deletion.record"
                   {:status "REJ"
                    :checker user
                    :reason (str (:reason record) "\n" reason)}
                   ["id = ?" (:id record)])
        (throw (Exception. "No nominating record exists."))))))

(defn execute [atype a-name user reason fun]
  (j/with-db-transaction [t1 @database]
    (let [record (first (j/query t1 [(str "SELECT id, reason, maker "
                                          "FROM deletion.record "
                                          "WHERE type = ? "
                                          "AND name = ? "
                                          "AND status = 'NOM' "
                                          "FOR UPDATE") atype a-name]))]
      (when (= (:maker record) user)
        (throw (Exception. "Nominator cannot also delete.")))
      (if (not-empty record)
        (do
          (fun atype a-name)
          (j/update! t1 "deletion.record"
                     {:status "EXE"
                      :checker user
                      :reason (str (:reason record) "\n" reason)}
                     ["id = ?" (:id record)]))
        (throw (Exception. "No nominating record exists."))))))

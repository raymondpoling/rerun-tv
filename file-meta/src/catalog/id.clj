(ns catalog.id
  (:require [clojure.string :as cls]))

(def screened-words [
  "THE", "IN", "AND", "OF", "A", "AN", "OR", "IS", "TO"
  ])

(defn- alphanumeric [string]
  (filter #(Character/isLetterOrDigit %) string))

(defn- screen-words [arr]
    (filter #(not (some #{%} screened-words)) arr))

(defn- check-length [original_array modified_array]
  (let [ma (cls/join modified_array)]
    (if (< (count ma) 5)
      (cls/join original_array)
      ma)))

(defn create-id [name]
  (let [upcase-name (cls/upper-case name)
        seperated-values (cls/split upcase-name #"\s")
        screened (screen-words seperated-values)]
        (cls/join (take 5 (alphanumeric (cls/join [(check-length seperated-values screened) "0000"]))))))

(defn next-id [name]
  (let [root (cls/join (take 5 name))
        int (+ 1 (Integer/parseInt (cls/join (drop 5 name))))]
        (format "%5s%02d" root int)))

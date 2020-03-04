(ns catalog.id)

(def screened-words [
  "THE", "IN", "AND", "OF", "A", "AN", "OR", "IS", "TO"
  ])

(defn- alphanumeric [string]
  (filter #(Character/isLetterOrDigit %) string))

(defn- screen-words [arr]
    (filter #(not (some #{%} screened-words)) arr))

(defn- check-length [original_array modified_array]
  (let [ma (clojure.string/join modified_array)]
    (if (< (count ma) 5)
      (clojure.string/join original_array)
      ma)))

(defn create-id [name]
  (let [upcase-name (clojure.string/upper-case name)
        seperated-values (clojure.string/split upcase-name #"\s")
        screened (screen-words seperated-values)]
        (clojure.string/join (take 5 (alphanumeric (clojure.string/join [(check-length seperated-values screened) "0000"]))))))

(defn next-id [name]
  (let [root (clojure.string/join (take 5 name))
        int (+ 1 (Integer/parseInt (clojure.string/join (drop 5 name))))]
        (format "%5s%02d" root int)))

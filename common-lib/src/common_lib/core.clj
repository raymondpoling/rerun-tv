(ns common-lib.core
  (:require [ring.util.response :refer [response header status]]
            [clojure.tools.logging :as logger]
            [clojure.string :as cls]))

(defn make-host [prefix default-port]
  (let [upcase-prefix (cls/upper-case prefix)
        host (or (System/getenv (str upcase-prefix "_HOST"))
                  (str prefix ":" default-port))]
  [(keyword prefix) host]))

(defn make-hosts [& hosts]
  (into {} (map #(make-host (first %) (second %)) hosts)))

(defn make-response [st resp]
  (-> (response resp)
      (status st)
      (header "content-type" "application/json")))

(defn not-exceptional [] (fn [input] (or (= input 404) (= input 200))))

(defmacro log-on-error [error-result & body]
  (let [error (gensym 'e)]
    `(try ~@body
     (catch Exception ~error
       (logger/error ~error)
       ~error-result))))

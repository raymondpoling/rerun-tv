(ns common-lib.core
  (:require [ring.util.response :refer [response header status]]
            [clojure.tools.logging :as logger]))

(defn make-host [prefix default-port]
  (let [upcase-prefix (clojure.string/upper-case prefix)
        host (or (System/getenv (str upcase-prefix "_HOST"))
                  prefix)
        port (or (System/getenv (str upcase-prefix "_PORT")) default-port)]
  [(keyword prefix) (str host ":" port)]))

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

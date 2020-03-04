(ns remote-call.util)

(defn not-exceptional [] (fn [input] (or (= input 404) (= input 200))))

(defmacro log-on-error [error-result & body]
  (let [error (gensym 'e)]
    `(try ~@body
     (catch Exception ~error
       (logging/error ~error)
       ~error-result))))

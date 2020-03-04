(ns file-locator.url)

(defmulti make-url #(:protocol %))

(defmethod make-url "file" [x]
  (str "file://" (:url x)))

(defn- add-seperator [left right]
  (if (= (subs right 0 1) "/")
      (str left right)
      (str left "/" right)))

(defmethod make-url :default [x]
  (str (:protocol x) "://" (add-seperator (:host x) (:url x))))

(ns common-lib.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [common-lib.core :refer [make-hosts]]))

(deftest make-hosts-tests
  (testing "Make a list of hosts"
    (let [actual (make-hosts ["cat" 4000]
                             ["dog" 5000])
          expected {:cat "cat:4000" :dog "dog:5000"}]
    (is (= expected actual)))))

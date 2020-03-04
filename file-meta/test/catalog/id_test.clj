(ns catalog.id-test
  (:require [clojure.test :refer :all]
            [catalog.id :refer :all]))

(deftest catalog-id-test
  (testing "create a simple id"
    (let [input "Zombie 5"
          expected "ZOMBI"
          actual (create-id input)]
      (is (= expected actual))))
  (testing "create an id ignoring certain words"
    (let [input "The Zombie"
          expected "ZOMBI"
          actual (create-id input)]
      (is (= expected actual))))
  (testing "use ignored words"
    (let [input "the zom"
          expected "THEZO"
          actual (create-id input)]
      (is (= expected actual))))
  (testing "pad a short id"
    (let [input "ZOM"
          expected "ZOM00"
          actual (create-id input)]
      (is (= expected actual))))
  (testing "strip non-alphanumeric characters"
    (let [input "Zom-bie Apocalypse"
          expected "ZOMBI"
          actual (create-id input)]
      (is (= expected actual)))))

(deftest catalog-next-id-test
  (testing "on conflict, update id 1"
    (let [input "ZOMBI01"
          expected "ZOMBI02"
          actual (next-id input)]
      (is (= expected actual))))
  (testing "on conflict, update id 12"
    (let [input "ZOMBI12"
          expected "ZOMBI13"
          actual (next-id input)]
      (is (= expected actual)))))

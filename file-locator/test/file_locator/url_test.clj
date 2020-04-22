(ns file-locator.url-test
  (:require [clojure.test :refer [deftest is testing]]
            [file-locator.url :refer [make-url]]))

(deftest test-app
  (testing "file route"
    (let [expected "file:///home/ruguer/Videos/test-me/season 1/1-1.mkv"
          actual (make-url {:protocol "file"
                            :url "/home/ruguer/Videos/test-me/season 1/1-1.mkv"
                            :host "localhost"})]
      (is (= expected actual))))

  (testing "ssh route"
    (let [expected "ssh://CrystalBall/home/ruguer/Videos/test-me/season 1/1-1.mkv"
          actual (make-url {:protocol "ssh"
                            :url "/home/ruguer/Videos/test-me/season 1/1-1.mkv"
                            :host "CrystalBall"})]
      (is (= expected actual)))))

(ns remote-call.validate-test
  (:require [remote-call.validate :refer [validate-schedule]]
            [clojure.test :refer [deftest is testing]]))

(deftest test-validation
  (testing "just a playlist"
    (let [testing {:name "something"
                   :playlists [{:type "playlist" :name "yap" :length 12}]}
          playlist-map {"yap" 12}
          expected {:status :ok}
          actual (validate-schedule playlist-map "something" testing)]
      (is (= expected actual))))

  (testing "full playlist passing"
    (let [testing
          {:name "something"
           :playlists [
                       {:type "complex"
                        :playlists [{:type "playlist"
                                     :name "a"
                                     :length 13}
                                    {:type "playlist"
                                     :name "b"
                                     :length 14}]}
                       {:type "merge"
                        :playlists [{:type "playlist"
                                     :name "c"
                                     :length 12}
                                    {:type "playlist"
                                     :name "d"
                                     :length 16}
                                    ]}
                       {:type "multi" :start 0 :step 1
                        :playlist {:type "playlist"
                                   :name "e"
                                   :length 18}}
                       {:type "playlist" :name "f" :length 20}]}
          playlist-map {"a" 13 "b" 14 "c" 12 "d" 16 "e" 18 "f" 20}
          expected {:status :ok}
          actual (validate-schedule playlist-map "something" testing)]
      (is (= expected actual))))

  (testing "full playlist failing"
    (let [testing
          {:name "something"
           :playlists [
                       {:type "complex"
                        :playlists [{:type "playlist" :name "a" :length 12}
                                    {:type "playlist" :name "b" :length 14}]}
                       {:type "merge"
                        :playlists [{:type "playlist" :name "c" :length 11}
                                    {:type "playlist" :name "d" :length 16}
                                    ]}
                       {:type "multi" :start 0 :step 1
                        :playlist {:type "playlist" :name "e" :length 18}}
                       {:type "playlist" :name "f" :length 14}]}
          playlist-map {"a" 13 "b" 14 "c" 12 "d" 16 "e" 18 "f" 20}
          expected {:status :invalid
                    :messages ["Failed Validation: a",
                               "Failed Validation: c",
                               "Failed Validation: f"]}
          actual (validate-schedule playlist-map "something" testing)]
      (is (= (:status expected) (:status actual)))
      (is (= (:message expected) (:message actual))))))

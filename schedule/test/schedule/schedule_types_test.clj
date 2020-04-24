(ns schedule.schedule-types-test
  (:require [clojure.test :refer [deftest is testing]]
            [schedule.schedule-types :refer [->Complex
                                             ->Item
                                             ->Merge
                                             ->Multi
                                             ->Playlist
                                             ->Schedule
                                             index
                                             length
                                             make-sched-type-from-json
                                             make-schedule-from-json]]))

(deftest playlist-test
  (testing "Playlist length test"
    (let [a (->Playlist "a" 12)
          b (->Playlist "b" 23)]
      (is (= (length a) 12))
      (is (= (length b) 23))))

  (testing "Playlist index test"
    (let [a (->Playlist "a" 12)
          b (->Playlist "b" 23)
          c (take 32 (cycle (for [i (range 12)] (->Item "a" i))))
          d (take 32 (cycle (for [i (range 23)] (->Item "b" i))))]
      (is (= (for [i (range 32)] (index a i)) c))
      (is (= (for [i (range 32)] (index b i)) d)))))

(deftest merge-testing
  (let [a (->Playlist "a" 12)
        b (->Playlist "b" 23)
        c (->Playlist "c" 11)
        d (->Playlist "d" 33)
        e (->Merge [a b c])
        f (->Merge [c d])
        g (->Merge [a d])
        expectation-e (take 200 (cycle (concat (for [i (range 12)] (->Item "a" i))
                                               (for [i (range 23)] (->Item "b" i))
                                               (for [i (range 11)] (->Item "c" i)))))
        expectation-f (take 200 (cycle (concat (for [i (range 11)] (->Item "c" i))
                                               (for [i (range 33)] (->Item "d" i)))))
        expectation-g (take 200 (cycle (concat (for [i (range 12)] (->Item "a" i))
                                               (for [i (range 33)] (->Item "d" i)))))]
    (testing "Merge length test"
      (is (= (length e) 46))
      (is (= (length f) 44))
      (is (= (length g) 45)))

    (testing "Merge index test"
      (is (= (for [i (range 200)] (index e i)) expectation-e))
      (is (= (for [i (range 200)] (index f i)) expectation-f))
      (is (= (for [i (range 200)] (index g i)) expectation-g)))))

(deftest complex-testing
  (let [a (->Playlist "a" 12)
        b (->Playlist "b" 24)
        c (->Playlist "c" 11)
        d (->Playlist "d" 33)
        e (->Complex [a b c])
        f (->Complex [c d])
        g (->Complex [a d])
        expectation-e (take 200 (interleave (cycle (for [i (range 12)] (->Item "a" i)))
                                            (cycle (for [i (range 24)] (->Item "b" i)))
                                            (cycle (for [i (range 11)] (->Item "c" i)))))
        expectation-f (take 200 (interleave (cycle (for [i (range 11)] (->Item "c" i)))
                                            (cycle (for [i (range 33)] (->Item "d" i)))))
        expectation-g (take 200 (interleave (cycle (for [i (range 12)] (->Item "a" i)))
                                            (cycle (for [i (range 33)] (->Item "d" i)))))]
    (testing "Complex length test"
      (is (= (length e) 792))
      (is (= (length f) 66))
      (is (= (length g) 264)))

    (testing "Complex index test"
      (is (= (for [i (range 200)] (index e i)) expectation-e))
      (is (= (for [i (range 200)] (index f i)) expectation-f))
      (is (= (for [i (range 200)] (index g i)) expectation-g)))))

(deftest multi-testing
  (let [a (->Playlist "a" 12)
        b (->Playlist "b" 25)
        e (->Multi a 0 2)
        f (->Multi a 1 2)
        g (->Multi b 0 3)
        h (->Multi b 1 3)
        j (->Multi b 2 3)
        expectation-e (take 200 (cycle (for [i (range 0 12 2)] (->Item "a" i))))
        expectation-f (take 200 (cycle (for [i (range 1 12 2)] (->Item "a" i))))
        expectation-g (take 200 (cycle (concat (for [i (range 0 25 3)] (->Item "b" i))
                                               (for [i (range 2 25 3)] (->Item "b" i))
                                               (for [i (range 1 25 3)] (->Item "b" i)))))
        expectation-h (take 200 (cycle (concat (for [i (range 1 25 3)] (->Item "b" i))
                                               (for [i (range 0 25 3)] (->Item "b" i))
                                               (for [i (range 2 25 3)] (->Item "b" i)))))
        expectation-j (take 200 (cycle (concat (for [i (range 2 25 3)] (->Item "b" i))
                                               (for [i (range 1 25 3)] (->Item "b" i))
                                               (for [i (range 0 25 3)] (->Item "b" i)))))]
    (testing "Multi length test"
      (is (= (length e) 6))
      (is (= (length f) 6))
      (is (= (length g) 25))
      (is (= (length h) 25))
      (is (= (length j) 25)))

    (testing "Multi index test"
      (is (= (for [i (range 200)] (index e i)) expectation-e))
      (is (= (for [i (range 200)] (index f i)) expectation-f))
      (is (= (for [i (range 200)] (index g i)) expectation-g))
      (is (= (for [i (range 200)] (index h i)) expectation-h))
      (is (= (for [i (range 200)] (index j i)) expectation-j)))))

(deftest make-playlist
  (testing "Make a good playlist"
    (is (= (assoc (->Playlist "cobalt" 27) :type "playlist")
           (make-sched-type-from-json
            {:type "playlist" :name "cobalt" :length 27}))))
  (testing "Throws error if missing name"
    (is (thrown-with-msg? Exception #"^Invalid playlist missing name: .*"
                          (make-sched-type-from-json {:type "playlist" :length 43}))))
  (testing "Throws error if missing length"
    (is (thrown-with-msg? Exception #"^Invalid playlist missing length: .*"
                          (make-sched-type-from-json {:type "playlist" :name "cobalt"})))))

(deftest make-merge
  (testing "Make a good merge"
    (is (= (assoc (->Merge [(assoc (->Playlist "cobalt" 27) :type "playlist")
                            (assoc (->Playlist "crabs" 12) :type "playlist")])
                  :type "merge")
           (make-sched-type-from-json
            {:type "merge" :playlists [
                                       {:type "playlist" :name "cobalt" :length 27}
                                       {:type "playlist" :name "crabs" :length 12}]}))))
  (testing "Throws error if missing playlists"
    (is (thrown-with-msg? Exception #"^Invalid merge missing playlists: .*"
                          (make-sched-type-from-json
                           {:type "merge" :play [{:type "playlist" :name "cobalt" :length 24}]}))))
  (testing "Throws error if no elements"
    (is (thrown-with-msg? Exception #"^Invalid merge playlists empty: .*"
                          (make-sched-type-from-json
                           {:type "merge" :playlists []}))))
  (testing "Throws error if not seq"
    (is (thrown-with-msg? Exception #"^Invalid merge playlists not a seq: .*"
                          (make-sched-type-from-json
                           {:type "merge" :playlists :cat}))))
  ;; following test doesn't run
                                        ; (testing "Throws error if not super playlist"
                                        ;   (is (thrown-with-msg? Exception #"^Invalid playlist missing name: .*"
                                        ;       (make-sched-type-from-json
                                        ;         {:type "merge" :playlists [
                                        ;           {:type "playlist" :name "cobalt" :length 24}
                                        ;           {:type "playlist" :nam "cobalt" :length 24}]}))))
  )

(deftest make-multi
  (testing "Make a good multi"
    (is (= (assoc (->Multi (assoc (->Playlist "cobalt" 27) :type "playlist") 0 3) :type "multi")
           (make-sched-type-from-json
            {:type "multi" :playlist {:type "playlist" :name "cobalt" :length 27}
             :start 0 :step 3}))))
  (testing "Throws error if missing playlist"
    (is (thrown-with-msg? Exception #"^Invalid multi missing playlist: .*"
                          (make-sched-type-from-json
                           {:type "multi" :play {:type "playlist" :name "cobalt" :length 24}
                            :start 0 :step 3}))))
  (testing "Throws error if missing start"
    (is (thrown-with-msg? Exception #"^Invalid multi playlist no start: .*"
                          (make-sched-type-from-json
                           {:type "multi" :playlist :cat :step 3}))))
  (testing "Throws error if missing step"
    (is (thrown-with-msg? Exception #"^Invalid multi playlist no step: .*"
                          (make-sched-type-from-json
                           {:type "multi" :playlist :cat :start 0})))))

(deftest make-complex
  (testing "Make a good complex"
    (is (= (assoc (->Complex [(assoc (->Playlist "cobalt" 27) :type "playlist")
                              (assoc (->Playlist "crabs" 12) :type "playlist")])
                  :type "complex")
           (make-sched-type-from-json
            {:type "complex"
             :playlists [
                         {:type "playlist" :name "cobalt" :length 27}
                         {:type "playlist" :name "crabs" :length 12}]}))))
  (testing "Throws error if missing playlists"
    (is (thrown-with-msg? Exception #"^Invalid complex missing playlists: .*"
                          (make-sched-type-from-json
                           {:type "complex" :play [{:type "playlist" :name "cobalt" :length 24}]}))))
  (testing "Throws error if no elements"
    (is (thrown-with-msg? Exception #"^Invalid complex playlists empty: .*"
                          (make-sched-type-from-json
                           {:type "complex" :playlists []}))))
  (testing "Throws error if not seq"
    (is (thrown-with-msg? Exception #"^Invalid complex playlists not a seq: .*"
                          (make-sched-type-from-json
                           {:type "complex" :playlists :cat})))))

(deftest make-schedule
  (testing "Make a good schedule"
    (is (= (->Schedule "test"
                       [(assoc (->Playlist "cobalt" 27) :type "playlist")
                        (assoc (->Playlist "crabs" 12) :type "playlist")])
           (make-schedule-from-json
            {:name "test"
             :playlists [
                         {:type "playlist" :name "cobalt" :length 27}
                         {:type "playlist" :name "crabs" :length 12}]}))))
  (testing "Throws error if missing playlists"
    (is (thrown-with-msg? Exception #"^Invalid schedule missing playlists: .*"
                          (make-schedule-from-json
                           {:name "test" :play [{:type "playlist" :name "cobalt" :length 24}]}))))
  (testing "Throws error if no elements"
    (is (thrown-with-msg? Exception #"^Invalid schedule playlists empty: .*"
                          (make-schedule-from-json
                           {:name "test" :playlists []}))))
  (testing "Throws error if not seq"
    (is (thrown-with-msg? Exception #"^Invalid schedule playlists not a seq: .*"
                          (make-schedule-from-json
                           {:name "test" :playlists :cat}))))
  (testing "Throws error if no name"
    (is (thrown-with-msg? Exception #"^Invalid schedule missing name: .*"
                          (make-schedule-from-json
                           {:nam "test" :playlists :cat})))))

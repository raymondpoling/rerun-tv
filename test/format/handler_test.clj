(ns format.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [format.handler :refer :all]
            [cheshire.core :refer :all])
   (:use clj-http.fake))

(deftest test-app
  (let [route-maps {"http://schedule:4000/test-schedule/5"
                                  (fn [result] {:status 200 :headers {}
                                  :body (generate-string [{:name "Series 1" :index 5},{:name "Series 2", :index 3}])})
                                  "http://user:4002/test-user/test-schedule"
                                  (fn [result] {:status 200 :headers {}
                                  :body (generate-string {:status :ok, :idx 5})})
                                  "http://playlist:4001/Series%201/5"
                                  (fn [result] {:status 200 :headers {}
                                  :body "SERIE0101005"})
                                  "http://playlist:4001/Series%202/3"
                                  (fn [result] {:status 200 :headers {}
                                  :body "SERIE0201005"})
                                  "http://locator:4006/file/CrystalBall/SERIE0101005"
                                  (fn [result] {:status 200 :headers {}
                                    :body (generate-string {:url "file://home/Video/Series 1/series 1 D1-5.mkv"})})
                                  "http://locator:4006/file/CrystalBall/SERIE0201005"
                                  (fn [result] {:status 200 :headers {}
                                    :body (generate-string {:url "file://home/Video/Series 2/series 2 D2-2.mkv"})})
                                  "http://meta:4004/catalog-id/SERIE0101005?fields=season,episode_name,series,episode"
                                  (fn [result] {:status 200 :headers {}
                                    :body (generate-string {:records [{:episode 5 :series "Series 1" :season 1}]})})
                                  "http://meta:4004/catalog-id/SERIE0201005?fields=season,episode_name,series,episode"
                                  (fn [result] {:status 200 :headers {}
                                    :body (generate-string {:records [{:episode 3 :series "Series 2" :season 1}]})})}]
    (testing "get an m3u record"
      (with-fake-routes-in-isolation route-maps
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected (str "#EXTM3U\n"
                            "#PLAYLIST: test-schedule - 5\n"
                            "\n"
                            "#EXTINF:0, Series 1 S1E5\n"
                            "file://home/Video/Series 1/series 1 D1-5.mkv\n"
                            "\n"
                            "#EXTINF:0, Series 2 S1E3\n"
                            "file://home/Video/Series 2/series 2 D2-2.mkv\n")]
          (is (= (:status response) 200))
          (is (= (:body response) expected)))))
    (testing "propogate an error if schedule service fails"
      (with-fake-routes-in-isolation (merge route-maps {"http://schedule:4000/test-schedule/5"
                                      (fn [result] {:status 500 :headers {}
                                      :body nil})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "schedule service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "propogate an error if user service fails"
      (with-fake-routes-in-isolation (merge route-maps {"http://user:4002/test-user/test-schedule"
                                      (fn [result] {:status 500 :headers {}
                                      :body (generate-string {:status :ok, :idx 5})})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "user service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "propogate an error if playlist service fails"
      (with-fake-routes-in-isolation (merge route-maps {"http://playlist:4001/Series%201/5"
                                                        (fn [result] {:status 500 :headers {}
                                                        :body ""})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "playlist service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "propogate an error if locator service fails"
      (with-fake-routes-in-isolation (merge route-maps {"http://locator:4006/file/CrystalBall/SERIE0101005"
                                              (fn [result] {:status 400 :headers {}
                                                :body (generate-string {:url "file://home/Video/Series 1/series 1 D1-5.mkv"})})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "locator service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "propogate an error if meta service fails"
      (with-fake-routes-in-isolation (merge route-maps {"http://meta:4004/catalog-id/SERIE0101005?fields=season,episode_name,series,episode"
                                        (fn [result] {:status 510 :headers {}
                                            :body (generate-string {:records [{:episode 5 :series "Series 1" :season 1}]})})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "meta service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "not-found route"
      (let [response (app (mock/request :get "/invalid"))]
        (is (= (:status response) 404))))))

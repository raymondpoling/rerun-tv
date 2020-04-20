(ns format.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [format.handler :refer [app]]
            [cheshire.core :refer [generate-string
                                   parse-string]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-app
  (let [route-maps
        {"http://user:4002/test-user/test-schedule"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok
                    :idx 5})})
         "http://merge:4012/test-user/test-schedule/5"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok :playlist
                    [{:series "Series 1"
                      :locations ["file://home/Video/Series 1/series 1 D1-5.mkv"
                                  "http://archive/video/Series 1/series 1 D1-5.mkv"]
                      :episode 5
                      :season 1},
                     {:series "Series 2"
                      :locations ["file://home/Video/Series 2/series 2 D2-2.mkv"]
                      :episode 3
                      :season 1}]})})
         "http://merge:4012/test-user/test-schedule/5?host=archive&protocol=http"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok :playlist
                    [{:series "Series 1"
                      :locations "http://archive/video/Series 1/series 1 D1-5.mkv"
                      :episode 5
                      :season 1},
                     {:series "Series 2"
                      :locations "http://archive/video/Series 2/series 2 D2-2.mkv"
                      :episode 3
                      :season 1}]})})
         "http://merge:4012/test-user/test-schedule/5?host=archive&protocol=file"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok :playlist
                    [{:series "Series 1"
                      :locations "file:///video/Series 1/series 1 D1-5.mkv"
                      :episode 5
                      :season 1},
                     {:series "Series 2"
                      :locations "file:///video/Series 2/series 2 D2-2.mkv"
                      :episode 3
                      :season 1}]})})}]
    (testing "get a json record"
      (with-fake-routes-in-isolation route-maps
        (let [response (app (mock/request :get "/test-user/test-schedule?format=json"))
              expected {"status" "ok"
                        "playlist" [{"series" "Series 1"
                                     "season" 1
                                     "episode" 5
                                     "locations" ["file://home/Video/Series 1/series 1 D1-5.mkv"
                                                  "http://archive/video/Series 1/series 1 D1-5.mkv"]}
                                    {"series" "Series 2"
                                     "season" 1
                                     "episode" 3
                                     "locations" ["file://home/Video/Series 2/series 2 D2-2.mkv"]}]}]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response))
                 expected)))))
    (testing "get an json record for a specific host"
      (with-fake-routes-in-isolation route-maps
        (let [response (app (mock/request :get "/test-user/test-schedule?host=archive&protocol=http&format=json"))
              expected {"status" "ok"
                        "playlist" [{"series" "Series 1"
                                     "season" 1
                                     "episode" 5
                                     "locations" "http://archive/video/Series 1/series 1 D1-5.mkv"}
                                    {"series" "Series 2"
                                     "season" 1
                                     "episode" 3
                                     "locations" "http://archive/video/Series 2/series 2 D2-2.mkv"}]}]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response)) expected)))))
    (testing "get an m3u record"
      (with-fake-routes-in-isolation route-maps
        (let [response (app (mock/request :get "/test-user/test-schedule?format=m3u&host=archive&protocol=file"))
              expected (str "#EXTM3U\n"
                            "#PLAYLIST: test-schedule - 5\n"
                            "\n"
                            "#EXTINF:0,, Series 1 S1E5\n"
                            "file:///video/Series 1/series 1 D1-5.mkv\n"
                            "\n"
                            "#EXTINF:0,, Series 2 S1E3\n"
                            "file:///video/Series 2/series 2 D2-2.mkv\n")]
          (is (= (:status response) 200))
          (is (= (:body response) expected)))))
    (testing "get an m3u record and propogate update flag"
      (with-fake-routes-in-isolation (merge route-maps {"http://user:4002/test-user/test-schedule?update=true"
                                      (fn [result]
                                        (if (= "update=true" (:query-string result))
                                          {:status 200 :headers {}
                                            :body (generate-string {:status :ok, :idx 5})}
                                          {:status 500 :headers {}
                                            :body (generate-string {:status :ok})}))})
        (let [response (app (mock/request :get "/test-user/test-schedule?update=true&format=m3u&host=archive&protocol=file"))
              expected (str "#EXTM3U\n"
                            "#PLAYLIST: test-schedule - 5\n"
                            "\n"
                            "#EXTINF:0,, Series 1 S1E5\n"
                            "file:///video/Series 1/series 1 D1-5.mkv\n"
                            "\n"
                            "#EXTINF:0,, Series 2 S1E3\n"
                            "file:///video/Series 2/series 2 D2-2.mkv\n")]
          (is (= (:status response) 200))
          (is (= (:body response)
                 expected)))))
    (testing "propogate an error if merge service fails"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://merge:4012/test-user/test-schedule/5"
                (fn [_] {:status 500 :headers {}
                              :body nil})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "merge service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    ;
    (testing "propogate an error if user service fails"
      (with-fake-routes-in-isolation
        (merge route-maps {"http://user:4002/test-user/test-schedule"
                           (fn [_] {:status 500 :headers {}
                                    :body (generate-string
                                           {:status :ok, :idx 5})})})
        (let [response (app (mock/request :get "/test-user/test-schedule"))
              expected {"status" "failure" "message" "user service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))))

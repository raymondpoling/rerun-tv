(ns merge.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [merge.handler :refer [app]]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-app
  (let [route-maps
        {"http://schedule:4000/test-schedule/5"
         (fn [_] {:status 200 :headers {}
                  :body (generate-string
                         {:status :ok
                          :items [{:name "Series 1"
                                   :index 5},
                                  {:name "Series 2",
                                   :index 3}]})})
         "http://playlist:4001/Series%201/5"
         (fn [_] {:status 200 :headers {}
                  :body (generate-string {:status :ok
                                          :item "SERIE0101005"})})
         "http://playlist:4001/Series%202/3"
         (fn [_] {:status 200 :headers {}
                  :body (generate-string {:status :ok
                                          :item "SERIE0201005"})})
         "http://locator:4006/catalog-id/SERIE0101005"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok
                    :files
                    ["file://CrystalBall/home/Video/Series 1/series 1 D1-5.mkv"]})})
         "http://locator:4006/catalog-id/SERIE0201005"
         (fn [_]
           {:status 200 :headers {}
            :body (generate-string
                   {:status :ok
                    :files
                    ["file://CrystalBall/home/Video/Series 2/series 2 D2-2.mkv"
                     "http://archive/video/Series 2/series 2 D2-2.mkv"]})})
         "http://omdb:4011/catalog-id/SERIE0101005"
         (fn [_] {:status 200 :headers {}
                  :body (generate-string
                         {:status :ok
                          :catalog_ids ["SERIE0101005"]
                          :records [{:episode_name "five"
                                     :episode 5
                                     :series "Series 1"
                                     :season 1}]})})
         "http://omdb:4011/catalog-id/SERIE0201005"
         (fn [_] {:status 200 :headers {}
                  :body (generate-string
                         {:status :ok
                          :catalog_ids ["SERIE0201005"]
                          :records [{:episode_name "three"
                                     :episode 3
                                     :series "Series 2"
                                     :season 1}]})})}]
    (testing "propogate an error if schedule service fails"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://schedule:4000/test-schedule/5"
                (fn [_] {:status 500 :headers {}
                         :body nil})})
        (let [response (app (mock/request :get "/test-user/test-schedule/5"))
              expected {"status" "failure"
                        "message" "schedule service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
                                        ;
                                        ;
    (testing "propogate an error if playlist service fails"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://playlist:4001/Series%201/5"
                (fn [_] {:status 500 :headers {}
                         :body ""})})
        (let [response (app (mock/request :get "/test-user/test-schedule/5"))
              expected {"status" "failure"
                        "message" "playlist service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
                                        ;
    (testing "propogate an error if locator service fails"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://locator:4006/catalog-id/SERIE0101005"
                (fn [_]
                  {:status 400
                   :headers {}
                   :body (generate-string
                          {:status :ok
                           :url
                           "file://home/Video/Series 1/series 1 D1-5.mkv"})})})
        (let [response (app (mock/request :get "/test-user/test-schedule/5"))
              expected {"status" "failure"
                        "message" "locator service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
                                        ;
    (testing "propogate an error if meta service fails"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://omdb:4011/catalog-id/SERIE0101005"
                (fn [_] {:status 510 :headers {}
                         :body (generate-string
                                {:status :ok
                                 :catalog_ids ["SERIE0101005"]
                                 :records [{:episode 5
                                            :series "Series 1"
                                            :season 1}]})})})
        (let [response (app (mock/request :get "/test-user/test-schedule/5"))
              expected {"status" "failure"
                        "message" "meta service not available"}]
          (is (= (:status response) 502))
          (is (= (parse-string (:body response)) expected)))))
    (testing "get one schedule as a json record"
      (with-fake-routes-in-isolation route-maps
        (let [response (app (mock/request :get "/test-user/test-schedule/5"))
              expected
              {"status" "ok"
               "playlist"
               [{"playlist" {"name" "Series 1"
                            "index" 5},
                "episode_name" "five"
                 "episode" 5
                 "series" "Series 1"
                 "season" 1
                 "locations"
                 ["file://CrystalBall/home/Video/Series 1/series 1 D1-5.mkv"]},
                {"playlist" {"name" "Series 2",
                             "index" 3}
                 "episode_name" "three"
                 "episode" 3
                 "series" "Series 2"
                 "season" 1
                 "locations"
                 ["file://CrystalBall/home/Video/Series 2/series 2 D2-2.mkv"
                  "http://archive/video/Series 2/series 2 D2-2.mkv"]}]}]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response))
                 expected)))))
    (testing "get one schedule as a json record with only host/protocol selected file"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://locator:4006/file/CrystalBall/SERIE0101005"
                (fn [_]
                  {:status 200
                   :headers {}
                   :body (generate-string
                          {:status :ok
                           :url
                           "file:///home/Video/Series 1/series 1 D1-5.mkv"})})
                "http://locator:4006/file/CrystalBall/SERIE0201005"
                (fn [_]
                  {:status 200
                   :headers {}
                   :body (generate-string
                          {:status :ok
                           :url
                           "file:///home/Video/Series 2/series 2 D2-2.mkv"})})})
        (let [response
              (app
               (mock/request
                :get
                "/test-user/test-schedule/5?host=CrystalBall&protocol=file"))
              expected {"status" "ok"
                        "playlist"
                        [{"playlist" {"name" "Series 1"
                                      "index" 5},
                          "episode_name" "five"
                          "episode" 5
                          "series" "Series 1"
                          "season" 1
                          "locations"
                          "file:///home/Video/Series 1/series 1 D1-5.mkv"},
                         {"playlist"  {"name" "Series 2",
                                       "index" 3}
                          "episode_name" "three"
                          "episode" 3
                          "series" "Series 2"
                          "season" 1
                          "locations"
                          "file:///home/Video/Series 2/series 2 D2-2.mkv"}]}]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response))
                 expected)))))
    (testing "get one schedule as a json record with only host/protocol selected http"
      (with-fake-routes-in-isolation
        (merge route-maps
               {"http://locator:4006/http/archive/SERIE0101005"
                (fn [_]
                  {:status 200
                   :headers {}
                   :body (generate-string
                          {:status :ok
                           :url
                           "http://archive/video/Series 1/series 1 D1-5.mkv"})})
                "http://locator:4006/http/archive/SERIE0201005"
                (fn [_]
                  {:status 200
                   :headers {}
                   :body (generate-string
                          {:status :ok
                           :url
                           "http://archive/video/Series 2/series 2 D2-2.mkv"})})})
        (let [response
              (app
               (mock/request
                :get
                "/test-user/test-schedule/5?host=archive&protocol=http"))
              expected {"status" "ok"
                        "playlist"
                        [{"playlist" {"name" "Series 1"
                                      "index" 5},
                          "episode_name" "five"
                          "episode" 5
                          "series" "Series 1"
                          "season" 1
                          "locations"
                          "http://archive/video/Series 1/series 1 D1-5.mkv"},
                         {"playlist"  {"name" "Series 2",
                                       "index" 3}
                          "episode_name" "three"
                          "episode" 3
                          "series" "Series 2"
                          "season" 1
                          "locations"
                          "http://archive/video/Series 2/series 2 D2-2.mkv"}]}]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response))
                 expected)))))
    (testing "provide list of protocol/hosts"
      (with-fake-routes-in-isolation
        {"http://locator:4006/protocol-host"
         (fn [_] {:status 200
                  :headers {:content-type "application/json"}
                  :body (generate-string
                         {:status :ok
                          :protocol-host ["file/CrystalBall"
                                          "http/archive"
                                          "file/DeGuirre"]})})}
        (let [response (app (mock/request :get "/protocol-host"))]
          (is (= (:status response) 200))
          (is (= (parse-string (:body response))
                  {"status" "ok"
                   "protocol-host" ["file/CrystalBall"
                                   "http/archive"
                                   "file/DeGuirre"]})))))

                                        ;
    (testing "not-found route"
      (let [response (app (mock/request :get "/invalid"))]
        (is (= (:status response) 404))))))

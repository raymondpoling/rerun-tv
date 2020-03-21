(ns schedule-builder.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [schedule-builder.handler :refer :all]
            [cheshire.core :refer :all])
  (:use clj-http.fake))

(deftest playlist-tests
  (testing "get all playlists"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                  (fn [request] {:status 200 :headers {}
                                    :body (generate-string {:status :ok
                                      :playlists [{"name" "series_a" "length" 12},
                                                {"name" "series_b" "length" 23}]})})}
    (let [response (app (mock/request :get "/playlists"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"
               "playlists" [{"name" "series_a" "length" 12},
                            {"name" "series_b" "length" 23}]})))))
  (testing "failed to get all playlists"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                    (fn [request] {:status 500 :headers {}})}
    (let [response (app (mock/request :get "/playlists"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure"
                "message" "playlist service not available"})))))
  (testing "found playlist"
    (with-fake-routes-in-isolation {"http://playlist:4001/series_test1"
                                    (fn [request] {:status 200 :headers {}
                                      :body (generate-string {:status :ok :items ["a","b","c"]})})}
    (let [response (app (mock/request :get "/playlists/series_test1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"
                "items" ["a","b","c"]})))))
  (testing "playlist not found"
    (with-fake-routes-in-isolation {"http://playlist:4001/series_test1"
                                    (fn [request] {:status 404 :headers {}
                                      :body (generate-string {:status :not-found})})}
    (let [response (app (mock/request :get "/playlists/series_test1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "not-found"})))))
  (testing "playlist not available"
    (with-fake-routes-in-isolation {"http://playlist:4001/series_test1"
                                    (fn [request] {:status 500 :headers {}
                                      })}
    (let [response (app (mock/request :get "/playlists/series_test1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure"
              "message" "playlist service not available"}))))))

(deftest post-tests
  (testing "post new schedule"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"})))))
  (testing "posted schedule must have playlists"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: must provide a playlists"
                                              "Invalid Schedule: playlists must be an array"]})))))
  (testing "posted schedule must have correct name"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule2" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: name must match"]})))))
  (testing "post new schedule must put a schedule"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :post "/schedule/store/schedule1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: name must match"
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "post new schedule but invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test2", :length 27}]})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 12}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Failed Validation: series_test1"]})))))
  (testing "post new schedule but no playlist service"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 500 :headers ()
                                                    :body (generate-string {:status :invalid})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 12}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure" "message" "playlist service not available"})))))
  (testing "can't overwrite with post"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:post (fn [req] {:status 400 :headers {} :body (generate-string {:status :invalid})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :post "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure" "messages" ["Cannot Create Schedule: Already Exists"]}))))))

(deftest put-tests
  (testing "put new schedule"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"})))))
  (testing "puted schedule must have playlists"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "puted schedule must have correct name"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule2" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: name must match"]})))))
  (testing "put new schedule must put a schedule"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :put "/schedule/store/schedule1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: name must match"
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "put new schedule but invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test2", :length 27}]})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 12}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Failed Validation: series_test1"]})))))
  (testing "put new schedule but no playlist service"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 200 :headers {} :body (generate-string {:status :ok})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 500 :headers ()
                                                    :body (generate-string {:status :failure})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 12}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure" "message" "playlist service not available"})))))
  (testing "can't overwrite with put"
    (with-fake-routes-in-isolation {"http://schedule:4000/schedule1"
                                    {:put (fn [req] {:status 400 :headers {} :body (generate-string {:status :invalid})})}
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :put "/schedule/store/schedule1")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "failure" "messages" ["Schedule Does Not Exist: cannot update schedule"]}))))))

(deftest get-sent-validations
  (testing "validate a valid schedule"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :get "/schedule/validate")
                            (mock/json-body {:name "schedule1" :playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"})))))

  (testing "when schedule missing, invalid"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"
                "Invalid Schedule: name must be defined"]})))))
  (testing "when playlists missing, invalid"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :get "/schedule/validate")
                            (mock/json-body {:name "schedule1"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "when name missing, invalid"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :get "/schedule/validate")
                            (mock/json-body {:playlists [{:type "playlist" :name "series_test1" :length 27}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: name must be defined"]})))))
  (testing "when empty, doesn't validate"
    (with-fake-routes-in-isolation {"http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (-> (mock/request :get "/schedule/validate")
                            (mock/json-body {})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"
                "Invalid Schedule: name must be defined"]}))))))

(deftest get-schedule-validations
  (testing "validate a valid schedule"
    (with-fake-routes-in-isolation {"http://schedule:4000/test1"
                                      (fn [request] {:status 200 :headers ()
                                                      :body (generate-string {:status :ok :schedule {:name "test1" :playlists [{:type "playlist" :length 27 :name "series_test1"}]}})})
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate/test1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok"})))))

  (testing "when schedule missing, invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/test2"
                                      (fn [request] {:status 404 :headers ()
                                                      :body (generate-string {})})
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate/test2"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" [
                "Invalid Schedule: name must match"
                "Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "when playlists missing, invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/test3"
                                      (fn [request] {:status 200 :headers ()
                                                      :body (generate-string {:status :ok :schedule {:name "test3" }})})
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate/test3"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: must provide a playlists"
                "Invalid Schedule: playlists must be an array"]})))))
  (testing "when name missing, invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/test4"
                                      (fn [request] {:status 200 :headers ()
                                                      :body (generate-string {:status :ok :schedule { :playlists [{:type "playlist" :length 27 :name "series_test1"}]}})})
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate/test4"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: name must match"]})))))
  (testing "when name wrong, invalid"
    (with-fake-routes-in-isolation {"http://schedule:4000/test4"
                                      (fn [request] {:status 200 :headers ()
                                                      :body (generate-string {:status :ok :schedule {:name "test5" :playlists [{:type "playlist" :length 27 :name "series_test1"}]}})})
                                    "http://playlist:4001/"
                                      (fn [request] {:status 200 :headers ()
                                                    :body (generate-string {:status :ok :playlists [{:name "series_test1", :length 27}]})})}
    (let [response (app (mock/request :get "/schedule/validate/test4"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "invalid" "messages" ["Invalid Schedule: name must match"]}))))))

(deftest not-found
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

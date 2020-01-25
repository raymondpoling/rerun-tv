(ns playlist-schedule.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [playlist-schedule.handler :refer :all]))

(deftest test-app
  (testing "reject a bad schedule"
    (let [response (app (-> (mock/request :post "/test-bad")
                            (mock/json-body {"name" "test-bad"
                                        "playlists" [{"type" "playlist" "name" "cats"}]})))]
      (is (= (:status response) 412))
      (is (= (:body response) "{\"message\":{\"type\":\"missing-length\",\"cause\":\"validity\"}}"))))
  (testing "adding new schedule"
    (let [response (app (-> (mock/request :post "/test-found")
                            (mock/json-body {:name "test-found"
                                        :playlists [{:type "playlist" :name "cats" :length 12},
                                                    {:type "playlist" :name "dogs" :length 13}]})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "finding schedule"
    (let [response (app (mock/request :get "/test-found"))]
      (is (= (:status response) 200))
      (is (= (:body response) (str "{\"name\":\"test-found\",\"playlists\""
                                    ":[{\"name\":\"cats\",\"type\":\"playlist\",\"length\":12}"
                                    ",{\"name\":\"dogs\",\"type\":\"playlist\",\"length\":13}]}")))))
  (testing "getting a frame"
    (let [response (app (mock/request :get "/test-found/33"))]
      (is (= (:status response) 200))
      (is (= (:body response) "[{\"name\":\"cats\",\"index\":9},{\"name\":\"dogs\",\"index\":7}]"))))
  (testing "updating a schedule"
    (let [response (app (-> (mock/request :put "/test-found")
                            (mock/json-body {:name "test-found"
                                        :playlists [{:type "playlist" :name "cats" :length 15},
                                                    {:type "playlist" :name "dogs" :length 17}]})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "getting a frame"
    (let [response (app (mock/request :get "/test-found/33"))]
      (is (= (:status response) 200))
      (is (= (:body response) "[{\"name\":\"cats\",\"index\":3},{\"name\":\"dogs\",\"index\":16}]"))))
  (testing "deleting playlist"
    (let [response (app (mock/request :delete "/test-found"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "getting not-found route"
    (let [response (app (mock/request :get "/test-found"))]
      (is (= (:status response) 404)))))

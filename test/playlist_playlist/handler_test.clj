(ns playlist-playlist.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [playlist-playlist.handler :refer :all]))

(deftest test-app
  (testing "create new playlist"
    (let [response (app (-> (mock/request :post "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (map str (map char (range 97 123)))})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "find element in playlist"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
      (is (= (:body response) "m"))))
  (testing "reject overwriting playlist"
    (let [response (app (-> (mock/request :post "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (reverse (map str (map char (range 97 123))))})))]
      (is (= (:status response) 412))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "find element in playlist to ensure not overwritten"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
        (is (= (:body response) "m"))))
  (testing "replace playlist"
    (let [response (app (-> (mock/request :put "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (reverse (map str (map char (range 97 123))))})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "find element in updated playlist"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
      (is (= (:body response) "n"))))
  (testing "delete a playlist"
    (let [response (app (mock/request :delete "/new-playlist"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\"}"))))
  (testing "not-found route"
    (let [response (app (mock/request :get "/new-playlist"))]
      (is (= (:status response) 404)))))

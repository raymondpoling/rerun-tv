(ns playlist-playlist.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [playlist-playlist.handler :refer :all]
            [cheshire.core :refer :all]
            [db.db :refer [initialize]]))

(deftest test-app
  (initialize)
  (testing "create new playlist"
    (let [response (app (-> (mock/request :post "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (map str (map char (range 97 123)))})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "find element in playlist"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok" "playlist" "m"}))))
  (testing "reject overwriting playlist"
    (let [response (app (-> (mock/request :post "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (reverse (map str (map char (range 97 123))))})))]
      (is (= (:status response) 412))
      (is (= (parse-string (:body response)) {"status" "invalid"}))))
  (testing "add two additional playlists"
    (let [response1 (app (-> (mock/request :post "/another-new-playlist")
                            (mock/json-body {:name "another-new-playlist"
                            :playlist (map str (map char (range 97 105)))})))
          response2 (app (-> (mock/request :post "/new-playlist-also")
                              (mock/json-body {:name "new-playlist-also"
                              :playlist (map str (map char (range 97 114)))})))]
      (is (= (:status response1) 200))
      (is (= (parse-string (:body response1)) {"status" "ok"}))
      (is (= (:status response2) 200))
      (is (= (parse-string (:body response2)) {"status" "ok"}))))
  (testing "find element in playlist to ensure not overwritten"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok" "playlist" "m"}))))
  (testing "replace playlist"
    (let [response (app (-> (mock/request :put "/new-playlist")
                            (mock/json-body {:name "new-playlist"
                            :playlist (reverse (map str (map char (range 97 123))))})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "find element in updated playlist"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok" "playlist" "n"}))))
  (testing "get a list of playlists"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (count (:playlists (parse-string (:body response) true))) 3))))
  (testing "get specific playlists"
    (let [response1 (app (mock/request :get "/new-playlist"))
          response2 (app (mock/request :get "/another-new-playlist"))
          response3 (app (mock/request :get "/new-playlist-also"))
          response4 (app (mock/request :get "/not-found"))]
      (is (= (:status response1) 200))
      (is (= (parse-string (:body response1) true)
              {:status "ok" :playlist {:name "new-playlist" :length 26}}))

      (is (= (:status response2) 200))
      (is (= (parse-string (:body response2) true)
              {:status "ok" :playlist {:name "another-new-playlist" :length 8}}))

      (is (= (:status response3) 200))
      (is (= (parse-string (:body response3) true)
              {:status "ok" :playlist {:name "new-playlist-also" :length 17}}))

      (is (= (:status response4) 404))
      (is (= (parse-string (:body response4) true)
              {:status "not-found"}))))
  (testing "delete a playlist"
    (let [response (app (mock/request :delete "/new-playlist"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "not-found route"
    (let [response (app (mock/request :get "/new-playlist/12"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"})))))

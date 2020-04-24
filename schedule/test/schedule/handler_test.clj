(ns schedule.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [schedule.handler :refer [app]]
            [cheshire.core :refer [parse-string]]
            [schedule.test-db :refer [create-h2-mem-tables]]
            [db.db :refer [initialize]]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "reject a bad schedule"
    (let [response (app (-> (mock/request :post "/test-bad")
                            (mock/json-body {"name" "test-bad"
                                        "playlists" [{"type" "playlist" "name" "cats"}]})))]
      (is (= (:status response) 412))
      (is (= (parse-string (:body response)) {"status" "failed" "message" {"type" "missing-length","cause" "validity"}}))))
  (testing "adding new schedule"
    (let [response (app (-> (mock/request :post "/test-found")
                            (mock/json-body {:name "test-found"
                                        :playlists [{:type "playlist" :name "cats" :length 12},
                                                    {:type "playlist" :name "dogs" :length 13}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "finding schedule"
    (let [response (app (mock/request :get "/test-found"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "schedule" {"name" "test-found",
                              "playlists"
                                    [{"name" "cats", "length" 12, "type" "playlist"}
                                    ,{"name" "dogs", "length" 13, "type" "playlist"}]}}))))
  (testing "not find schedule"
    (let [response (app (mock/request :get "/test-found2"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"}))))
  (testing "getting a frame"
    (let [response (app (mock/request :get "/test-found/33"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok" "items" [{"name" "cats", "index" 9},{"name" "dogs","index" 7}]}))))
  (testing "updating a schedule"
    (let [response (app (-> (mock/request :put "/test-found")
                            (mock/json-body {:name "test-found"
                                        :playlists [{:type "playlist" :name "cats" :length 15},
                                                    {:type "playlist" :name "dogs" :length 17}]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "getting a frame"
    (let [response (app (mock/request :get "/test-found/33"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok" "items" [{"name" "cats","index" 3},{"name" "dogs","index" 16}]}))))
  (testing "get all schedule names"
    (let [_ (app (-> (mock/request :post "/test-found2")
                     (mock/json-body {:name "test-found2"
                                      :playlists [{:type "playlist" :name "cats" :length 12},
                                                  {:type "playlist" :name "dogs" :length 13}]})))
          response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
              {"status" "ok" "schedules" ["test-found" "test-found2"]}))))
  (testing "deleting playlist"
    (let [response (app (mock/request :delete "/test-found"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "getting not-found route"
    (let [response (app (mock/request :get "/test-found"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"})))))

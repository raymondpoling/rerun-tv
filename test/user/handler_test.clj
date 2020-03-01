(ns user.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [cheshire.core :refer :all]
            [db.db :refer [initialize]]
            [user.handler :refer :all]))

(deftest test-app
  (initialize)
  (testing "user not found"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"}))))
  (testing "add user"
    (let [response (app (mock/request :post "/test_user"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "add user 2"
    (let [response (app (mock/request :post "/test_user_2"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "find first schedule 0 for user 2"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","idx" 0}))))
  (testing "find first schedule 1"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "idx" 1}))))
  (testing "find second schedule 0"
    (let [response (app (mock/request :get "/test_user/test_schedule_0"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","idx" 0}))))
  (testing "find first schedule 0 for user 2"
    (let [response (app (mock/request :get "/test_user_2/test_schedule"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","idx" 0}))))
  (testing "find first schedule 2"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","idx" 2}))))
  (testing "set first schedule 1"
    (let [response (app (mock/request :put "/test_user/test_schedule/1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "find first schedule 1"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","idx" 1}))))
  (testing "find first schedule 1 preview twice"
    (let [_         (app (mock/request :put "/test_user/test_schedule/1"))
          response1 (app (mock/request :get "/test_user/test_schedule?preview=true"))
          response2 (app (mock/request :get "/test_user/test_schedule?preview=true"))]
      (is (= (:status response1) 200))
      (is (= (parse-string (:body response1)) {"status" "ok","idx" 1}))
      (is (= (:status response2) 200))
      (is (= (parse-string (:body response2)) {"status" "ok","idx" 1}))))
  (testing "delete test user"
    (let [response (app (mock/request :delete "/test_user"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "delete test user 2"
    (let [response (app (mock/request :delete "/test_user_2"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))
  (testing "deleted user not found"
    (let [response (app (mock/request :get "/test_user/test_schedule"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"}))))

  (testing "deleted user 2 not found"
    (let [response (app (mock/request :get "/test_user_2/test_schedule"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response)) {"status" "not-found"})))))

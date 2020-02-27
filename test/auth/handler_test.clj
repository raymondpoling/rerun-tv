(ns auth.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [db.db :refer [initialize]]
            [auth.handler :refer :all]
            [cheshire.core :refer :all]))

(deftest test-app
  (initialize)
  (testing "create a user"
    (let [response (app (-> (mock/request :post "/new/test-user")
                            (mock/json-body {:password "password"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "do not recreate user"
    (let [response (app (-> (mock/request :post "/new/test-user")
                            (mock/json-body {:password "not a password"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "could-not-create"}))))

  (testing "validate a user"
    (let [response (app (-> (mock/request :post "/validate/test-user")
                            (mock/json-body {:password "password"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "do not validate a user with bad password"
    (let [response (app (-> (mock/request :post "/validate/test-user")
                            (mock/json-body {:password "bad password"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "invalid-credentials"}))))

  (testing "fail a non-valid user"
    (let [response (app (-> (mock/request :post "/validate/test-user2")
                            (mock/json-body {:password "password"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "invalid-credentials"}))))

  (testing "change password"
    (let [response (app (-> (mock/request :post "/update/test-user")
                            (mock/json-body {:old-password "password"
                                        :new-password "changed"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "validate a user"
    (let [response (app (-> (mock/request :post "/validate/test-user")
                            (mock/json-body {:password "changed"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 404)))))

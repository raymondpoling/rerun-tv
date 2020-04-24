(ns auth.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [db.db :refer [initialize database]]
            [clojure.java.jdbc :as j]
            [auth.test-db :refer [create-h2-mem-tables]]
            [auth.handler :refer [app]]
            [cheshire.core :refer [parse-string]]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
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

  (testing "validate a user" ; do not like, but necessary to prove 256 being used. Ideally, we are salting a hash though.
    (let [pass (:password (first (j/query @database ["SELECT * FROM auth.authorize WHERE auth.authorize.user = 'test-user'"])))
          response (app (-> (mock/request :post "/validate/test-user")
                            (mock/json-body {:password "changed"})))]
      (is (some? pass))
      (is (seq pass))
      (is (= (count pass) 64))
      (is (not (= pass "changed")))
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 404)))))

(ns identity.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [db.db :refer [initialize]]
            [identity.test-db :refer [create-h2-mem-tables]]
            [identity.handler :refer [app]]
            [cheshire.core :refer [parse-string]]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "create a user"
    (let [response (app (-> (mock/request :post "/user/test-user")
                            (mock/json-body {:role "user"
                                              :email "me@nail.com"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "create a user with default role"
    (let [response (app (-> (mock/request :post "/user/test-user3")
                            (mock/json-body {:email "me3@nail.com"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "make sure user is the default role"
    (let [response (app (-> (mock/request :get "/user/test-user3")))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"
                                              "role" "user"
                                              "user" "test-user3"
                                              "email" "me3@nail.com"}))))

  (testing "email required for creating a user"
    (let [response (app (-> (mock/request :post "/user/test-user4")
                            (mock/json-body {})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "failed" "message" "user must have an email address"}))))

  (testing "do not recreate user"
    (let [response (app (-> (mock/request :post "/user/test-user")
                            (mock/json-body {:role "user"
                                              :email "me@nail.com"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "failed" "message" "user already exists"}))))

  (testing "change user role"
    (let [response (app (-> (mock/request :put "/user/test-user")
                            (mock/json-body {:role "media"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "reject non-existant roles on create"
    (let [response (app (-> (mock/request :post "/user/test-user2")
                            (mock/json-body {:role "media-czar"
                                              :email "me@nail.com"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "failed" "message" "role [media-czar] does not exist"}))))

  (testing "reject non-existant roles on update"
    (let [response (app (-> (mock/request :put "/user/test-user")
                            (mock/json-body {:role "media-czar"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "failed" "message" "role [media-czar] does not exist"}))))

  (testing "create new role"
    (let [response (app (mock/request :post "/role/media-czar"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "fail to recreate role"
    (let [response (app (mock/request :post "/role/media-czar"))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response)) {"status" "failed" "message" "cannot create role"}))))

  (testing "create user with new role"
    (let [response (app (-> (mock/request :post "/user/test-user4")
                            (mock/json-body {:role "media-czar"
                                              :email "me4@nail.com"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "update user with new role"
    (let [response (app (-> (mock/request :put "/user/test-user3")
                            (mock/json-body {:role "media-czar"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "get all roles"
    (let [response (app (mock/request :get "/role"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok" "roles" ["admin","media","media-czar","user"]}))))

  (testing "get role for user"
    (let [response1 (app (-> (mock/request :get "/user/test-user")))
          response2 (app (-> (mock/request :get "/user/test-user2")))
          response3 (app (-> (mock/request :get "/user/test-user3")))
          response4 (app (-> (mock/request :get "/user/test-user4")))]
    (is (= (:status response1) 200))
    (is (= (parse-string (:body response1)) {"status" "ok"
                                              "user" "test-user"
                                              "email" "me@nail.com"
                                              "role" "media"}))
    (is (= (:status response2) 404))
    (is (= (parse-string (:body response2)) {"status" "not-found"}))
    (is (= (:status response3) 200))
    (is (= (parse-string (:body response3)) {"status" "ok"
                                              "user" "test-user3"
                                              "email" "me3@nail.com"
                                              "role" "media-czar"}))
    (is (= (:status response4) 200))
    (is (= (parse-string (:body response4)) {"status" "ok"
                                              "user" "test-user4"
                                              "email" "me4@nail.com"
                                              "role" "media-czar"}))))

  (testing "get all users"
    (let [response (app (mock/request :get "/user"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"
                                              "users"[{"user" "test-user"
                                                "email" "me@nail.com"
                                                "role" "media"}
                                                {"user" "test-user3"
                                                "email" "me3@nail.com"
                                                "role" "media-czar"}
                                                {"user" "test-user4"
                                                "email" "me4@nail.com"
                                                "role" "media-czar"}]}))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 404)))))

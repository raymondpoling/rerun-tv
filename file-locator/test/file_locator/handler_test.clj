(ns file-locator.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [cheshire.core :refer [parse-string]]
            [db.db :refer [initialize]]
            [file-locator.test-db :refer [create-h2-mem-tables]]
            [file-locator.handler :refer [app]]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "post a new url"
    (let [response (app (-> (mock/request :post "/file/host1/TESTM0101001")
                            (mock/json-body {:path "/home/myself/Videos/test-me/Season 1/1-1.mkv"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "get the url back"
    (let [response (app (mock/request :get "/file/host1/TESTM0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "url" "file:///home/myself/Videos/test-me/Season 1/1-1.mkv"}))))

  (testing "post a different related url"
    (let [response (app (-> (mock/request :post "/ssh/host2/TESTM0101001")
                            (mock/json-body {:path "/home/myself/Videos/test-me/Season 1/1-1.mkv"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "get the url back"
    (let [response (app (mock/request :get "/ssh/host2/TESTM0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "url" "ssh://host2/home/myself/Videos/test-me/Season 1/1-1.mkv"}))))

  (testing "get all urls for a resource"
    (let [response (app (mock/request :get "/catalog-id/TESTM0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok",
              "files" ["file://host1/home/myself/Videos/test-me/Season 1/1-1.mkv"
                       "ssh://host2/home/myself/Videos/test-me/Season 1/1-1.mkv"]}))))

  (testing "save a set of urls for a catalog-id both new and updated"
    (let [response (app (-> (mock/request :put "/catalog-id/TESTM0101001")
                            (mock/json-body
                             {:files
                              ["file://host1/home/myself/Videos/test-me/Season 1/1-1.mkv"
                               "ssh://host2/home/myself/Videos/test-me/Season 1/1-1.avi"
                               "ssh://host3/home/other/other.mkv"]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok"}))))

(testing "get all urls for a resource after update by catalog-id"
    (let [response (app (mock/request :get "/catalog-id/TESTM0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok",
              "files" ["file://host1/home/myself/Videos/test-me/Season 1/1-1.mkv"
                       "ssh://host2/home/myself/Videos/test-me/Season 1/1-1.avi"
                       "ssh://host3/home/other/other.mkv"]}))))
  (testing "get available protocol/hosts"
    (let [response (app (mock/request :get "/all"))]
      (is (= (:status response) 200))
      (is (parse-string (:body response))
          {"status" "ok",
           "host-protocol" ["file/host1"
                            "ssh/host2"
                            "ssh/host3"]})))
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

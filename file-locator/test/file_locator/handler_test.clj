(ns file-locator.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [cheshire.core :refer :all]
            [db.db :refer [initialize]]
            [file-locator.handler :refer :all]))

(deftest test-app
  (initialize)
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

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

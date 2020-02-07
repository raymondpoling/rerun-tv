(ns file-meta.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [file-meta.handler :refer :all]
            [db.db :refer [initialize]]
            [cheshire.core :refer :all]))

(deftest test-app
  (initialize)
  (testing "create a stub record"
    (let [response (app (mock/request :post "/series/test-series/1/1"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101001\"]}"))))
  (testing "create a stub record"
    (let [response (app (mock/request :post "/series/test-series/1/2"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101002\"]}"))))
  (testing "create a stub record"
    (let [response (app (mock/request :post "/series/test-series/1/3"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101003\"]}"))))
  (testing "update the stub record"
    (let [response (app (-> (mock/request :put "/series/test-series/1/1")
                            (mock/json-body {"episode_name" "The Cat Returns" "summary" (str "Wonderful "
                            "story of cats being cats and everyone loving them. Hooray!")})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101001\"]}"))))
  (testing "bulk update many records"
    (let [response (app (-> (mock/request :put "/series/test-series")
                            (mock/json-body [{"episode_name" "The Cat Returns 2" "summary" (str "Wonderful "
                            "story of cats being cats and everyone loving them. Hooray! For the second time")
                             "episode" 2 "season" 1}
                            {"episode_name" "The Cat Returns 3" "summary" (str "Wonderful "
                            "story of cats being cats and everyone loving them. Hooray! Got tired of this") "episode" 3 "season" 1}])))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101002\",\"TESTS0101003\"]}"))))
  (testing "find a single record"
    (let [response (app (mock/request :get "/series/test-series/1/1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"],"records"
                              [{"episode_name" "The Cat Returns", "summary" (str "Wonderful "
      "story of cats being cats and everyone loving them. Hooray!") "season" 1 "episode" 1 "series" "test-series"}]}))))
  (testing "find specific fields of a second record"
    (let [response (app (mock/request :get "/series/test-series/1/2?fields=summary"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids"["TESTS0101002"],"records"
      [{"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time"}]}))))
  (testing "verify catalog ids are created correctly for subsequent records"
    (let [response (app (mock/request :post "/series/test-serials/1/1"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0201001\"]}"))))
  (testing "delete a record"
    (let [response (app (mock/request :delete "/series/test-series/1/2"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101002"]}))))
  (testing "lookup by catalog id"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"], "records" [
      {"episode_name" "The Cat Returns","summary" (str "Wonderful "
      "story of cats being cats and everyone loving them. Hooray!"),"episode" 1, "season" 1, "series" "test-series"}]}))))
  (testing "find specific fields of a second record by catalog_id"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101003?fields=summary"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids"["TESTS0101003"],"records"
      [{"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"}]}))))
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid/1/1"))]
      (is (= (:status response) 404))))
  (testing "not-found after delete"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101002"))]
      (is (= (:status response) 404)))))

(ns file-meta.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [file-meta.handler :refer :all]
            [db.db :refer [initialize]]
            [file-meta.test-db :refer [create-h2-mem-tables]]
            [cheshire.core :refer :all]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "create a stub record"
    (let [response (app (mock/request :post "/series/test-series/1/1"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101001\"]}"))))
  (testing "find a single record"
    (let [response (app (mock/request :get "/series/test-series/1/1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"],"records"
                              [{"episode_name" nil,
                                "summary" nil
                                "season" 1
                                "episode" 1
                                "series" "test-series"
                                "imdbid" nil
                                "thumbnail" nil
                                }]}))))
  (testing "find a single record's catalog id only"
    (let [response (app (mock/request :get "/series/test-series/1/1?catalog_id_only=true"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"]}))))
  (testing "create a stub record"
    (let [response (app (mock/request :post "/series/test-series/1/2"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101002\"]}"))))
  (testing "create another stub record"
    (let [response (app (mock/request :post "/series/test-series/1/3"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101003\"]}"))))
  (testing "create a full record"
    (let [response (app (-> (mock/request :post "/series/test-series/1/4")
                            (mock/json-body {
                              "episode_name" "The Cat Returns 4"
                              "summary" "Definitely jumped the shark by now"
                              "imdbid" "tt6533"
                              "thumbnail" "http://here.com/jump.jpg"
                              })))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok" "catalog_ids" ["TESTS0101004"]}))))
  (testing "find all records catalog_ids"
    (let [response (app (mock/request :get "/series/test-series?catalog_id_only=true"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok",
        "catalog_ids" ["TESTS0101001","TESTS0101002","TESTS0101003","TESTS0101004"]}))))
  (testing "update the stub record"
    (let [response (app (-> (mock/request :put "/series/test-series/1/1")
                            (mock/json-body {
                              "episode_name" "The Cat Returns"
                              "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!"
                              "imdbid" "tt6565"
                              "thumbnail" "http://here.com/img.jpg"
                              })))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101001\"]}"))))
  (testing "can't update nonexistant stub record"
    (let [response (app (-> (mock/request :put "/series/does-not-exist-series/1/1")
                            (mock/json-body {
                                             "episode_name" "The Cat Returns"
                                             "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!"
                                             "imdbid" "tt6565"
                                             "thumbnail" "http://here.com/img.jpg"
                                             })))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response))
             {"status" "failure",
              "message" "Series does-not-exist-series does not exist"}))))
  (testing "bulk update many records"
    (let [response (app (-> (mock/request :put "/series/test-series")
                            (mock/json-body {"series" {"imdbid" "tt222222"
                                "thumbnail" "http://test-series.jpg"
                                "summary" "a test series i enjoy"}
                            "records" [{
                              "episode_name" "The Cat Returns 2"
                              "summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time"
                             "episode" 2
                             "season" 1
                             "imdbid" "tt6465"
                             "thumbnail" "http://mew"
                           }
                            {"episode_name" "The Cat Returns 3"
                            "summary" "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"
                            "episode" 3
                            "season" 1
                            "imdbid" "tt6466"
                            "thumbnail" "http://mew2"
                            }]})))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0101002\",\"TESTS0101003\"]}"))))
  (testing "bulk update nonexistent records"
    (let [response (app (-> (mock/request :put "/series/test-series")
                            (mock/json-body {"series" {"imdbid" "tt222222"
                                "thumbnail" "http://test-series.jpg"
                                "summary" "a test series i enjoy"}
                            "records" [{
                              "episode_name" "The Cat Returns 2"
                              "summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time"
                             "episode" 25
                             "season" 17
                             "imdbid" "tt6465"
                             "thumbnail" "http://mew"
                           }]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok","catalog_ids"[],"failures"["TESTS0117025"]}))))
  (testing "can't bulk-update a series that doesn't exist"
    (let [response (app (-> (mock/request :put "/series/not-a-test-series")
                           (mock/json-body
                            {"series" {
                                       "imdbid" "tt333"
                                       "thumbnail" "http://otherseries"
                                       "summary" "i should fail"}
                             "records" [{
                                         "episode_name" "Failures abound!"
                                         "summary" "I don't work"
                                         "episode" 44
                                         "season" 21
                                         "imdbid" "tt4444"
                                         "thumbnail" "http://otherseries"
                                         }]})))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response))
             {"status" "failure",
              "message" "Series not-a-test-series does not exist"}))))
  (testing "don't find non-existent record"
    (let [response (app (mock/request :get "/series/test-series/17/25"))]
      (is (= (:status response) 404))
      (is (= (parse-string (:body response))
             {"status" "not_found"}))))
  (testing "find a single full record"
    (let [response (app (mock/request :get "/series/test-series/1/1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"],"records"
                              [{"episode_name" "The Cat Returns",
                              "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!"
                              "season" 1
                              "episode" 1
                              "series" "test-series",
                              "imdbid" "tt6565",
                              "thumbnail" "http://here.com/img.jpg"
                              }]}))))
  (testing "find specific fields of a second record"
    (let [response (app (mock/request :get "/series/test-series/1/2?fields=summary"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids"["TESTS0101002"],"records"
        [{"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time"}]}))))
  (testing "verify catalog ids are created correctly for subsequent records"
    (let [response (app (mock/request :post "/series/test-serials/1/1"))
          response2 (app (mock/request :post "/series/test-serials2/1/1"))]
      (is (= (:status response) 200))
      (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0201001\"]}"))
      (is (= (:status response2) 200))
      (is (= (:body response2) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0301001\"]}"))))
  (testing "get list of available series"
    (let [response (app (mock/request :get "/series"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "results" ["test-serials", "test-serials2", "test-series"]}))))
  (testing "delete a record"
    (let [response (app (mock/request :delete "/series/test-series/1/2"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101002"]}))))
  (testing "lookup by catalog id"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101001"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"], "records" [
        {"episode_name" "The Cat Returns",
        "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!",
        "episode" 1,
        "season" 1,
        "series" "test-series",
        "imdbid" "tt6565",
        "thumbnail" "http://here.com/img.jpg"
        }]}))))
  (testing "lookup full created record by catalog id"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101004"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101004"], "records" [
        {"episode_name" "The Cat Returns 4",
        "summary" "Definitely jumped the shark by now",
        "episode" 4,
        "season" 1,
        "series" "test-series",
        "imdbid" "tt6533",
        "thumbnail" "http://here.com/jump.jpg"
        }]}))))
  (testing "find specific fields of a second record by catalog_id"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101003?fields=summary"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok","catalog_ids"["TESTS0101003"],"records"
        [{"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"}]}))))
  (testing "find specific series by name"
    (let [response (app (mock/request :get "/series/test-series"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "catalog_ids" ["TESTS0101001","TESTS0101003","TESTS0101004"]
        "records" [{"imdbid" "tt222222"
            "thumbnail" "http://test-series.jpg"
            "summary" "a test series i enjoy"}]}))))
  (testing "partial bulk update works"
    (let [response (app (-> (mock/request :put "/series/test-serials")
                            (mock/json-body {
                            "records" [{
                              "episode_name" "The Cat Returns 2"
                              "summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time"
                             "episode" 1
                             "season" 1
                           }]})))]
    (is (= (:status response) 200))
    (is (= (:body response) "{\"status\":\"ok\",\"catalog_ids\":[\"TESTS0201001\"]}"))))
  (testing "find specific series by name"
    (let [response (app (mock/request :get "/series/test-serials"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "catalog_ids" ["TESTS0201001"]
        "records" [{"imdbid" nil
            "thumbnail" nil
            "summary" nil}]}))))
  (testing "find partial specific episode by name"
    (let [response (app (mock/request :get "/series/test-serials/1/1"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok", "catalog_ids" ["TESTS0201001"]
        "records" [{"episode_name" "The Cat Returns 2",
        "summary" "Wonderful story of cats being cats and everyone loving them. Hooray! For the second time",
        "episode" 1,
        "season" 1,
        "series" "test-serials",
        "imdbid" nil,
        "thumbnail" nil}]}))))
  (testing "not-found route1"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))
  (testing "not-found route2"
    (let [response (app (mock/request :get "/invalid/1/1"))]
      (is (= (:status response) 404))))
  (testing "not-found after delete"
    (let [response (app (mock/request :get "/catalog-id/TESTS0101002"))]
      (is (= (:status response) 404)))))

(deftest post-series
  (testing "create a new series"
    (let [response (app (-> (mock/request :post "/series/new-t")
                            (mock/json-body
                             {"series" {
                                        "name" "new-t"
                                        "summary" "do save"
                                        }
                              "records"[{
                                         "episode_name" "save me"
                                         "episode" 15
                                         "season" 32
                                         "summary" "new episode"
                                         }
                                        {
                                         "episode_name" "me too"
                                         "episode" 4
                                         "season" 4
                                         "summary" "here!"
                                         }]})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok"
              "catalog_ids" ["NEWT00132015","NEWT00104004"]}))))
  (testing "Cannot post existing series"
    (let [response (app (-> (mock/request :post "/series/new-t")
                            (mock/json-body
                             {"series" {
                                        "name" "test-series"
                                        "summary" "don't save"
                                        }
                              "records"[{
                                         "episode_name" "don't save"
                                         "episode" 15
                                         "season" 32
                                         "summary" "new episode"
                                         }]})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response))
             {"status" "failure"
              "message" "Series new-t already exists"}))))
  (testing "find previous series"
    (let [response (app (mock/request :get "/series/new-t"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"records" [{
                        "summary" "do save"
                        "imdbid" nil
                        "thumbnail" nil
                        }]
              "status" "ok"
              "catalog_ids"["NEWT00104004"
                            "NEWT00132015"]})))))

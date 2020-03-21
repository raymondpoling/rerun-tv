(ns omdb-meta.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [omdb-meta.handler :refer :all]
            [cheshire.core :refer :all])
  (:use clj-http.fake))

(deftest omdb-meta-tests
  (testing "create a stub record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/1"
                                    (fn [request] {:status 200 :headers {}
                                      :body (generate-string {:status :ok
                                        :catalog_ids ["TESTS0101001"]})})
                                    "http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
                                    (fn [request]
                                               {:status 200 :headers {}
                                                 :body (generate-string {:Poster "http://blah.jpg"
                                                 :Title "test title 1"
                                                 :imdbID "tt303030"
                                                 :Plot "This is a test file"})})}
      (let [response (app (mock/request :post "/series/test-series/1/1"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"]})))))
  (testing "find a single record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/1"
                                    (fn [request] {:status 200 :headers {}
                                      :body (generate-string {:status :ok
                                    :catalog_ids ["TESTS0101001"],
                                    :records [{"episode_name" "test title 1"
                                                "imdbid" "tt303030"
                                                "thumbnail" "http://blah.jpg"
                                                "summary" "This is a test file"
                                                "series" "test-series"
                                                "season" 1
                                                "episode" 1}]})})}
      (let [response (app (mock/request :get "/series/test-series/1/1"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"],"records"
                                [{"episode_name" "test title 1",
                                  "summary" "This is a test file"
                                  "season" 1
                                  "episode" 1
                                  "series" "test-series"
                                  "imdbid" "tt303030"
                                  "thumbnail" "http://blah.jpg"
                                  }]})))))
  (testing "find a single record's catalog id only"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/1?catalog_id_only=true"
                                    (fn [request]
                                        {:status 200 :headers {}
                                        :body (generate-string {:status :ok
                                      :catalog_ids ["TESTS0101001"]})})}
      (let [response (app (mock/request :get "/series/test-series/1/1?catalog_id_only=true"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"]})))))
  (testing "create a full record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/4"
                                    (fn [request]
                                      (if (= (parse-string (slurp (:body request)))
                                        {"episode_name" "The Cat Returns 4"
                                        "summary" "Definitely jumped the shark by now"
                                        "imdbid" "tt6533"
                                        "thumbnail" "http://here.com/jump.jpg"
                                        "season" "1"
                                        "episode" "4"})
                                        {:status 200 :headers {}
                                        :body (generate-string {:status :ok
                                          :catalog_ids ["TESTS0101004"]})}
                                          {:status 500 :headers {} :body (generate-string {:status :failed})}))}
      (let [response (app (-> (mock/request :post "/series/test-series/1/4")
                              (mock/json-body {
                                "episode_name" "The Cat Returns 4"
                                "summary" "Definitely jumped the shark by now"
                                "imdbid" "tt6533"
                                "thumbnail" "http://here.com/jump.jpg"
                                })))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok" "catalog_ids" ["TESTS0101004"]})))))
  (testing "find all records catalog_ids"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series?catalog_id_only=true"
                                    (fn [request]
                                        {:status 200
                                          :body (generate-string
                                            {:status "ok"
                                            :catalog_ids ["TESTS0101001","TESTS0101002","TESTS0101003","TESTS0101004"]})})}
    (let [response (app (mock/request :get "/series/test-series?catalog_id_only=true"))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok",
        "catalog_ids" ["TESTS0101001","TESTS0101002","TESTS0101003","TESTS0101004"]})))))
  (testing "update the stub record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/1"
                                    {:put (fn [request]
                                      (let [body (parse-string (slurp (:body request)) true)]
                                        (if (= "tt3333" (:imdbid body))
                                          ;;; Throw an error if not updated correctly
                                          {:status 200 :body (generate-string {:catalog_ids ["TESTS0101001"] :status :ok})
                                          :headers {:content-type "application/json"}}
                                          {:status 500})))}
                                      "http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
                                      (fn [request] {:status 200
                                                      :body (generate-string {:Poster "http://other.jpg"
                                                        :Plot "Just another plog"
                                                        :imdbID "tt3333"
                                                        :Title "Another title"
                                                        })})}
      (let [response (app (-> (mock/request :put "/series/test-series/1/1")
                              (mock/json-body {
                                "episode_name" "The Cat Returns"
                                "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!"
                                "thumbnail" "http://here.com/img.jpg"
                                })))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok", "catalog_ids" ["TESTS0101001"]})))))
  (testing "bulk update many records"
    (with-fake-routes-in-isolation {"http://omdb:8888/?apikey=&t=test-series&type=series"
                                    (fn [request]
                                      {:status 200
                                        :body (generate-string
                                          {:Title "blah"
                                          :Plot "another series"
                                          :Poster "http://com.com/jpg.jpg"
                                          :imdbID "tt8998"})})
                                    "http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=2&type=episode"
                                    (fn [request]
                                      {:status 200
                                        :body (generate-string
                                          {:Title "another blah"
                                          :Plot "The one that gets through"
                                          :Poster "http://com.org/org.jpg"
                                          :imdbID "tt3434"})})
                                    "http://meta:4004/series/test-series"
                                    (fn [request]
                                      (let [r (parse-string (slurp (:body request)) true)
                                            series (:series r)
                                            record-1 (first (:records r))
                                            record-2 (second (:records r))]
                                      (if (and (= "a test series i enjoy" (:summary series))
                                                (= "http://com.com/jpg.jpg" (:thumbnail series))
                                                (= "tt8998" (:imdbid series))
                                                (= "another blah" (:episode_name record-1))
                                                (= "The one that gets through" (:summary record-1))
                                                (= "tt6465" (:imdbid record-1))
                                                (= "http://mew2" (:thumbnail record-2))
                                                (= "Not in omdb" (:episode_name record-2))
                                                (= "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this" (:summary record-2))
                                                (= "tt6466" (:imdbid record-2)))
                                              {:status 200 :body (generate-string {:status :ok :catalog_ids ["TESTS0101002""TESTS0101003"]})}
                                              (do
                                                (println "FAILED TO MATCH!" (generate-string request))
                                                {:status 500}))))}
      (let [response (app (-> (mock/request :put "/series/test-series")
                              (mock/json-body {"series" {
                                  "summary" "a test series i enjoy"}
                              "records" [{
                               "episode" 2
                               "season" 1
                               "imdbid" "tt6465"
                               "thumbnail" "http://mew"
                             }
                              {"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"
                              "episode" 3
                              "season" 1
                              "imdbid" "tt6466"
                              "thumbnail" "http://mew2"
                              }]})))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101002","TESTS0101003"]})))))
  (testing "find a single full record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/1"
                                    (fn [request]
                                      {:status 200
                                        :body (generate-string
                                          {:status :ok
                                            :catalog_ids ["TESTS0101001"]
                                            :records [{:episode_name "The Cat Returns"
                                            :summary "Wonderful story of cats being cats and everyone loving them. Hooray!"
                                            :season 1
                                            :episode 1
                                            :series "test-series"
                                            :imdbid "tt6565"
                                            :thumbnail "http://here.com/img.img"}]})})}
      (let [response (app (mock/request :get "/series/test-series/1/1"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101001"],"records"
                                [{"episode_name" "The Cat Returns",
                                "summary" "Wonderful story of cats being cats and everyone loving them. Hooray!"
                                "season" 1
                                "episode" 1
                                "series" "test-series",
                                "imdbid" "tt6565",
                                "thumbnail" "http://here.com/img.img"
                                }]})))))
  (testing "get list of available series"
    (with-fake-routes-in-isolation {"http://meta:4004/series"
                                    (fn [request]
                                      {:status 200
                                        :body (generate-string
                                          {:status :ok, :results ["test-serials", "test-serials2", "test-series"]})})}
      (let [response (app (mock/request :get "/series"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok", "results" ["test-serials", "test-serials2", "test-series"]})))))
  (testing "delete a record"
    (with-fake-routes-in-isolation {"http://meta:4004/series/test-series/1/2"
                                    {:delete (fn [request] {:status 200 :body (generate-string {:status :ok :catalog_ids ["TESTS0101002"]})})}}
      (let [response (app (mock/request :delete "/series/test-series/1/2"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids" ["TESTS0101002"]})))))
  (testing "lookup by catalog id"
    (with-fake-routes-in-isolation {"http://meta:4004/catalog-id/TESTS0101001"
                                    (fn [request] {:status 200
                                      :body (generate-string
                                        {:status :ok :catalog_ids ["TESTS0101001"]
                                        :records [{
                                          :episode_name "The Cat Returns",
                                          :summary "Wonderful story of cats being cats and everyone loving them. Hooray!",
                                          :episode 1,
                                          :season 1,
                                          :series "test-series",
                                          :imdbid "tt6565",
                                          :thumbnail "http://here.com/img.jpg"
                                          }]})})}
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
          }]})))))
  (testing "lookup incomplete record by catalog id"
    (with-fake-routes-in-isolation {"http://meta:4004/catalog-id/TESTS0101004"
                                    (fn [request] {:status 200
                                      :body (generate-string
                                        {:status :ok :catalog_ids ["TESTS0101004"]
                                        :records [{:episode_name "The Cat Returns 4",
                                          :episode 4,
                                          :season 1,
                                          :series "test-series",
                                          :thumbnail "http://here.com/jump.jpg"}]})})
                                      "http://meta:4004/series/test-series/1/4"
                                      (fn [request]
                                        {:status 200 :body (generate-string {:status :ok :catalog_ids ["TESTS0101004"]})})
                                      "http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=4&type=episode"
                                      (fn [request] {
                                        :status 200
                                        :body (generate-string
                                          {:Plot "Definitely jumped the shark by now"
                                          :imdbID "tt6533"
                                          :Title "Cat R4"
                                          :Poster "http://here.org/other.jpg"})
                                        })}
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
          }]})))))
  (testing "find specific fields of a second record by catalog_id"
    (with-fake-routes-in-isolation {"http://meta:4004/catalog-id/TESTS0101003?fields=summary"
                                    (fn [requests]
                                      {:status 200
                                        :body (generate-string
                                          {:status :ok,
                                            :catalog_ids ["TESTS0101003"],
                                            :records
                                            [{:summary "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"}]})})}
      (let [response (app (mock/request :get "/catalog-id/TESTS0101003?fields=summary"))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response)) {"status" "ok","catalog_ids"["TESTS0101003"],"records"
          [{"summary" "Wonderful story of cats being cats and everyone loving them. Hooray! Got tired of this"}]})))))
  (testing "not-found route1"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))
  (testing "not-found route2"
    (let [response (app (mock/request :get "/invalid/1/1"))]
      (is (= (:status response) 404))))
  (testing "not-found after delete"
    (with-fake-routes-in-isolation {"http://meta:4004/catalog-id/TESTS0101002"
                                    (fn [request] {:status 404 :body (generate-string {:status "not found"})})}
      (let [response (app (mock/request :get "/catalog-id/TESTS0101002"))]
        (is (= (:status response) 404))))))

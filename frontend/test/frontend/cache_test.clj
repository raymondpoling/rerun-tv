(ns frontend.cache-test
  (:require [clojure.test :refer [deftest is]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [cheshire.core :refer [generate-string]]
            [frontend.util :refer [testing-with-log-markers]]
            [remote-call.meta :as meta]
            [remote-call.locator :as locator]))

(deftest meta-cache-tests
  (testing-with-log-markers
   "get by catalog-id, save, ensure next get = save, not first fetch"
   (let [test1 "http://omdb:4011/catalog-id/TEST04301001"]
     (with-fake-routes-in-isolation
       {test1
        (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301001"]
                         :records [{:episode_name "test1"
                                    :summary "summary"
                                    :imdbid "tt4565"
                                    :episode 1
                                    :season 1}]})})
        "http://omdb:4011/series/test/1/1"
        (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301001"]})})
        }
       (let [to-save {:episode_name "notTest1" :summary "something else"
                      :episode 1 :season 1}
             response1 (meta/get-meta-by-catalog-id "omdb:4011" "TEST04301001")
             _         (meta/save-episode "omdb:4011" "test" to-save)
             response3 (with-fake-routes-in-isolation
                         {test1
                          (fn [_]
                            {:status 200
                             :headers {:content-type "application/json"}
                             :body (generate-string to-save)})}
                         (meta/get-meta-by-catalog-id
                          "omdb:4011" "TEST04301001"))]
         (is (not (= response1
                     response3)))
         (is (= to-save
                response3))))
  (testing-with-log-markers
   "bulk-update should trigger an evoke"
   (let [test1 "http://omdb:4011/catalog-id/TEST04301002"]
     (with-fake-routes-in-isolation
       {test1
        (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301002"]
                         :records [{:episode_name "test1"
                                    :summary "summary"
                                    :imdbid "tt4565"
                                    :episode 2
                                    :season 1}]})})
        "http://omdb:4011/series/test"
        (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301002"]})})
        }
       (let [to-save {:episode_name "notTest1" :summary "something else"
                      :episode 1 :season 1}
             response1 (meta/get-meta-by-catalog-id "omdb:4011" "TEST04301002")
             _         (meta/bulk-update-series "omdb:4011" "test" to-save)
             response3 (with-fake-routes-in-isolation
                         {test1
                          (fn [_]
                            {:status 200
                             :headers {:content-type "application/json"}
                             :body (generate-string to-save)})}
                         (meta/get-meta-by-catalog-id
                          "omdb:4011" "TEST04301002"))]
         (is (not= response1
                   response3))
         (is (= to-save
                response3))))
  (testing-with-log-markers
   "on creating a new series, evict the series list"
   (let [test1 "http://omdb:4011/series"]
     (with-fake-routes-in-isolation
       {test1
        (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301001"]
                         :results ["a" "b" "c"]})})
        "http://omdb:4011/series/test"
        (fn [_] {:status 200 :body {}}) ;; not actually used
        }
       (let [to-save {:records [{:episode_name "notTest1"
                                 :summary "something else"
                                 :episode 1
                                 :season 1}]}
             response1 (meta/get-all-series "omdb:4011")
             _         (meta/bulk-create-series "omdb:4011" "test" to-save)
             response3 (with-fake-routes-in-isolation
                         {test1
                          (fn [_]
                            {:status 200
                             :headers {:content-type "application/json"}
                             :body (generate-string
                                    {:status "ok"
                                     :results ["a" "b" "c" "d"]})})}
                         (meta/get-all-series "omdb:4011"))]
         (is (not= response1
                   response3))
         (is (= ["a" "b" "c" "d"] response3)))))))))))

(deftest locator-cache-test
    (testing-with-log-markers
   "get by catalog-id, save, ensure next get = save, not first fetch"
   (let [test1 "http://locator:4005/catalog-id/TEST04301001"]
     (with-fake-routes-in-isolation
       {test1
        {:get (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301001"]
                         :files ["file://one"
                                 "http://two"]})})
        :put (fn [_] {:status 200
                 :headers {:content-type "application/json"}
                 :body (generate-string
                        {:status "ok"
                         :catalog_ids ["TEST04301001"]})})
         }
        }
       (let [to-save {:files ["file://a"
                              "http://b"]
                      :catalog_ids ["TEST04301001"]}
             response1 (locator/get-locations "locator:4005" "TEST04301001")
             _         (locator/save-locations "locator:4005"
                                               "TEST04301001"
                                               to-save)
             response3 (with-fake-routes-in-isolation
                         {test1
                          (fn [_]
                            {:status 200
                             :headers {:content-type "application/json"}
                             :body (generate-string to-save)})}
                         (locator/get-locations
                          "locator:4005" "TEST04301001"))]
         (is (not (= response1
                     response3)))
         (is (= (:files to-save)
                response3)))))))

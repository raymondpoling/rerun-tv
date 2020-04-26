(ns redis-cache.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [redis-cache.core :refer [make-cache redis-cache evict]]
            [cheshire.core :refer [generate-string]]))


(deftest basic-cache-tests
  (make-cache)
  (let [host "host"
        path "/uri1"]
    (with-fake-routes-in-isolation
      {"http://host/uri1"
       (fn [_] {:status 200
                :body (generate-string {:a 1 :b 2})})}
      (testing "check cache, if empty, fetch response"
        (let [actual (redis-cache host path)]
          (is (= actual
                 {:a 1 :b 2}))))
      (testing "check cache, if not empty, get cache response"
        (with-fake-routes-in-isolation
          {"http://host/uri1"
           (fn [_] {:status 200
                    :body (generate-string {:b 3 :c 4})})}
          (let [actual (redis-cache host path)]
            (is (= actual
                   {:a 1 :b 2})))))
      (testing "forcibly evict from cache, find anotehr response"
        (with-fake-routes-in-isolation
          {"http://host/uri1"
           (fn [_] {:status 200
                    :body (generate-string {:c 5 :d 6})})}
          (evict host path)
          (let [actual (redis-cache host path)]
            (is (= actual
                   {:c 5 :d 6}))))))))

(ns ^:integration redis-cache.integration-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [cheshire.core :refer [generate-string]]
            [taoensso.carmine :as redis]
            [redis-cache.core :as cache]
            [clojure.core.cache :as c]))

(def test-binding {:pool {}
                   :spec {:uri "redis://localhost:6379"}})

(deftest integrated-cache-tests-api
  (binding [cache/internal-cache
            (atom (cache/make-cache
                   :conn test-binding))]
    (redis/wcar test-binding (redis/flushall))
      (let [host "host"
            path "/uri1"]
        (with-fake-routes-in-isolation
          {"http://host/uri1"
           (fn [_] {:status 200
                    :body (generate-string {:status "ok" :a 1 :b 2})})}
          (testing "check cache, if empty, fetch response"
            (let [actual (cache/redis-cache host path)]
              (is (= actual
                     {:status "ok" :a 1 :b 2}))))
          (testing "check cache, if not empty, get cache response"
            (with-fake-routes-in-isolation
              {"http://host/uri1"
               (fn [_] {:status 200
                        :body (generate-string {:status "ok" :b 3 :c 4})})}
              (let [actual (cache/redis-cache host path)]
                (is (= actual
                       {:status "ok" :a 1 :b 2})))))
          (testing "forcibly evict from cache, find anotehr response"
            (with-fake-routes-in-isolation
              {"http://host/uri1"
               (fn [_] {:status 200
                        :body (generate-string {:status "ok" :c 5 :d 6})})}
              (cache/evict host path)
              (let [actual (cache/redis-cache host path)]
                (is (= actual
                       {:status "ok" :c 5 :d 6})))))))
      (redis/wcar test-binding (redis/flushall))))

(deftest cache-interface-test
  (binding [cache/internal-cache
            (atom (cache/make-cache
                   :conn test-binding))]
    (redis/wcar test-binding (redis/flushall))
    (testing "lookup one arg miss"
      (let [actual (c/lookup @cache/internal-cache "key")]
        (is (nil? actual))))
    (testing "lookup two arg missing"
      (let [actual (c/lookup @cache/internal-cache "key"
                             "KEY")]
        (is (= actual "KEY"))))
    (testing "call miss"
      (let [actual (c/miss @cache/internal-cache
                           "key"
                             {:status "ok"
                              :t "KEY"})]
        (is (= actual @cache/internal-cache))))
    (testing "call hit"
      (let [actual (c/hit @cache/internal-cache "key")]
        (is (= actual @cache/internal-cache))))
    (testing "lookup one arg hit"
      (let [actual (c/lookup @cache/internal-cache "key")]
        (is (= actual {:status "ok"
                       :t "KEY"}))))
    (testing "lookup one arg returns cache value, not miss"
      (let [actual (c/lookup @cache/internal-cache "key" "FALSE")]
        (is (= actual {:status "ok" :t "KEY"}))))
    (testing "evict a key (means lookup won't find)"
      (let [key (c/has? @cache/internal-cache "key")
            _ (c/evict @cache/internal-cache "key")
            no-key (c/has? @cache/internal-cache "key")]
        (is (= key true))
        (is (= no-key false))))
    (redis/wcar test-binding (redis/flushall))))

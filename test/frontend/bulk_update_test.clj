(ns frontend.bulk-update-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all]
            [frontend.make-cookie :refer [make-cookie]]
            [clojure.tools.logging :as logger])
  (:use clj-http.fake))

(defn make-response [response]
  {:headers {:content-type "application/json"}
   :body (generate-string response)})

(deftest test-all-missing-preview
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "user has no access"
      (logger/info "user has no access")
      (with-fake-routes-in-isolation
        {}
        (let [response (app (-> (mock/request :get "/bulk-update.html")
                                user-cookie))]
          (is (= (:status response) 302))
          (is (get (:headers response) "Location")
              "http://localhost:4008/login.html"))))

    (testing "get basic service with media"
      (logger/info "get basic service with media")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         }
        (let [response (app (-> (mock/request :get "/bulk-update.html")
                                media-cookie))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<select id=\"series\" name=\"series\">"
                     "<option>one</option>"
                     "<option>two</option>"
                     "<option>three</option>"
                     "</select>.*"))
               (:body response))))))
    (testing "if omdb down, no series"
      (logger/info "if omdb down, no series")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (throw (Exception. "Must fail.")))
         }
        (let [response (app (-> (mock/request :get "/bulk-update.html")
                                media-cookie))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<select id=\"series\" name=\"series\">"
                     "<option>not available</option>"
                     "</select>.*"))
               (:body response))))))
    (testing "Create new"
      (logger/info "Create new")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         "http://omdb:4011/series/one"
         {:post (fn [_] (make-response
                         {:status :ok :catalog_ids ["ONE000101001"
                                                    "ONE000101002"
                                                    "ONE000101003"]}))}
         }
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body {:series "one"
                                            :update
                                            (generate-string
                                             {:records
                                              [{:season 1
                                                :episode 1}
                                               {:season 1
                                                :episode 2}
                                               {:season 1
                                                :episode 3}]})
                                            :create? :create})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Updated Catalog Ids</h3><ol>"
                     "<li>ONE000101001</li>"
                     "<li>ONE000101002</li>"
                     "<li>ONE000101003</li></ol>"
                     ".*"))
               (:body response))))))
    (testing "Create new but two exist"
      (logger/info "Create new but two exist")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         "http://omdb:4011/series/one"
         {:post (fn [_] (make-response
                         {:status :ok
                          :catalog_ids ["ONE000101003"]
                          :failures ["ONE000101001"
                                     "ONE000101002"]}))}
         }
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body {:series "one"
                                            :update
                                            (generate-string
                                             {:records
                                              [{:season 1
                                                :episode 1}
                                               {:season 1
                                                :episode 2}
                                               {:season 1
                                                :episode 3}]})
                                            :create? :create})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Updated Catalog Ids</h3><ol>"
                     "<li>ONE000101003</li></ol>"
                     ".*"))
               (:body response)))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Failed Catalog Ids</h3><ol>"
                     "<li>ONE000101001</li>"
                     "<li>ONE000101002</li>"
                     "</ol>"
                     ".*"))
               (:body response))))))
    (testing "Update existing"
      (logger/info "Update existing")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         "http://omdb:4011/series/one"
         {:put (fn [request]
                 (let [t (parse-string (slurp (:body request)) true)]
                 (if (= (:records t)
                        [{:season 1 :episode 1}
                         {:season 1 :episode 2}
                         {:season 1 :episode 3}])
                 (make-response
                  {:status :ok :catalog_ids ["ONE000101001",
                                             "ONE000101002",
                                             "ONE000101003"]})
                 {:status 500})))}
         }
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body {:series "one"
                                            :update
                                            (generate-string
                                             {:records
                                              [{:season 1
                                                :episode 1}
                                               {:season 1
                                                :episode 2}
                                               {:season 1
                                                :episode 3}]})})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Updated Catalog Ids</h3><ol>"
                     "<li>ONE000101001</li>"
                     "<li>ONE000101002</li>"
                     "<li>ONE000101003</li></ol>"
                     ".*"))
               (:body response))))))
    (testing "Update existing but one exists"
      (logger/info "Update existing but one exists")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         "http://omdb:4011/series/one"
         {:put (fn [request]
                 (let [t (parse-string (slurp (:body request)) true)]
                 (if (= (:records t)
                        [{:season 1 :episode 1}
                         {:season 1 :episode 2}
                         {:season 1 :episode 3}])
                 (make-response
                  {:status :ok
                   :catalog_ids ["ONE000101002",
                                 "ONE000101003"]
                   :failures ["ONE000101001"]})
                 {:status 500})))}
         }
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body {:series "one"
                                            :update
                                            (generate-string
                                             {:records
                                              [{:season 1
                                                :episode 1}
                                               {:season 1
                                                :episode 2}
                                               {:season 1
                                                :episode 3}]})})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Updated Catalog Ids</h3><ol>"
                     "<li>ONE000101002</li>"
                     "<li>ONE000101003</li></ol>"
                     ".*"))
               (:body response)))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Failed Catalog Ids</h3><ol>"
                     "<li>ONE000101001</li>"
                     "</ol>"
                     ".*"))
               (:body response))))))
    (testing "series name in record takes precedence"
      (logger/info "series name in record takes precedence")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_]
           (make-response {:status "ok"
                           :results ["one","two","three"]}))
         "http://omdb:4011/series/two"
         {:put (fn [request]
                 (let [t (parse-string (slurp (:body request)) true)]
                   (if (= (:records t)
                          [{:season 1 :episode 1}
                           {:season 1 :episode 2}
                           {:season 1 :episode 3}])
                     (make-response
                      {:status :ok :catalog_ids ["TWO000101001",
                                                 "TWO000101002",
                                                 "TWO000101003"]})
                     {:status 500})))}
         }
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body {:series "one"
                                            :update
                                            (generate-string
                                             {:series {
                                                       :name "two"
                                                       }
                                              :records
                                              [{:season 1
                                                :episode 1}
                                               {:season 1
                                                :episode 2}
                                               {:season 1
                                                :episode 3}]})})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str "(?s).*"
                     "<h3>Updated Catalog Ids</h3><ol>"
                     "<li>TWO000101001</li>"
                     "<li>TWO000101002</li>"
                     "<li>TWO000101003</li></ol>"
                     ".*"))
               (:body response))))))))

(deftest parse-error
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "on post parse error, populate failed"
      (logger/info "on post parse error, populate failed")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series"
         (fn [_] (make-response {:status "ok"
                                 :results ["one","two","three"]}))}
        (let [response (app (-> (mock/request :post "/bulk-update.html")
                                media-cookie
                                (mock/body
                                 {:series "any"
                                  :update
                                  "{\"series\"{\"name\":\"don mai\" }}"})))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str
                 "(?s).*"
                 "<h3>Failure</h3><p>"
                 "Unexpected character \\('\\{' \\(code 123\\)\\): was expecting a colon to separate field name and value\n at .*"
                 "</p>.*"))
               (:body response))))))))

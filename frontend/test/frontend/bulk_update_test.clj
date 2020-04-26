(ns frontend.bulk-update-test
  (:require [clojure.test :refer [deftest is]]
            [ring.mock.request :as mock]
            [frontend.handler :refer [app]]
            [cheshire.core :refer [parse-string generate-string]]
            [frontend.util :refer [make-cookie
                                   make-response
                                   testing-with-log-markers]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-all-missing-preview
  (let [media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing-with-log-markers
     "user has no access"
     (with-fake-routes-in-isolation
        {}
        (let [response (app (-> (mock/request :get "/bulk-update.html")
                                user-cookie))]
          (is (= (:status response) 302))
          (is (get (:headers response) "Location")
              "http://localhost:4008/login.html"))))

    (testing-with-log-markers
     "if omdb down, no series"
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
    (testing-with-log-markers "get basic service with media"
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
    (testing-with-log-markers "Create new"
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
    (testing-with-log-markers "Create new but two exist"
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
    (testing-with-log-markers "Update existing"
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
    (testing-with-log-markers "Update existing but one exists"
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
    (testing-with-log-markers "series name in record takes precedence"
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
  (let [media-cookie (make-cookie "media")]
    (testing-with-log-markers "on post parse error, populate failed"
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

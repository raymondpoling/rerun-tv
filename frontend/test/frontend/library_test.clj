(ns frontend.library-test
  (:require
   [clojure.test :refer [deftest is]]
   [ring.mock.request :as mock]
   [redis-cache.core :as cache]
   [frontend.handler :refer [app]]
   [frontend.util :refer [make-cookie
                          make-response
                          testing-with-log-markers
                          basic-matcher]]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-admin-routes
  (binding [cache/internal-cache (atom (cache/make-cache))]
    (let [user-cookie (make-cookie "user")]
      (testing-with-log-markers
       "user can view library"
       (with-fake-routes-in-isolation
         {"http://omdb:4011/series"
          (fn [_] (make-response {:status "ok"
                                  :results ["one""two""three"]}))
          "http://omdb:4011/series/one"
          (fn [_] (make-response {:status "ok"
                                  :records [{"name" "one"
                                             "summary" "one's summary"
                                             "imdbid" "tt3232"
                                             "thumbnail" "http://tt.com/tt.jpg"
                                             }]
                                  :catalog_ids ["ONE000101001"]}))
          "http://omdb:4011/catalog-id/ONE000101001"
          (fn [_] (make-response {:status "ok"
                                  :records [{"episode_name""one1"
                                             "season" 1
                                             "episode" 1
                                             "imdbid" "tt8989"
                                             "summary" "one1 summary"
                                             "series" "one"}]
                                  :catalog_ids ["ONE000101001"]
                                  }))
          }
         (let [response (app (-> (mock/request :get "/library.html")
                                 user-cookie))]
           (is (= (:status response) 200))
           (is (basic-matcher (str
                               "<option selected=\"selected\">one</option>"
                               "<option>two</option>"
                               "<option>three</option>")
                              (:body response)))
           (is (basic-matcher "<h2>one</h2>" (:body response)))
           (is (basic-matcher
                (str "<a href=\"http://imdb.com/title/tt3232\" "
                     "target=\"_blank\">IMDB</a>")
                (:body response)))
           (is (basic-matcher
                (str "<a href=\"/update-series.html\\?series-name=one\">"
                     "Edit Series</a>")
                (:body response)))
           (is (basic-matcher "<p>one's summary<br>" (:body response)))
           (is (basic-matcher "<img src=\"http://tt.com/tt.jpg\">"
                              (:body response)))
           (is (basic-matcher "<em>one1</em>" (:body response)))
           (is (basic-matcher
                (str "<a href=\"http://imdb.com/title/tt8989\" "
                     "target=\"_blank\">one S1E1</a>")
                (:body response)))
           (is (basic-matcher "<p>one1 summary</p>" (:body response)))
           (is (basic-matcher (str
                               "<img src=\"http://tt.com/tt.jpg\">"
                               "<ul class=\"textbox\">")
                              (:body response))))))
      (testing-with-log-markers
       "user can view library"
       (with-fake-routes-in-isolation
         {"http://omdb:4011/series"
          (fn [_] (make-response {:status "ok"
                                  :results ["one""two""three"]}))
          "http://omdb:4011/series/two"
          (fn [_] (make-response {:status "ok"
                                  :records [{"name" "two"
                                             "summary" "two's summary"
                                             "imdbid" "tt2323"
                                             }]
                                  :catalog_ids ["TWO000101001"
                                                "TWO000102001"
                                                ]}))
          "http://omdb:4011/catalog-id/TWO000101001"
          (fn [_] (make-response {:status "ok"
                                  :records [{"episode_name""two1"
                                             "season" 1
                                             "episode" 1
                                             "imdbid" "tt2324"
                                             "summary" "two1 summary"
                                             "series" "two"}]
                                  :catalog_ids ["TWO000101001"]}))
          "http://omdb:4011/catalog-id/TWO000102001"
          (fn [_] (make-response {:status "ok"
                                  :records [{"episode_name""two2"
                                             "season" 2
                                             "episode" 1
                                             "imdbid" "tt2325"
                                             "summary" "two2 summary"
                                             "series" "two"}]
                                  :catalog_ids ["TWO000102001"]}))
          }
         (let [response (app (-> (mock/request
                                  :get "/library.html?series-name=two")
                                 user-cookie))]
           (is (= (:status response) 200))
           (is (basic-matcher (str
                               "<option>one</option>"
                               "<option selected=\"selected\">two</option>"
                               "<option>three</option>")
                              (:body response)))
           (is (basic-matcher "<h2>two</h2>" (:body response)))
           (is (basic-matcher
                (str "<a href=\"http://imdb.com/title/tt2323\" "
                     "target=\"_blank\">IMDB</a>")
                (:body response)))
           (is (basic-matcher
                (str "<a href=\"/update-series.html\\?series-name=two\">"
                     "Edit Series</a>")
                (:body response)))
           (is (basic-matcher "<p>two's summary<br>" (:body response)))
           (is (basic-matcher "<em>two1</em>" (:body response)))
           (is (basic-matcher
                (str "<a href=\"http://imdb.com/title/tt2324\" "
                     "target=\"_blank\">two S1E1</a>")
                (:body response)))
           (is (basic-matcher "<p>two1 summary</p>" (:body response)))
           (is (basic-matcher "<em>two2</em>" (:body response)))
           (is (basic-matcher
                (str "<a href=\"http://imdb.com/title/tt2325\" "
                     "target=\"_blank\">two S2E1</a>")
                (:body response)))
           (is (basic-matcher "<p>two2 summary</p>" (:body response)))
           (is (basic-matcher (str
                               "<img src=\"/image/not-available.svg\">"
                               "<ul class=\"textbox\">")
                              (:body response)))
           ))))))

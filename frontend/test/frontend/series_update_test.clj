(ns frontend.series-update-test
  (:require [clojure.test :refer [deftest is]]
            [ring.mock.request :as mock]
            [frontend.handler :refer [app]]
            [frontend.util :refer [basic-matcher make-cookie
                                   make-response testing-with-log-markers]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest update-series
  (let [media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing-with-log-markers "user has no access"
      (with-fake-routes-in-isolation
        {}
        (let [response (app (-> (mock/request :get "/update-series.html")
                                user-cookie))]
          (is (= (:status response) 302))
          (is (get (:headers response) "Location")
              "http://localhost:4008/login.html"))))
    (testing-with-log-markers "basic series get"
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series/jane"
         (fn [_] (make-response
                  {:status "ok"
                   :records [{
                            :imdbid "tt4567"
                            :summary "a brief summary"
                            :thumbnail "http://jpg.com/jpg.jpg"}]}))}
        (let [response (app (-> (mock/request
                                 :get
                                 "/update-series.html?series-name=jane")
                                media-cookie))]
          (is (= (:status response) 200))
          (is (basic-matcher
                 "<input id=\"imdbid\" name=\"imdbid\" value=\"tt4567\">"
                 (:body response)))
          (is (basic-matcher
               (str "<input id=\"name\" "
                    "name=\"name\" value=\"jane\"")
               (:body response)))
          (is (basic-matcher
               (str "<textarea id=\"summary\" name=\"summary\">"
                    "a brief summary</textarea>")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"thumbnail\" name=\"thumbnail\" "
                    "value=\"http://jpg.com/jpg.jpg\">")
               (:body response))))))
    (testing-with-log-markers "basic series post"
      (with-fake-routes-in-isolation
        {"http://omdb:4011/series/yellow"
         {:get (fn [_] (make-response
                        {:status "ok"
                         :records [{
                                    :imdbid "tt4567"
                                    :summary "a brief summary"
                                    :thumbnail "http://jpg.com/jpg.jpg"}]}))
          :put (fn [r]
                  (make-response
                   {:status "ok"
                    :catalog_ids ["AAAAA0103004"]
                    :records [{:imdbid (:imdbid (:body r))
                               :summary (:summary (:body r))
                               :thumbnail "N/A"}]}))}
         }
        (let [response (app (-> (mock/request
                                 :post
                                 "/update-series.html?name=yellow")
                                (mock/body {:series "yellow"
                                            :imdbid "tt4343"
                                            :summary "too yellow"
                                            :thumbnail "N/A"
                                            :mode "Save"
                                            :files "http://crystalball/mnt/values.mkv\nfile://localhost/mnt/value.mkv"})
                                media-cookie))]
          (is (= (:status response) 200))
          (is (basic-matcher
               "<input id=\"imdbid\" name=\"imdbid\" value=\"tt4343\">"
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"name\" "
                    "name=\"name\" value=\"yellow\"")
               (:body response)))
          (is (basic-matcher
               (str "<textarea id=\"summary\" name=\"summary\">"
                    "too yellow</textarea>")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"thumbnail\" name=\"thumbnail\" "
                    "value=\"N/A\">")
               (:body response))))))
    (testing-with-log-markers "omdb lookup side-by-side"
      (with-fake-routes-in-isolation
        {"http://omdb:4011/imdbid/series/tt4343"
         {:get (fn [_] (make-response
                        {:status "ok"
                         :records [{
                                    :imdbid "tt4343"
                                    :summary "a brief summary"
                                    :thumbnail "http://jpg.com/jpg.jpg"}]}))
          :post (fn [r]
                  (make-response
                   {:status "ok"
                    :catalog_ids ["AAAAA0103004"]
                    :records [{
                               :series (:series (:body r))
                               :imdbid (:imdbid (:body r))
                               :summary (:summary (:body r))
                               :thumbnail "N/A"}]}))}
         "http://locator:4005/catalog-id/AAAAA0103004"
         (fn [_]
           (make-response {:status "ok"}))
         }
        (let [response (app (->
                             (mock/request
                              :post
                              "/update-series.html?name=yellow")
                             (mock/body {
                                         :imdbid "tt4343"
                                         :summary "too yellow"
                                         :thumbnail "N/A"
                                         :mode "IMDB Lookup"
                                         :files "http://crystalball/mnt/values.mkv\nfile://localhost/mnt/value.mkv"})
                             media-cookie))]
          (is (= (:status response) 200))
          (is (basic-matcher
               "<input id=\"imdbid\" name=\"imdbid\" type=\"radio\" value=\"tt4343\">"
               (:body response)))
          (is (basic-matcher
               (str "<input checked=\"checked\" id=\"summary\" "
                    "name=\"summary\" type=\"radio\" "
                    "value=\"a brief summary\">")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"summary\" name=\"summary\" "
                    "type=\"radio\" value=\"too yellow\">")
               (:body response)))
          (is (basic-matcher
               (str "<input checked=\"checked\" id=\"thumbnail\" "
                    "name=\"thumbnail\" "
                    "type=\"radio\" value=\"http://jpg.com/jpg.jpg\">")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"thumbnail\" name=\"thumbnail\" "
                    "type=\"radio\" value=\"N/A\">")
               (:body response))))))))

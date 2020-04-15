(ns frontend.update-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all]
            [frontend.util :refer [make-cookie
                                   make-response
                                   basic-matcher]]
            [clojure.tools.logging :as logger])
  (:use clj-http.fake))

(deftest update-episode
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "user has no access"
      (logger/info "user has no access")
      (with-fake-routes-in-isolation
        {}
        (let [response (app (-> (mock/request :get "/update.html")
                                user-cookie))]
          (is (= (:status response) 302))
          (is (get (:headers response) "Location")
              "http://localhost:4008/login.html"))))
    (testing "basic episode get"
      (logger/info "basic episode get")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/catalog-id/AAAAA0102010"
         (fn [_] (make-response {:status "ok"
                                 :records [{
                                            :episode_name "new world order"
                                            :imdbid "tt4567"
                                            :summary "a brief summary"
                                            :season 2
                                            :episode 10
                                            :thumbnail "http://jpg.com/jpg.jpg"}]}))}
        (let [response (app (-> (mock/request
                                 :get
                                 "/update.html?catalog-id=AAAAA0102010")
                                media-cookie))]
          (is (= (:status response) 200))
          (is (basic-matcher
                 "<input id=\"imdbid\" name=\"imdbid\" value=\"tt4567\">"
                 (:body response)))
          (is (basic-matcher
               (str "<input id=\"episode_name\" "
                    "name=\"episode_name\" value=\"new world order\"")
               (:body response)))
          (is (basic-matcher
               (str "<textarea id=\"summary\" name=\"summary\">"
                    "a brief summary</textarea>")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"thumbnail\" name=\"thumbnail\" "
                    "value=\"http://jpg.com/jpg.jpg\">")
               (:body response)))
          (is (basic-matcher
               "<input id=\"episode\" name=\"episode\" value=\"10\">"
               (:body response)))
          (is (basic-matcher
               "<input id=\"season\" name=\"season\" value=\"2\">"
               (:body response))))))
    (testing "basic episode post"
      (logger/info "basic episode post")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/catalog-id/AAAAA0102010"
         {:get (fn [_] (make-response {:status "ok"
                                 :records [{
                                            :episode_name "new world order"
                                            :imdbid "tt4567"
                                            :summary "a brief summary"
                                            :season 2
                                            :episode 10
                                            :thumbnail "http://jpg.com/jpg.jpg"}]}))
          :post (fn [r]
                  (make-response
                   {:status "ok"
                    :catalog_ids ["AAAAA0103004"]
                    :records [{:episode_name (:episode_name (:body r))
                               :series (:series (:body r))
                               :imdbid (:imdbid (:body r))
                               :season (:season (:body r))
                               :episode (:episode (:body r))
                               :summary (:summary (:body r))
                               :thumbnail "N/A"}]}))}
         "http://locator:4005/catalog-id/AAAAA0103004"
         (fn [r]
           (make-response {:status "ok"}))
         }
        (let [response (app (-> (mock/request
                                 :post
                                 "/update.html?catalog-id=YELLO0103004")
                                (mock/body {:episode_name "yellow"
                                            :series "yellow"
                                            :imdib "tt4343"
                                            :summary "too yellow"
                                            :season 3
                                            :episode 4
                                            :thumbnail "N/A"
                                            :mode "Save"
                                            :files "http://crystalball/mnt/values.mkv\nfile://localhost/mnt/value.mkv"})
                                media-cookie))]
          (is (= (:status response) 302))
          (is (= (get (:headers response) "Location")
                 "http://localhost/update.html?catalog-id=YELLO0103004")))))
    (testing "omdb lookup side-by-side"
      (logger/info "omdb lookup side-by-side")
      (with-fake-routes-in-isolation
        {"http://omdb:4011/imdbid/tt4343"
         {:get (fn [_] (make-response {:status "ok"
                                 :records [{
                                            :episode_name "new world order"
                                            :imdbid "tt4343"
                                            :summary "a brief summary"
                                            :season 2
                                            :episode 10
                                            :thumbnail "http://jpg.com/jpg.jpg"}]}))
          :post (fn [r]
                  (make-response
                   {:status "ok"
                    :catalog_ids ["AAAAA0103004"]
                    :records [{:episode_name (:episode_name (:body r))
                               :series (:series (:body r))
                               :imdbid (:imdbid (:body r))
                               :season (:season (:body r))
                               :episode (:episode (:body r))
                               :summary (:summary (:body r))
                               :thumbnail "N/A"}]}))}
         "http://locator:4005/catalog-id/AAAAA0103004"
         (fn [r]
           (make-response {:status "ok"}))
         }
        (let [response (app (-> (mock/request
                                 :post
                                 "/update.html?catalog-id=YELLO0103004")
                                (mock/body {:episode_name "yellow"
                                            :series "yellow"
                                            :imdbid "tt4343"
                                            :summary "too yellow"
                                            :season 3
                                            :episode 4
                                            :thumbnail "N/A"
                                            :mode "IMDB Lookup"
                                            :files "http://crystalball/mnt/values.mkv\nfile://localhost/mnt/value.mkv"})
                                media-cookie))]
          (is (= (:status response) 200))
          (is (basic-matcher
               "<input id=\"imdbid\" name=\"imdbid\" type=\"radio\" value=\"tt4343\">"
               (:body response)))
          (is (basic-matcher
               (str "<input checked=\"checked\" id=\"episode_name\" "
                    "name=\"episode_name\" type=\"radio\" "
                    "value=\"new world order\">")
               (:body response)))
          (is (basic-matcher
               (str "<input id=\"episode_name\" name=\"episode_name\" "
                    "type=\"radio\" value=\"yellow\">")
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
               (:body response)))
          (is (basic-matcher
               (str "<input checked=\"checked\" id=\"episode\" "
                    "name=\"episode\" type=\"radio\" value=\"10\">")
               (:body response)))
          (is (basic-matcher
               (str "<input checked=\"checked\" id=\"season\" "
                    "name=\"season\" type=\"radio\" value=\"2\">")
               (:body response))))))))

(ns omdb-meta.update-test
  (:require [clojure.test :refer [deftest is testing]]
            [omdb-meta.update :refer [update-episode update-series]]
            [cheshire.core :refer [generate-string]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))


(deftest update-test-episode
  (testing "completely replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {:Title "maki maki"
                :Plot "alpha 12"
                :imdbID "tt434343"
                :Poster "http://lugos/lug.jpg"})})}
      (let [response (update-episode {:episode 1 :season 1} "test-series" "omdb:8888" "")]
        (is (= response
                {:episode 1 :season 1
                  :summary "alpha 12" :imdbid "tt434343"
                  :episode_name "maki maki"
                  :thumbnail "http://lugos/lug.jpg"})))))
  (testing "partial replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {:Title "maki maki"
                :Plot "alpha 12"
                :imdbID "tt434343"
                :Poster "http://lugos/lug.jpg"})})}
      (let [response (update-episode {:episode 1 :season 1 :summary "original summary" :episode_name "original name"} "test-series" "omdb:8888" "")]
        (is (= response
                {:episode 1 :season 1
                  :summary "original summary" :imdbid "tt434343"
                  :episode_name "original name"
                  :thumbnail "http://lugos/lug.jpg"})))))
  (testing "information not in response"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {})})}
      (let [response (update-episode {:episode 1 :season 1} "test-series" "omdb:8888" "")]
        (is (= response
                {:episode 1 :season 1
                  :summary "Not in omdb" :imdbid ""
                  :episode_name "Not in omdb"
                  :thumbnail ""})))))
  (testing "error replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&Season=1&Episode=1&type=episode"
        (fn [_]
          {:status 500
            :headers {}
              :body (generate-string
                {})})}
      (let [response (update-episode {:episode 1 :season 1} "test-series" "omdb:8888" "")]
        (is (= response
                {:episode 1 :season 1
                  :summary "Not in omdb" :imdbid ""
                  :episode_name "Not in omdb"
                  :thumbnail ""}))))))

(deftest update-test-series
  (testing "completely replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&type=series"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {:Title "maki maki"
                :Plot "alpha 12"
                :imdbID "tt434343"
                :Poster "http://lugos/lug.jpg"})})}
      (let [response (update-series {} "test-series" "omdb:8888" "")]
        (is (= response
                {  :summary "alpha 12" :imdbid "tt434343"
                  :thumbnail "http://lugos/lug.jpg"})))))
  (testing "partial replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&type=series"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {:Title "maki maki"
                :Plot "alpha 12"
                :imdbID "tt434343"
                :Poster "http://lugos/lug.jpg"})})}
      (let [response (update-series {:summary "original summary"} "test-series" "omdb:8888" "")]
        (is (= response
                {  :summary "original summary" :imdbid "tt434343"
                  :thumbnail "http://lugos/lug.jpg"})))))
  (testing "information not in response"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&type=series"
        (fn [_]
          {:status 200
            :headers {}
              :body (generate-string
                {})})}
      (let [response (update-series {} "test-series" "omdb:8888" "")]
        (is (= response
                {:summary "Not in omdb" :imdbid ""
                  :thumbnail ""})))))
  (testing "error replace"
    (with-fake-routes-in-isolation
      {"http://omdb:8888/?apikey=&t=test-series&type=series"
        (fn [_]
          {:status 500
            :headers {}
              :body (generate-string
                {})})}
      (let [response (update-series {} "test-series" "omdb:8888" "")]
        (is (= response
                {  :summary "Not in omdb" :imdbid ""
                  :thumbnail ""}))))))

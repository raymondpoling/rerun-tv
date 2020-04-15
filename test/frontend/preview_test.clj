(ns frontend.preview-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all]
            [frontend.util :refer [make-cookie]])
  (:use clj-http.fake))

(defn make-response [response]
  {:headers {:content-type "application/json"}
   :body (generate-string response)})

(deftest test-all-missing-preview
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "if no services available, get something"
      (with-fake-routes-in-isolation {}
        (let [response (app (-> (mock/request :get "/preview.html")
                                user-cookie))]
          (is (= (:status response) 200))
                                        ; No schedules, but html exists
          (is (re-matches
               #"(?s).*<select name=\"schedule\"></select>.*"
               (:body response))))))))

(deftest all-working
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "check a full page"
      (with-fake-routes-in-isolation
        {"http://schedule:4000"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://user:4002/user/one?preview=true"
         (fn [_] (make-response {:status :ok
                                 :idx 40}))
         "http://schedule:4000/one/40"
         (fn [_] (make-response {:status :ok
                                 :items [{"name" "AAAAA","index" 24},
                                         {"name" "BBBBB","index" 3},
                                         {"name" "CCCCC","index" 5}]}))
         "http://schedule:4000/one/39"
         (fn [_] (make-response {:status :ok
                                 :items [{"name" "AAAAA","index" 23},
                                         {"name" "BBBBB","index" 2},
                                         {"name" "CCCCC","index" 4}]}))
         "http://schedule:4000/one/41"
         (fn [_] (make-response {:status :ok
                                 :items [{"name" "AAAAA","index" 25},
                                         {"name" "BBBBB","index" 4},
                                         {"name" "CCCCC","index" 6}]}))
         "http://playlist:4001/AAAAA/24"
         (fn [_] (make-response {:status :ok
                                 :item "AAAAA0102012"}))
         "http://playlist:4001/AAAAA/23"
         (fn [_] (make-response {:status :ok
                                 :item "AAAAA0102011"}))
         "http://playlist:4001/AAAAA/25"
         (fn [_] (make-response {:status :ok
                                 :item "AAAAA0102013"}))
         "http://playlist:4001/BBBBB/2"
         (fn [_] (make-response {:status :ok
                                 :item "BBBBB0101002"}))
         "http://playlist:4001/BBBBB/3"
         (fn [_] (make-response {:status :ok
                                 :item "BBBBB0101003"}))
         "http://playlist:4001/BBBBB/4"
         (fn [_] (make-response {:status :ok
                                 :item "BBBBB0101004"}))
         "http://playlist:4001/CCCCC/4"
         (fn [_] (make-response {:status :ok
                                 :item "CCCCC0101004"}))
         "http://playlist:4001/CCCCC/5"
         (fn [_] (make-response {:status :ok
                                 :item "CCCCC0101005"}))
         "http://playlist:4001/CCCCC/6"
         (fn [_] (make-response {:status :ok
                                 :item "CCCCC0101006"}))
         "http://omdb:4011/catalog-id/AAAAA0102011"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102011"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 1"
                                            "episode" 11
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0102012"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102012"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 2"
                                            "episode" 12
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0102013"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102013"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 3"
                                            "episode" 13
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/BBBBB0101002"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["BBBBB0101002"]
                                 :records [{"series" "A v B v C"
                                            "episode_name" "A"
                                            "episode" 2
                                            "season" 1
                                            "summary" "A wins"
                                            "imdbid" "tt2222"
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/BBBBB0101003"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["BBBBB0101003"]
                                 :records [{"series" "A v B v C"
                                            "episode_name" "B"
                                            "episode" 3
                                            "season" 1
                                            "summary" "B wins"
                                            "imdbid" "tt3333"
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/BBBBB0101004"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["BBBBB0101004"]
                                 :records [{"series" "A v B v C"
                                            "episode_name" "C"
                                            "episode" 4
                                            "season" 1
                                            "summary" "C wins"
                                            "imdbid" "tt4444"
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/CCCCC0101006"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["CCCCC0101006"]
                                 :records [{"series" "Yap!"
                                            "episode_name" "yappy again"
                                            "episode" 6
                                            "season" 1
                                            "summary" "it yaps even more"
                                            "imdbid" "tt45456"
                                            "thumbnail" "image/yap3.jpg"}]}))
         "http://omdb:4011/catalog-id/CCCCC0101005"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["CCCCC0101005"]
                                 :records [{"series" "Yap!"
                                            "episode_name" "not so yappy"
                                            "episode" 5
                                            "season" 1
                                            "summary" "it stops yapping"
                                            "imdbid" "tt45455"
                                            "thumbnail" "image/yap2.jpg"}]}))
         "http://omdb:4011/catalog-id/CCCCC0101004"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["CCCCC0101004"]
                                 :records [{"series" "Yap!"
                                            "episode_name" "yappy"
                                            "episode" 4
                                            "season" 1
                                            "summary" "it yaps"
                                            "imdbid" "tt45454"
                                            "thumbnail" "image/yap1.jpg"}]}))
         }
        (let [response (app (-> (mock/request :get "/preview.html")
                                user-cookie))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str
                 "(?s)"
                 ".*<h2>one: 39</h2>"
                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li>SQUID S2E11</li>"
                 ".*<li><em>squid 1</em></li>"
                 ".*<p>not in omdb</p>"

                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt2222\" target=\"_blank\">A v B v C S1E2</a></li>"
                 ".*<li><em>A</em></li>"
                 ".*<p>A wins</p>"

                 ".*<img src=\"image/yap1.jpg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt45454\" target=\"_blank\">Yap! S1E4</a></li>"
                 ".*<li><em>yappy</em></li>"
                 ".*<p>it yaps</p>"

                 ".*<h2>one: 40</h2>"
                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li>SQUID S2E12</li>"
                 ".*<li><em>squid 2</em></li>"
                 ".*<p>not in omdb</p>"

                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt3333\" target=\"_blank\">A v B v C S1E3</a></li>"
                 ".*<li><em>B</em></li>"
                 ".*<p>B wins</p>"

                 ".*<img src=\"image/yap2.jpg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt45455\" target=\"_blank\">Yap! S1E5</a></li>"
                 ".*<li><em>not so yappy</em></li>"
                 ".*<p>it stops yapping</p>"

                 ".*<h2>one: 41</h2>"
                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li>SQUID S2E13</li>"
                 ".*<li><em>squid 3</em></li>"
                 ".*<p>not in omdb</p>"

                 ".*<img src=\"/image/not-available.svg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt4444\" target=\"_blank\">A v B v C S1E4</a></li>"
                 ".*<li><em>C</em></li>"
                 ".*<p>C wins</p>"

                 ".*<img src=\"image/yap3.jpg\">"
                 ".*<li><a href=\"http://imdb.com/title/tt45456\" target=\"_blank\">Yap! S1E6</a></li>"
                 ".*<li><em>yappy again</em></li>"
                 ".*"))
               (:body response))))))))

(deftest download-test
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing "check download"
      (with-fake-routes-in-isolation
        {"http://schedule:4000/"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://format:4009/admin/two?index=12"
         (fn [r] {:headers {"Content-Type"
                            "application/mpegurl"
                            "Content-Disposition"
                            "attachment; filename=\"two-12.m3u\""}
                  :body "a file!"})}
        (let [response (app (-> (mock/request :get "/preview.html")
                                admin-cookie
                                (mock/body {:schedule "two"
                                            :index 12
                                            :download true})))]
          (is (= (:status response) 200))
          (is (= (get (:headers response) "content-disposition")
                 "attachment; filename=\"two-12.m3u\""))
          (is (= (get (:headers response) "content-type")
                 "application/mpegurl"))
          (is (= (:body response) "a file!")))))
    (testing "check download with update"
      (with-fake-routes-in-isolation
        {"http://schedule:4000/"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://format:4009/admin/two?index=12&update=update"
         (fn [r] {:headers {"Content-Type"
                            "application/mpegurl"
                            "Content-Disposition"
                            "attachment; filename=\"two-12.m3u\""}
                  :body "a file!"})}
        (let [response (app (-> (mock/request :get "/preview.html")
                                admin-cookie
                                (mock/body {:schedule "two"
                                            :index 12
                                            :download "download"
                                            :update "update"})))]
          (is (= (:status response) 200))
          (is (= (get (:headers response) "content-disposition")
                 "attachment; filename=\"two-12.m3u\""))
          (is (= (get (:headers response) "content-type")
                 "application/mpegurl"))
          (is (= (:body response) "a file!")))))))

(deftest preview-with-all-options
    (let [admin-cookie (make-cookie "admin")
          media-cookie (make-cookie "media")
          user-cookie (make-cookie "user")]
      (testing "test reset and schedule name"
        (with-fake-routes-in-isolation
          {"http://schedule:4000"
           (fn [_] (make-response {:status :ok
                                   :schedules ["one","two","three"]}))
           "http://user:4002/admin/three?preview=true"
           (fn [_] (make-response {:status :ok
                                   :idx 22}))
           "http://schedule:4000/three/22"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 22}]}))
           "http://schedule:4000/three/23"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 1}]}))
           "http://schedule:4000/three/21"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 21}]}))
           "http://playlist:4001/AAAAA/22"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0102013"}))
           "http://playlist:4001/AAAAA/1"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0101001"}))
           "http://playlist:4001/AAAAA/21"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0102012"}))
         "http://omdb:4011/catalog-id/AAAAA0102013"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102013"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 1"
                                            "episode" 13
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0102012"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102012"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 2"
                                            "episode" 12
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0101001"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0101001"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 3"
                                            "episode" 1
                                            "season" 1
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         }
        (let [response (app (-> (mock/request :get "/preview.html")
                                (mock/body {:reset "reset"
                                            :idx "32"
                                            :index "44"
                                            :schedule "three"})
                                admin-cookie))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str
                 "(?s)"
                 ".*<h2>three: 21</h2>"
                 ".*<h2>three: 22</h2>"
                 ".*<h2>three: 23</h2>"
                 ".*"))
               (:body response))))))
      (testing "test schedule name and index"
        (with-fake-routes-in-isolation
          {"http://schedule:4000"
           (fn [_] (make-response {:status :ok
                                   :schedules ["one","two","three"]}))
           "http://schedule:4000/two/44"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 22}]}))
           "http://schedule:4000/two/43"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 1}]}))
           "http://schedule:4000/two/45"
           (fn [_] (make-response {:status :ok
                                   :items [{"name" "AAAAA","index" 21}]}))
           "http://playlist:4001/AAAAA/22"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0102013"}))
           "http://playlist:4001/AAAAA/1"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0101001"}))
           "http://playlist:4001/AAAAA/21"
           (fn [_] (make-response {:status :ok
                                   :item "AAAAA0102012"}))
         "http://omdb:4011/catalog-id/AAAAA0102013"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102013"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 1"
                                            "episode" 13
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0102012"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0102012"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 2"
                                            "episode" 12
                                            "season" 2
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         "http://omdb:4011/catalog-id/AAAAA0101001"
         (fn [_] (make-response {:status :ok
                                 :catalog_ids ["AAAAA0101001"]
                                 :records [{"series" "SQUID"
                                            "episode_name" "squid 3"
                                            "episode" 1
                                            "season" 1
                                            "summary" "not in omdb"
                                            "imdbid" ""
                                            "thumbnail" "N/A"}]}))
         }
        (let [response (app (-> (mock/request :get "/preview.html")
                                (mock/body {:idx "32"
                                            :index "44"
                                            :schedule "two"})
                                media-cookie))]
          (is (= (:status response) 200))
          (is (re-matches
               (re-pattern
                (str
                 "(?s)"
                 ".*<h2>two: 43</h2>"
                 ".*<h2>two: 44</h2>"
                 ".*<h2>two: 45</h2>"
                 ".*"))
               (:body response))))))))

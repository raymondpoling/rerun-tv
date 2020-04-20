(ns frontend.preview-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [frontend.handler :refer [app]]
            [cheshire.core :refer [generate-string]]
            [frontend.util :refer [make-cookie
                                   each-line-and-combined
                                   basic-matcher]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(defn make-response [response]
  {:headers {:content-type "application/json"}
   :body (generate-string response)})

(deftest test-all-missing-preview
  (let [user-cookie (make-cookie "user")]
    (testing "if no services available, get something"
      (with-fake-routes-in-isolation {}
        (let [response (app (-> (mock/request :get "/preview.html")
                                user-cookie))]
          (is (= (:status response) 200))
                                        ; No schedules, but html exists
          (is (basic-matcher
               "<select name=\"schedule\"></select>"
               (:body response))))))))

(deftest all-working
  (let [user-cookie (make-cookie "user")]
    (testing "check a full page"
      (with-fake-routes-in-isolation
        {"http://schedule:4000"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://user:4002/user/one?preview=true"
         (fn [_] (make-response {:status :ok
                                 :idx 40}))
         "http://format:4009/user/one?index=40"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"playlist" {"name" "squid"
                                                         "index" 3}
                                             "series" "SQUID"
                                             "episode_name" "squid 2"
                                             "episode" 12
                                             "season" 2
                                             "summary" "not in omdb"
                                             "imdbid" ""
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "AvBvC"
                                                         "index" 25}
                                             "series" "A v B v C"
                                             "episode_name" "B"
                                             "episode" 3
                                             "season" 1
                                             "summary" "B wins"
                                             "imdbid" "tt3333"
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "yap"
                                                         "index" 14}
                                             "series" "Yap!"
                                             "episode_name" "not so yappy"
                                             "episode" 5
                                             "season" 1
                                             "summary" "it stops yapping"
                                             "imdbid" "tt45455"
                                             "thumbnail" "image/yap2.jpg"}]}))
         "http://format:4009/user/one?index=39"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"playlist" {"name" "squid"
                                                         "index" 2}
                                             "series" "SQUID"
                                             "episode_name" "squid 1"
                                             "episode" 11
                                             "season" 2
                                             "summary" "not in omdb"
                                             "imdbid" ""
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "AvBvC"
                                                         "index" 24}
                                             "series" "A v B v C"
                                             "episode_name" "A"
                                             "episode" 2
                                             "season" 1
                                             "summary" "A wins"
                                             "imdbid" "tt2222"
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "yap"
                                                         "index" 13}
                                             "series" "Yap!"
                                             "episode_name" "yappy"
                                             "episode" 4
                                             "season" 1
                                             "summary" "it yaps"
                                             "imdbid" "tt45454"
                                             "thumbnail" "image/yap1.jpg"}]}))
         "http://format:4009/user/one?index=41"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"playlist" {"name" "squid"
                                                         "index" 4}
                                             "series" "SQUID"
                                             "episode_name" "squid 3"
                                             "episode" 13
                                             "season" 2
                                             "summary" "not in omdb"
                                             "imdbid" ""
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "AvBvC"
                                                         "index" 26}
                                             "series" "A v B v C"
                                             "episode_name" "C"
                                             "episode" 4
                                             "season" 1
                                             "summary" "C wins"
                                             "imdbid" "tt4444"
                                             "thumbnail" "N/A"},
                                            {"playlist" {"name" "yap"
                                                         "index" 15}
                                             "series" "Yap!"
                                             "episode_name" "yappy again"
                                             "episode" 6
                                             "season" 1
                                             "summary" "it yaps even more"
                                             "imdbid" "tt45456"
                                             "thumbnail" "image/yap3.jpg"}]}))
         }
        (let [response (app (-> (mock/request :get "/preview.html")
                                user-cookie))]
          (is (= (:status response) 200))
          (each-line-and-combined (:body response)
                                  "<h2>one: 39</h2>"
                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">squid: 2</li>"
                                  "<li>SQUID S2E11</li>"
                                  "<li><em>squid 1</em></li>"
                                  "<p>not in omdb</p>"

                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">AvBvC: 24</li>"
                                  "<li><a href=\"http://imdb.com/title/tt2222\" target=\"_blank\">A v B v C S1E2</a></li>"
                                  "<li><em>A</em></li>"
                                  "<p>A wins</p>"

                                  "<img src=\"image/yap1.jpg\">"
                                  "<li class=\"index\">yap: 13</li>"
                                  "<li><a href=\"http://imdb.com/title/tt45454\" target=\"_blank\">Yap! S1E4</a></li>"
                                  "<li><em>yappy</em></li>"
                                  "<p>it yaps</p>"

                                  "<div class=\"column\" "
                                  "style=\"border:solid black 1px;border-radius: 0.5em\">"
                                  "<h2>one: 40</h2>"
                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">squid: 3</li>"
                                  "<li>SQUID S2E12</li>"
                                  "<li><em>squid 2</em></li>"
                                  "<p>not in omdb</p>"

                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">AvBvC: 25</li>"
                                  "<li><a href=\"http://imdb.com/title/tt3333\" target=\"_blank\">A v B v C S1E3</a></li>"
                                  "<li><em>B</em></li>"
                                  "<p>B wins</p>"

                                  "<img src=\"image/yap2.jpg\">"
                                  "<li class=\"index\">yap: 14</li>"
                                  "<li><a href=\"http://imdb.com/title/tt45455\" target=\"_blank\">Yap! S1E5</a></li>"
                                  "<li><em>not so yappy</em></li>"
                                  "<p>it stops yapping</p>"

                                  "<h2>one: 41</h2>"
                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">squid: 4</li>"
                                  "<li>SQUID S2E13</li>"
                                  "<li><em>squid 3</em></li>"
                                  "<p>not in omdb</p>"

                                  "<img src=\"/image/not-available.svg\">"
                                  "<li class=\"index\">AvBvC: 26</li>"
                                  "<li><a href=\"http://imdb.com/title/tt4444\" target=\"_blank\">A v B v C S1E4</a></li>"
                                  "<li><em>C</em></li>"
                                  "<p>C wins</p>"

                                  "<img src=\"image/yap3.jpg\">"
                                  "<li class=\"index\">yap: 15</li>"
                                  "<li><a href=\"http://imdb.com/title/tt45456\" target=\"_blank\">Yap! S1E6</a></li>"
                                  "<li><em>yappy again</em></li>"))))))

(deftest download-test
  (let [admin-cookie (make-cookie "admin")]
    (testing "check download"
      (with-fake-routes-in-isolation
        {"http://schedule:4000/"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://user:4002/admin/two?preview=true"
         (fn [_] (make-response {:status :ok
                                 :idx 12}))
         "http://format:4009/admin/two?protocol=http&host=archive&index=12&format=m3u"
         (fn [_] {:headers {"Content-Type"
                            "application/mpegurl"
                            "Content-Disposition"
                            "attachment; filename=\"two-12.m3u\""}
                  :body "a file!"})}
        (let [response (app (-> (mock/request :get "/preview.html")
                                admin-cookie
                                (mock/body {:protocol-host "http/archive"
                                            :format "m3u"
                                            :schedule "two"
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
         "http://format:4009/admin/two?protocol=http&host=archive&index=12&update=update&format=m3u"
         (fn [_] {:headers {"Content-Type"
                            "application/mpegurl"
                            "Content-Disposition"
                            "attachment; filename=\"two-12.m3u\""}
                  :body "a file!"})}
        (let [response (app (-> (mock/request :get "/preview.html")
                                admin-cookie
                                (mock/body {:format "m3u"
                                            :protocol-host "http/archive"
                                            :schedule "two"
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
        media-cookie (make-cookie "media")]
    (testing "test reset and schedule name"
      (with-fake-routes-in-isolation
        {"http://schedule:4000"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://user:4002/admin/three?preview=true"
         (fn [_] (make-response {:status :ok
                                 :idx 22}))
         "http://format:4009/admin/three?index=22"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"series" "SQUID"
                                             "episode_name" "squid 1"
                                             "episode" 13
                                             "season" 2
                                             "summary" "not in omdb"
                                             "imdbid" ""
                                             "thumbnail" "N/A"}]}))
         "http://format:4009/admin/three?index=23"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"series" "SQUID"
                                             "episode_name" "squid 3"
                                             "episode" 1
                                             "season" 1
                                             "summary" "not in omdb"
                                             "imdbid" ""
                                             "thumbnail" "N/A"}]}))
         "http://format:4009/admin/three?index=21"
         (fn [_] (make-response {:status :ok
                                 :playlist [{"series" "SQUID"
                                             "episode_name" "squid 2"
                                             "episode" 12
                                             "season" 2
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
          (is (basic-matcher
               (str
                "<h2>three: 21</h2>"
                ".*<h2>three: 22</h2>.*"
                "<h2>three: 23</h2>")
               (:body response))))))
    (testing "test schedule name and index"
      (with-fake-routes-in-isolation
        {"http://schedule:4000"
         (fn [_] (make-response {:status :ok
                                 :schedules ["one","two","three"]}))
         "http://format:4009/media/two?index=44"
         (fn [_] (make-response {:status :ok
                                 :items [{"series" "SQUID"
                                          "episode_name" "squid 1"
                                          "episode" 13
                                          "season" 2
                                          "summary" "not in omdb"
                                          "imdbid" ""
                                          "thumbnail" "N/A"}]}))
         "http://format:4009/media/two?index=43"
         (fn [_] (make-response {:status :ok
                                 :items [{"series" "SQUID"
                                          "episode_name" "squid 3"
                                          "episode" 1
                                          "season" 1
                                          "summary" "not in omdb"
                                          "imdbid" ""
                                          "thumbnail" "N/A"}]}))
         "http://format:4009/media/two?index=45"
         (fn [_] (make-response {:status :ok
                                 :items [{"series" "SQUID"
                                          "episode_name" "squid 2"
                                          "episode" 12
                                          "season" 2
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
          (is (basic-matcher
               ["<h2>two: 43</h2>"
                "<h2>two: 44</h2>"
                "<h2>two: 45</h2>"]
               (:body response))))))))

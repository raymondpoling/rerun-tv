(ns frontend.schedule-post-test
  (:require
   [clojure.test :refer [deftest is]]
   [ring.mock.request :as mock]
   [frontend.handler :refer [app]]
   [cheshire.core :refer [generate-string]]
   [frontend.util :refer [make-cookie
                          make-response
                          testing-with-log-markers
                          basic-matcher]]
   [clojure.string :as cls]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-schedule-post-routes-access
  (let [media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing-with-log-markers
     "user cannot view schedule builder get"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :post "/schedule-builder.html")
                               user-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html")))))
    (testing-with-log-markers
     "media gets redirected trying to create existing schedule"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "two"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status :ok
                                :schedule {:name "two"
                                           :playlists [{:type "playlist"
                                             :length 20
                                             :name "my:SYSTEM"}]}}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Create"})))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                (str "http://localhost/schedule-builder.html?"
                     "message=Schedule with name 'two' already exists"))))))))

(deftest schedule-post-get-logic-tests
  (let [media-cookie (make-cookie "media")
        space? "\\s*"
        object-start "\\{"
        object-stop "\\}"
        list-start "\\["
        list-stop "\\]"]
    (testing-with-log-markers
     "media makes a new schedule template shows up"
     (with-fake-routes-in-isolation
       {
        "http://builder:4003/schedule/validate"
        (fn [_] (make-response {:status :ok}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                            {:name "system:SYSTEM" :length 12}
                                            {:name "b:SYSTEM" :length 12}]}))
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "not found"
                                }))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Create"
                                           :preview "true"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr><td class=\"empty\">Empty</td></tr>"
              (:body response)))
         (is (basic-matcher
              (str
               "<option>a:SYSTEM 12</option>"
               "<option>system:SYSTEM 12</option>"
               "<option>b:SYSTEM 12</option>")
              (:body response))))))
    (testing-with-log-markers
     "media pulls an existing table and it is drawn up"
     (with-fake-routes-in-isolation
       {
        "http://builder:4003/schedule/validate"
        (fn [_] (make-response {:status :ok}))
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "two"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "okay"
                                :schedule {
                                           :name "two"
                                           :playlists
                                           [{
                                             :type "playlist"
                                             :name "system:SYSTEM"
                                             :length 12
                                             }]
                                           }}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                        {:name "system:SYSTEM" :length 12}
                                        {:name "b:SYSTEM" :length 12}]}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :preview "true"
                                           :mode "Update"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                object-start "\"type\"" ":" "\"playlist\","
                "\"name\"" ":" "\"system:SYSTEM\","
                "\"length\"" ":" "12"
                object-stop list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr class=\"playlist\">"
              (:body response)))
         (is (basic-matcher
              "<th class=\"first\" scope=\"row\">Playlist: 12<br>RR: 1.00</th>"
              (:body response)))
         (is (basic-matcher
              (str "<td colspan=\"12\">"
                   "<span class=\"name\">system:SYSTEM</span>"
                   "<br><span class=\"count\">Count: 12</span></td>")
              (:body response))))))
(testing-with-log-markers
     "media updates schedule"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "not found"}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                        {:name "system:SYSTEM" :length 12}
                                        {:name "b:SYSTEM" :length 12}]}))
        "http://builder:4003/validate/two"
        (fn [_] (make-response {:status :ok}))
        "http://builder:4003/schedule/store/two"
        (fn [_] (make-response {:status :ok}))
        "http://messages:4010/"
        (fn [_] (make-response {:status :ok}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Create"
                                           :schedule-body
                                           (generate-string
                                            {:name "two",
                                             :playlists [{:type "playlist"
                                                          :name "b:SYSTEM"
                                                :length 12}]})})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                object-start "\"type\"" ":" "\"playlist\","
                "\"name\"" ":" "\"b:SYSTEM\","
                "\"length\"" ":" "12"
                object-stop list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr class=\"playlist\">"
              (:body response)))
         (is (basic-matcher
              "<th class=\"first\" scope=\"row\">Playlist: 12<br>RR: 1.00</th>"
              (:body response)))
         (is (basic-matcher
              (str "<td colspan=\"12\">"
                   "<span class=\"name\">b:SYSTEM</span>"
                   "<br><span class=\"count\">Count: 12</span></td>")
              (:body response))))))
(testing-with-log-markers
     "media updates schedule"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "two"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "okay"
                                :schedule {
                                           :name "two"
                                           :playlists
                                           [{
                                             :type "playlist"
                                             :name "system:SYSTEM"
                                             :length 12
                                             }]
                                           }}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                        {:name "system:SYSTEM" :length 12}
                                        {:name "b:SYSTEM" :length 12}]}))
        "http://builder:4003/validate/two"
        (fn [_] (make-response {:status :ok}))
        "http://builder:4003/schedule/store/two"
        (fn [_] (make-response {:status :ok}))
        "http://messages:4010/"
        (fn [_] (make-response {:status :ok}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Update"
                                           :schedule-body
                                           (generate-string
                                            {:name "two",
                                             :playlists [{:type "playlist"
                                                          :name "b:SYSTEM"
                                                :length 12}]})})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                object-start "\"type\"" ":" "\"playlist\","
                "\"name\"" ":" "\"b:SYSTEM\","
                "\"length\"" ":" "12"
                object-stop list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr class=\"playlist\">"
              (:body response)))
         (is (basic-matcher
              "<th class=\"first\" scope=\"row\">Playlist: 12<br>RR: 1.00</th>"
              (:body response)))
         (is (basic-matcher
              (str "<td colspan=\"12\">"
                   "<span class=\"name\">b:SYSTEM</span>"
                   "<br><span class=\"count\">Count: 12</span></td>")
              (:body response))))))
(testing-with-log-markers
     "media updates schedule but fails on validation"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "two"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "okay"
                                :schedule {
                                           :name "two"
                                           :playlists
                                           [{
                                             :type "playlist"
                                             :name "system:SYSTEM"
                                             :length 12
                                             }]
                                           }}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                        {:name "system:SYSTEM" :length 12}
                                        {:name "b:SYSTEM" :length 12}]}))
        "http://builder:4003/validate/two"
        (fn [_] (make-response {:status :ok}))
        "http://builder:4003/schedule/store/two"
        (fn [_] (make-response {:status :failed
                                :messages ["fail 1" "fail 2"]}))
        "http://messages:4010/"
        (fn [_] (make-response {:status :ok}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Update"
                                           :schedule-body
                                           (generate-string
                                            {:name "two",
                                             :playlists [{:type "playlist"
                                                          :name "b:SYSTEM"
                                                :length 12}]})})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                object-start "\"type\"" ":" "\"playlist\","
                "\"name\"" ":" "\"b:SYSTEM\","
                "\"length\"" ":" "12"
                object-stop list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr class=\"playlist\">"
              (:body response)))
         (is (basic-matcher
              "<th class=\"first\" scope=\"row\">Playlist: 12<br>RR: 1.00</th>"
              (:body response)))
         (is (basic-matcher
              (str "<td colspan=\"12\">"
                   "<span class=\"name\">b:SYSTEM</span>"
                   "<br><span class=\"count\">Count: 12</span></td>")
              (:body response)))
         (is (basic-matcher
              (str "<li>fail 1</li>"
                   "<li>fail 2</li>")
              (:body response))))))
(testing-with-log-markers
     "media creates schedule but fails on validation"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "three"]}))
        "http://schedule:4000/two"
        (fn [_] (make-response {:status "not found"}))
        "http://playlist:4001"
        (fn [_] (make-response {:status :ok
                                :playlists [{:name "a:SYSTEM" :length 12}
                                        {:name "system:SYSTEM" :length 12}
                                        {:name "b:SYSTEM" :length 12}]}))
        "http://builder:4003/validate/two"
        (fn [_] (make-response {:status :ok}))
        "http://builder:4003/schedule/store/two"
        (fn [_] (make-response {:status :failed
                                :messages ["fail 1" "fail 2"]}))
        "http://messages:4010/"
        (fn [_] (make-response {:status :ok}))
        }
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :mode "Create"
                                           :schedule-body
                                           (generate-string
                                            {:name "two",
                                             :playlists [{:type "playlist"
                                                          :name "b:SYSTEM"
                                                :length 12}]})})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (cls/join
               space?
               [object-start "\"name\"" ":" "\"two\","
                "\"playlists\"" ":" list-start
                object-start "\"type\"" ":" "\"playlist\","
                "\"name\"" ":" "\"b:SYSTEM\","
                "\"length\"" ":" "12"
                object-stop list-stop object-stop])
              (:body response)))
         (is (basic-matcher
              "<tr class=\"playlist\">"
              (:body response)))
         (is (basic-matcher
              "<th class=\"first\" scope=\"row\">Playlist: 12<br>RR: 1.00</th>"
              (:body response)))
         (is (basic-matcher
              (str "<td colspan=\"12\">"
                   "<span class=\"name\">b:SYSTEM</span>"
                   "<br><span class=\"count\">Count: 12</span></td>")
              (:body response)))
         (is (basic-matcher
              (str "<li>fail 1</li>"
                   "<li>fail 2</li>")
              (:body response))))))))

(deftest table-logic-tests
  (let [media-cookie (make-cookie "media")
        all-playlists [{:name "a"
                        :length 12}
                       {:name "b"
                        :length 25}
                       {:name "c"
                        :length 13}
                       {:name "d"
                        :length 8}
                       {:name "e"
                        :length 42}]
        standard-routes {
                         "http://builder:4003/schedule/validate"
                         (fn [_] (make-response {:status :ok}))
                         "http://schedule:4000/"
                         (fn [_] (make-response {:status :ok
                                                 :schedules ["one"
                                                             "two"
                                                             "three"]}))
                         "http://playlist:4001"
                         (fn [_] (make-response {:status :ok
                                                 :playlists all-playlists}))
                         }] 
    (testing-with-log-markers
     "media pulls table with just playlist"
     (with-fake-routes-in-isolation
       (merge standard-routes
              {"http://schedule:4000/two"
               (fn [_] (make-response
                        {:status "ok"
                         :schedule {
                                    :name "two"
                                    :playlists
                                    [{:type "playlist"
                                      :name "e"
                                      :length 42}]}
                         }))})
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :preview "true"
                                           :mode "Update"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              "<tr class=\"playlist\">.*</tr>"
              (:body response)))
         (is (basic-matcher
              (str
               "<th class=\"first\" scope=\"row\">"
               "Playlist: 42<br>RR: 1.00</th>")
              (:body response)))
         (is (basic-matcher
              (str
               "<td colspan=\"42\"><span class=\"name\">e</span>"
               "<br><span class=\"count\">Count: 42</span></td>")
              (:body response))))))
    (testing-with-log-markers
     "media pulls table with just merge"
     (with-fake-routes-in-isolation
       (merge standard-routes
              {"http://schedule:4000/two"
               (fn [_] (make-response
                        {:status "ok"
                         :schedule {
                                    :name "two"
                                    :playlists
                                    [{:type "merge"
                                      :playlists [{:type "playlist"
                                                   :name "e"
                                                   :length 42}
                                                  {:type "playlist"
                                                   :name "a"
                                                   :length 12}]}]}
                         }))})
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :preview "true"
                                           :mode "Update"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              "<tr class=\"merge\">.*</tr>"
              (:body response)))
         (is (basic-matcher
              (str
               "<th class=\"first\" scope=\"row\">"
               "Merge: 54<br>RR: 1.00</th>")
              (:body response)))
         (is (basic-matcher
              (str
               "<td colspan=\"42\"><span class=\"name\">e</span>"
               "<br><span class=\"count\">Count: 42</span></td>"
               "<td colspan=\"12\"><span class=\"name\">a</span>"
               "<br><span class=\"count\">Count: 12</span></td>")
              (:body response))))))
    (testing-with-log-markers
     "media pulls table with just multi"
     (with-fake-routes-in-isolation
       (merge standard-routes
              {"http://schedule:4000/two"
               (fn [_] (make-response
                        {:status "ok"
                         :schedule {
                                    :name "two"
                                    :playlists
                                    [{:type "multi"
                                      :step 2
                                      :start 1
                                      :playlist {:type "merge"
                                                 :playlists
                                                 [{:type "playlist"
                                                   :name "e"
                                                   :length 42}
                                                  {:type "playlist"
                                                   :name "a"
                                                   :length 12}]}}
                                     {:type "multi"
                                      :step 2
                                      :start 1
                                      :playlist {:type "merge"
                                                 :playlists
                                                 [{:type "playlist"
                                                   :name "e"
                                                   :length 42}
                                                  {:type "playlist"
                                                   :name "a"
                                                   :length 12}]}}]}
                         }))})
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :preview "true"
                                           :mode "Update"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (str
               "<tr class=\"multi\">.*</tr>"
               "<tr class=\"multi\">.*</tr>")
              (:body response)))
         (is (basic-matcher
              (str
               "<th class=\"first\" scope=\"row\">"
               "Multi: 27.0<br>RR: 1.00<br>Step: 2</th>.*"
               "<th class=\"first\" scope=\"row\">"
               "Multi: 27.0<br>RR: 1.00<br>Step: 2</th>")
              (:body response)))
         (is (basic-matcher
              (str
               "<td colspan=\"21\"><span class=\"name\">e</span>"
               "<br><span class=\"count\">Count: 42</span></td>"
               "<td colspan=\"6\"><span class=\"name\">a</span>"
               "<br><span class=\"count\">Count: 12</span></td>")
              (:body response))))))
    (testing-with-log-markers
     "media pulls table with just complex"
     (with-fake-routes-in-isolation
       (merge standard-routes
              {"http://schedule:4000/two"
               (fn [_] (make-response
                        {:status "ok"
                         :schedule {
                                    :name "two"
                                    :playlists
                                    [{:type "complex"
                                      :playlists [{:type "merge"
                                                 :playlists
                                                   [{:type "playlist"
                                                     :name "e"
                                                     :length 42}
                                                    {:type "playlist"
                                                     :name "a"
                                                     :length 12}]}
                                                  {:type "merge"
                                                   :playlists
                                                   [
                                                    {:type "playlist"
                                                     :length 8
                                                     :name "d"}
                                                    {:type "playlist"
                                                     :length 25
                                                     :name "b"}
                                                    {:type "playlist"
                                                     :length 13
                                                     :name "c"}]}
                                                  ]}]}
                         }))})
       (let [response (app (-> (mock/request
                                :post
                                "/schedule-builder.html")
                               media-cookie
                               (mock/body {:schedule-name "two"
                                           :preview "true"
                                           :mode "Update"})))]
         (is (= (:status response) 200))
         (is (basic-matcher
               "<tr class=\"complex\">.*</tr>"
              (:body response)))
         (is (basic-matcher
              (str
               "<th class=\"first\" scope=\"row\">"
               "Complex: 108<br>RR: 1.00</th>")
              (:body response)))
         (is (basic-matcher
              (str
               "<td colspan=\"108\"><table")
              (:body response)))
         (is (basic-matcher
              (str
               "<tr class=\"merge\">.*</tr>.*"
               "<tr class=\"merge\">.*</tr>")
              (:body response)))
         (is (basic-matcher
              "<th colspan=\"108\">Playlists</th>"
              (:body response))))))
    ))


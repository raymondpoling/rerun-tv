(ns frontend.schedule-get-test
  (:require
   [clojure.test :refer [deftest is]]
   [ring.mock.request :as mock]
   [frontend.handler :refer [app]]
   [frontend.util :refer [make-cookie
                          make-response
                          testing-with-log-markers
                          basic-matcher]]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-schedule-get-routes
  (let [media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (testing-with-log-markers
     "user cannot view schedule builder post"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :get "/schedule-builder.html")
                               user-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html")))))
    (testing-with-log-markers
     "media gets page"
     (with-fake-routes-in-isolation
       {
        "http://schedule:4000/"
        (fn [_] (make-response {:status :ok
                                :schedules ["one"
                                            "two"
                                            "three"]}))
        }
       (let [response (app (-> (mock/request :get "/schedule-builder.html")
                               media-cookie))]
         (is (= (:status response) 200))
         (is (basic-matcher
              (str
               "<option></option>"
               "<option>one</option>"
               "<option>two</option>"
               "<option>three</option>")
              (:body response))))))))

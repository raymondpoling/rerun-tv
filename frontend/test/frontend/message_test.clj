(ns frontend.message-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [frontend.handler :refer [app]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [frontend.util :refer [make-cookie
                                   testing-with-log-markers]]))

(deftest test-messages
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]
    (with-fake-routes-in-isolation
      {
       "http://messages:4010/messages/"
       (fn [_] {:status 200 :body {}})
       }
      (testing-with-log-markers
       "can get page with auth"
       (let [req (-> (mock/request :get "/message.html")
                     admin-cookie)
             response (app req)]
         (is (= (:status response) 200))
         (is (re-matches #"(?s).*<textarea.*" (:body response)))))

      (testing-with-log-markers
       "cannot get page with user"
       (let [response (app (-> (mock/request :get "/message.html")
                               user-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html"))))

      (testing-with-log-markers
       "cannot get page with media"
       (let [response (app (-> (mock/request :get "/message.html")
                               media-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html"))))

      (testing-with-log-markers
       "can post as auth"
       (let [req (-> (mock/request :post "/message.html")
                     admin-cookie)
             response (app req)]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html"))))

      (testing-with-log-markers
       "cannot post as media"
       (let [response (app (-> (mock/request :post "/message.html")
                               (mock/body {"message" "doesn't matter"
                                           "title" "who cares"})
                               media-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html"))))

      (testing-with-log-markers
       "cannot post as user"
       (let [response (app (-> (mock/request :post "/message.html")
                               (mock/body {"message" "doesn't matter"
                                           "title" "who cares"})
                               media-cookie))]
         (is (= (:status response) 302))
         (is (= (get (:headers response) "Location")
                "http://localhost/index.html")))))))

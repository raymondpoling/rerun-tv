(ns frontend.message-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all]
            [frontend.make-cookie :refer [make-cookie]])
  (:use clj-http.fake))

(deftest test-messages
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")
        user-cookie (make-cookie "user")]

    (testing "can get page with auth"
      (let [req (-> (mock/request :get "/message.html")
                    admin-cookie)
            response (app req)]
        (is (= (:status response) 200))
        (is (re-matches #"(?s).*<textarea.*" (:body response)))))

  (testing "cannot get page with user"
    (let [response (app (-> (mock/request :get "/message.html")
                            user-cookie))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/index.html"))))

  (testing "cannot get page with media"
    (let [response (app (-> (mock/request :get "/message.html")
                            media-cookie))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/index.html"))))

  (testing "can post as auth"
    (let [req (-> (mock/request :post "/message.html")
                  admin-cookie)
          response (app req)]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/index.html"))))

  (testing "cannot post as media"
    (let [response (app (-> (mock/request :post "/message.html")
                            (mock/body {"message" "doesn't matter"
                                        "title" "who cares"})
                            media-cookie))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/index.html"))))

  (testing "cannot post as user"
    (let [response (app (-> (mock/request :post "/message.html")
                            (mock/body {"message" "doesn't matter"
                                        "title" "who cares"})
                            media-cookie))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/index.html"))))))

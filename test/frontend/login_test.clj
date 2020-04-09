(ns frontend.login-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [frontend.handler :refer :all]
            [cheshire.core :refer :all]
            [frontend.make-cookie :refer [extract]])
  (:use clj-http.fake))

(deftest test-login
  (testing "redirect to login"
    (let [response (app (mock/request :get "/index.html"))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/login.html"))))

  (testing "login succeeds"
    (with-fake-routes-in-isolation
      {"http://auth:4007/validate/whoever"
       {:post (fn [request]
                (if (= (extract request "password")
                       "whocares")
                  {:headers {:content-type
                             "application/json"}
                   :body (generate-string
                          {"status" "ok"})}
                  {:headers {:content-type
                             "application/json"}
                   :body (generate-string
                          {"status" "failed"})}))}}
      (let [response (app (-> (mock/request :post "/login")
                              (mock/body {"username" "whoever"
                                          "password" "whocares"})))]
        (is (= (:status response) 302))
        (is (= (get (:headers response) "Location")
               "http://localhost/index.html")))))

  (testing "login fails"
    (with-fake-routes-in-isolation
      {"http://auth:4007/validate/whoever"
       {:post (fn [request]
                {:headers {:content-type
                           "application/json"}
                 :body (generate-string
                        {"status" "failed"})})}}
      (let [response (app (-> (mock/request :post "/login")
                              (mock/body {"username" "whoever"
                                          "password" "whocares"})))]
      (is (= (:status response) 302))
      (is (= (get (:headers response) "Location")
             "http://localhost/login.html")))))

  (testing "login missing password"
    (with-fake-routes-in-isolation
      {"http://auth:4007/validate/whoever"
       {:post (fn [request]
                {:headers {:content-type
                           "application/json"}
                 :body (generate-string
                        {"status" "failed"})})}}
      (let [response (app (-> (mock/request :post "/login")
                              (mock/body {"username" "whoever"})))]
        (is (= (:status response) 302))
        (is (= (get (:headers response) "Location")
               "http://localhost/login.html")))))

  (testing "login followed by logout"
    (with-fake-routes-in-isolation
      {"http://auth:4007/validate/logsingood"
       {:post (fn [request]
                {:headers {:content-type
                           "application/json"}
                 :body (generate-string
                        {"status" "ok"})})}
       "http://identity:4012/user/logsingood"
       (fn [request]
         {:headers {:content-type
                    "application/json"}
          :body (generate-string
                 {:status "ok"
                  :user "logsingood"
                  :role "user"
                  :email "ruguer@gmail.com"})})}
      (let [response (app (-> (mock/request :post "/login")
                              (mock/body {"username" "logsingood"
                                          "password" "everythingworkds"})))
            [_ session-cookie] (re-find #"([^;]*);.*"
                                        (first (get (:headers response)
                                                    "Set-Cookie")))
            response2 (app (-> (mock/request :get "/index.html")
                               (mock/header "Cookie" session-cookie)))
            response3 (app (-> (mock/request :get "/logout")
                               (mock/header "Cookie" session-cookie)))
            response4 (app (-> (mock/request :get "/index.html")
                               (mock/header "Cookie" session-cookie)))]
        (is (= (:status response ) 302))
        (is (= (:status response2) 200))
        (is (= (:status response3) 302))
        (is (= (:status response4) 302))
        (is (= (get (:headers response) "Location")
               "http://localhost/index.html"))
        (is (= (get (:headers response3) "Location")
               "http://localhost/login.html"))
        (is (= (get (:headers response4) "Location")
               "http://localhost/login.html"))
        (is (re-matches #"(?s).*<h1>ReRun TV</h1>.*"
                        (:body response2))))))


    (testing "auth service fails" 
      (with-fake-routes-in-isolation
        {"http://auth:4007/validate/whoever"
         {:post (fn [request]
                  (throw (Exception. "doesn't work")))}}
        (let [response (app (-> (mock/request :post "/login")
                                (mock/body {"username" "whoever"})))]
          (is (= (:status response) 302))
          (is (= (get (:headers response) "Location")
                 "http://localhost/login.html"))))))

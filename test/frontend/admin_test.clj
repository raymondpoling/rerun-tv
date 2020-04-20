(ns frontend.admin-test
  (:require
   [clojure.test :refer [deftest is]]
   [ring.mock.request :as mock]
   [frontend.handler :refer [app]]
   [frontend.util :refer [make-cookie
                          make-response
                          testing-with-log-markers
                          basic-matcher]]
   [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-admin-routes
  (let [admin-cookie (make-cookie "admin")
        media-cookie (make-cookie "media")]
    (testing-with-log-markers
     "user has no access to user-management"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :get "/user-management.html")
                               media-cookie))]
         (is (= (:status response) 302))
         (is (get (:headers response) "Location")
             "http://localhost:4008/login.html"))))
    (testing-with-log-markers
     "user has no access to user"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :post "/user")
                               media-cookie))]
         (is (= (:status response) 302))
         (is (get (:headers response) "Location")
             "http://localhost:4008/login.html"))))
    (testing-with-log-markers
     "user has no access to role"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :post "/role")
                               media-cookie))]
         (is (= (:status response) 302))
         (is (get (:headers response) "Location")
             "http://localhost:4008/login.html"))))
    (testing-with-log-markers
     "admin user has access"
     (with-fake-routes-in-isolation
       {
        "http://identity:4012/role"
        (fn [_] (make-response {:status "ok"
                                :roles ["user","admin","media"]}))
        "http://identity:4012/user"
        (fn [_] (make-response {:status "ok"
                                :users [{
                                         :email "yap@yap.org"
                                         :user "yapper"
                                         :role "user"
                                         }
                                        {
                                         :email "medina@med.com"
                                         :user "medina"
                                         :role "media"
                                         }
                                        {
                                         :email "addie@adder.org"
                                         :user "addie"
                                         :role "admin"
                                         }]}))
        }
       (let [response (app (-> (mock/request :get "/user-management.html")
                               admin-cookie))]
         (is (= (:status response) 200))
         (is (basic-matcher "<option selected=\"selected\">user</option>"
                            (:body response)))
         (is (basic-matcher "<option>admin</option>" (:body response)))
         (is (basic-matcher "<option>media</option>" (:body response)))
         (is (basic-matcher (str
                             "<tr><td>yapper</td>"
                             "<td>yap@yap.org</td>"
                             "<td>user</td></tr>")
                            (:body response)))
         (is (basic-matcher (str
                             "<tr><td>medina</td>"
                             "<td>medina@med.com</td>"
                             "<td>media</td></tr>")
                            (:body response)))
         (is (basic-matcher (str
                             "<tr><td>addie</td>"
                             "<td>addie@adder.org</td>"
                             "<td>admin</td></tr>")
                            (:body response))))))
    (testing-with-log-markers
     "admin adds user"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :post "/user")
                               admin-cookie))]
         (is (= (:status response) 302))
         (is (get (:headers response) "Location")
             "http://localhost:4008/user-management.html"))))
    (testing-with-log-markers
     "user has no access to role"
     (with-fake-routes-in-isolation
       {}
       (let [response (app (-> (mock/request :post "/role")
                               admin-cookie))]
         (is (= (:status response) 302))
         (is (get (:headers response) "Location")
             "http://localhost:4008/user-management.html"))))))

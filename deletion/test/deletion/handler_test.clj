(ns deletion.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [db.db :refer [initialize]]
            [deletion.test-db :refer [create-h2-mem-tables]]
            [deletion.handler :refer [app]]
            [cheshire.core :refer [parse-string generate-string]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]))

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "create a new nomination for a playlist"
    (let [response (app (-> (mock/request :post "/nominate/playlist/a-test")
                            (mock/json-body {:reason "no longer used"
                                              :user "some-media-user"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "fail to duplicate nomination"
    (let [response (app (-> (mock/request :post "/nominate/playlist/a-test")
                            (mock/json-body {:reason "not used"
                                             :user "some-media-user"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response))
             {"status" "failed"
              "message" "Nomination already exists."}))))

  (testing "make a new nomination for a schedule"
    (let [response (app (-> (mock/request :post
                                          "/nominate/schedule/a-schedule")
                            (mock/json-body {:reason "didn't create right"
                                             :user "ms"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "make a new nomination for a series"
    (let [response (app (-> (mock/request :post "/nominate/series/BGATA01")
                            (mock/json-body {:reason "failed upload"
                                             :user "another-media"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "make a new nomination for a season"
    (let [response (app (-> (mock/request :post "/nominate/season/BGATA0103")
                            (mock/json-body {:reason "season shouldn't exist"
                                             :user "another-media"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "make a new nomination for an episode"
    (let [response (app
                    (->
                     (mock/request :post "/nominate/episode/BGATA0102001")
                     (mock/json-body {:reason
                                      "episode was moved to another season"
                                      :user "a-media"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response)) {"status" "ok"}))))

  (testing "list all current nominations"
    (let [response (app (mock/request :get "/nominate"))]
      (is (= (:status response) 200))
      (is (= (:outstanding (parse-string (:body response) true))
             [{:type "playlist", :name "a-test"}
              {:type "schedule", :name "a-schedule"}
              {:type "series", :name "BGATA01"}
              {:type "season", :name "BGATA0103"}
              {:type "episode", :name "BGATA0102001"}]))))

  (testing "reject playlist"
    (let [response (app (-> (mock/request :post "/reject/playlist/a-test")
                            (mock/json-body {:reason "*I* use this!"
                                             :user "admin1"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok"}))))

  (testing "reject schedule"
    (let [response (app (-> (mock/request :post "/reject/schedule/a-schedule")
                            (mock/json-body {:reason
                                             "Another user does use this"
                                             :user "admin2"})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok"}))))

  (testing "cannot reject without nomination"
    (let [response (app (-> (mock/request :post "/reject/playlist/a-test")
                            (mock/json-body {:reason "*I* use this!"
                                             :user "admin1"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response))
             {"status" "failed"
              "message" "No nominating record exists."}))))

  (testing "cannot both create and reject nomination"
    (let [response (app
                    (->
                     (mock/request :post "/reject/episode/BGATA0102001")
                     (mock/json-body {:reason
                                      "episode was moved to another season"
                                      :user "a-media"})))]
      (is (= (:status response) 400))
      (is (= (parse-string (:body response))
             {"status" "failed"
              "message" "Nominator cannot also reject."}))))

  (testing "list all current nominations after rejections"
    (let [response (app (mock/request :get "/nominate"))]
      (is (= (:status response) 200))
      (is (= (:outstanding (parse-string (:body response) true))
             [{:type "series", :name "BGATA01"}
              {:type "season", :name "BGATA0103"}
              {:type "episode", :name "BGATA0102001"}]))))

  (testing "get recent"
    (let [response (app (mock/request :get "/recent"))]
      (is (= (:status response) 200))
      (is (= (:records (parse-string (:body response) true))
             [{:name "BGATA0102001",
               :type "episode",
               :maker "a-media",
               :checker nil,
               :reason "episode was moved to another season",
               :status "NOM"}
              {:name "BGATA0103",
               :type "season",
               :maker "another-media",
               :checker nil,
               :reason "season shouldn't exist",
               :status "NOM"}
              {:name "BGATA01",
               :type "series",
               :maker "another-media",
               :checker nil,
               :reason "failed upload",
               :status "NOM"}
              {:name "a-schedule",
               :type "schedule",
               :maker "ms",
               :checker "admin2",
               :reason "didn't create right\nAnother user does use this",
               :status "REJ"}
              {:name "a-test",
               :type "playlist",
               :maker "some-media-user",
               :checker "admin1",
               :reason "no longer used\n*I* use this!",
               :status "REJ"}]))))

  (testing "delete a series"
    (with-fake-routes-in-isolation
      {"http://meta:4004/catalog-id/BGATA01"
       {:delete
        (fn [_] {:status 200
                 :headers {}
                 :body (generate-string {:status :ok})})}}
    (let [response (app (-> (mock/request :post "/execute/series/BGATA01")
                            (mock/json-body {:user "admin3"
                                             :reason
                                             "Yeah, make sure to fix that."})))]
      (is (= (:status response) 200))
      (is (= (parse-string (:body response))
             {"status" "ok"})))))

  (testing "delete an season"
    (with-fake-routes-in-isolation
      {"http://meta:4004/catalog-id/BGATA0103"
       {:delete
        (fn [_] {:status 200
                 :headers {}
                 :body (generate-string {:status :ok})})}}
      (let [response (app (-> (mock/request :post "/execute/season/BGATA0103")
                              (mock/json-body {:user "admin4"
                                               :reason
                                               "These things happen"})))]
        (is (= (:status response) 200))
        (is (= (parse-string (:body response))
               {"status" "ok"})))))

  (testing "list all current nominations after deletions"
    (let [response (app (mock/request :get "/nominate"))]
      (is (= (:status response) 200))
      (is (= (:outstanding (parse-string (:body response) true))
             [{:type "episode", :name "BGATA0102001"}]))))

  (testing "cannot both create and delete nomination"
    (with-fake-routes-in-isolation
      {"http://meta:4004/catalog-id/BGATA0102001"
       {:delete
        (fn [_] (println "Should not reach this code!")
          (throw (Exception. "Should not be reached.")))}}
      (let [response (app
                      (->
                       (mock/request :post "/execute/episode/BGATA0102001")
                       (mock/json-body {:reason
                                        "episode was moved to another season"
                                        :user "a-media"})))]
        (is (= (:status response) 400))
        (is (= (parse-string (:body response))
               {"status" "failed"
                "message" "Nominator cannot also delete."})))))
  )

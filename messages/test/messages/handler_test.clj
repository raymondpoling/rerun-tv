(ns messages.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [db.db :refer [initialize database]]
            [clojure.java.jdbc :as j]
            [messages.handler :refer :all]
            [messages.test-db :refer [create-h2-mem-tables]]
            [java-time :as jt]
            [cheshire.core :refer :all]))

(def format-string "yyyy-MM-dd HH:mm:ss.SSS")

(deftest test-app
  (initialize)
  (create-h2-mem-tables)
  (testing "create a bunch of logs"
    (let [twenty (range 20)
          response (map #(app (mock/request :post "/"
                        {:form-params { :null nil
                                        :title (str "simple test " %)
                                        :author (str "test-" %)
                                        :information (str (* % (- % 1)))}})) twenty)
          json-response (map #(parse-string (:body %) true) response)]
      (is (= (map :status response) (map (constantly 200) twenty)))
      (is (= (map #(:status %) json-response) (map (constantly "ok") twenty)))))

  (testing "get the last 10 frames"
    (let [ten (range 10)
          response (app (mock/request :get "/" {:query-params {:null :nil
                                                               :start "21"
                                                               :step "10"}}))
          json-responses (:events (parse-string (:body response) true))]
      (is (= (:status response) 200))
      (is (= (map :author json-responses)
             (map #(str "test-" (+ 10 %)) (reverse ten))))
       (is (= (map :information json-responses)
              (map #(str (* (+ 10 %) (+ 9 %))) (reverse ten))))
      (is (= (map :title json-responses)
             (map #(str "simple test " (+ 10 %)) (reverse ten))))))
   (testing "get the last 10 frames without a start"
     (let [ten (range 10)
           response (app (mock/request :get "/" {:query-params {:null :nil
                                                                :step "10"}}))
           json-responses (:events (parse-string (:body response) true))]
       (is (= (:status response) 200))
       (is (= (map :author json-responses)
              (map #(str "test-" (+ 10 %)) (reverse ten))))
        (is (= (map :information json-responses)
               (map #(str (* (+ 10 %) (+ 9 %))) (reverse ten))))
       (is (= (map :title json-responses)
              (map #(str "simple test " (+ 10 %)) (reverse ten))))))
    (testing "try to get older frames"
      (let [ten (range 10)
            response1 (app (mock/request :get "/" {:query-params {:null :nil
                                                                 :step "10"}}))
            json-responses1 (last (:events (parse-string (:body response1) true)))
            last-message (:message_number json-responses1)
           response2 (app (mock/request :get "/" {:query-params {:null :nil
                                                                :start last-message
                                                                :step "10"}}))
            json-responses2 (:events (parse-string (:body response2) true))]
        (is (= (:status response2) 200))
        (is (= (map :author json-responses2)
               (map #(str "test-" %) (reverse ten))))
         (is (= (map :information json-responses2)
                (map #(str (* (- % 1)  %)) (reverse ten))))
        (is (= (map :title json-responses2)
               (map #(str "simple test " %) (reverse ten))))
        (is (not= json-responses1 (last json-responses2)))))
   )

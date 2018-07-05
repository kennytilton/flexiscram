(ns flexiana.handler-test
  (:require [clojure.test :refer :all]
            [cheshire.core :refer :all]
            [ring.mock.request :as mock]
            [flexiana.handler :refer :all]))

(def Response-OK 200)
(def Unprocessable-Entity 422)
(def Not-Found 404)

(defn body->map [response]
  (parse-string (:body response) true))

(deftest test-app
  ;;
  ;; we have implemented an API in which the response will contain
  ;; the queried params and a :result key bound to true or false,
  ;; and in which it is an error to omit :source or :target params.
  ;;
  (testing "main route scramblep true"
    (let [query {:source "booya", :target "yabo"}
          response (app
                     (mock/request :get "/scramblep" query))]
      (prn response)
      (is (= (:status response) Response-OK))
      (is (= (body->map response) (merge query {:result true})))))

  (testing "main route scramblep False"
    (let [query {:source "booya", :target "yabbo"}
          response (app
                     (mock/request :get "/scramblep" query))]
      (is (= (:status response) Response-OK))
      (is (= (body->map response) (merge query {:result false})))))

  (testing "bad request1"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:source "booya"}))]
      (is (= (:status response) Unprocessable-Entity))))

  (testing "bad request1"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:target "yoba"}))]
      (is (= (:status response) Unprocessable-Entity))))

  (testing "bad route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) Not-Found)))))

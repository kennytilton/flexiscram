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

(deftest test-app-mini
  ;;
  ;; we have implemented an API in which the response will contain
  ;; the queried params and a :result key bound to true or false,
  ;; and in which it is an error to omit :source or :target params
  ;; or for them to violate the a-z limitation.
  ;;
  (testing "bad request no target"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:source "booya"}))]
      (prn :response response)
      (is (= (:status response) Response-OK))
      (is (= (:usageError (body->map response)) "Source and target both required.")))))

(deftest test-app
  ;;
  ;; we have implemented an API in which the response will contain
  ;; the queried params and a :result key bound to true or false,
  ;; and in which it is an error to omit :source or :target params
  ;; or for them to violate the a-z limitation.
  ;;
  (testing "main route scramblep true"
    (let [query {:source "booya", :target "yabo"}
          response (app
                     (mock/request :get "/scramblep" query))]
      (is (= (:status response) Response-OK))
      (is (= (body->map response) (merge query {:result true})))))

  (testing "main route scramblep False"
    (let [query {:source "booya", :target "yabbo"}
          response (app
                     (mock/request :get "/scramblep" query))]
      (is (= (:status response) Response-OK))
      (is (= (body->map response) (merge query {:result false})))))

  (testing "bad request no target"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:source "booya"}))]
      (is (= (:status response) Response-OK))
      (is (= (:usageError (body->map response)) "Source and target both required."))))

  (testing "bad request no source"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:target "yoba"}))]
      (is (= (:status response) Response-OK))
      (is (= (:usageError (body->map response)) "Source and target both required."))))

  (testing "bad non a-z source"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:source "b o o y a"
                        :target "yoba"}))]
      (is (= (:status response) Response-OK))
      (is (= (:usageError (body->map response)) "Source and target must both be lowercase a to z with no spaces."))))

  (testing "bad non a-z target"
    (let [response (app
                     (mock/request :get "/scramblep"
                       {:source "booya"
                        :target "b y a"}))]
      (is (= (:status response) Response-OK))
      (is (= (:usageError (body->map response)) "Source and target must both be lowercase a to z with no spaces."))))

  (testing "bad route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) Not-Found)))))

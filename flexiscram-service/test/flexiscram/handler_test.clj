(ns flexiscram.handler-test
  (:require [clojure.test :refer :all]
            [cheshire.core :refer :all]
            [ring.mock.request :as mock]
            [flexiscram.handler :refer :all]))

(def Response-OK 200)
(def Unprocessable-Entity 422)
(def Not-Found 404)

(defn body->map [response]
  (parse-string (:body response) true))

(defn pic9A [n]
  (let [scale (Math/floor (/ (Math/log10 n) 3))]
    (clojure.pprint/cl-format
      nil "~d~v[~;K~;M~;B~;T~;G~]"
      (/ n (Math/pow 10 (* scale 3)))
      scale)))

(deftest test-app
  ;;
  ;; we have implemented an API in which the response will contain
  ;; the queried params and either:
  ;;    status 200 and a :result key bound to true or false, or
  ;;    status 422 and a :usageError.
  ;;
  ;; Usage errors can arise because it is an error to omit :source or :target params
  ;; or for either of them to violate the a-z limitation.
  ;;
  (testing "main route scramblep true"
    (let [query {:source "booya", :target "yabo"}
          response (app
                     (mock/request :get "/scramblep" query))]
      (is (= (:status response) Response-OK))
      (prn :bmap (body->map response) response)
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
      (is (= (:status response) Unprocessable-Entity))
      (is (= (:usageError (body->map response)) "Source and target both required."))))

  (testing "bad request no source"
    (let [response (app
                     (mock/request :get "/scramblep"
                                   {:target "yoba"}))]
      (is (= (:status response) Unprocessable-Entity))
      (is (= (:usageError (body->map response)) "Source and target both required."))))

  (testing "bad non a-z source"
    (let [response (app
                     (mock/request :get "/scramblep"
                                   {:source "b o o y a"
                                    :target "yoba"}))]
      (is (= (:status response) Unprocessable-Entity))
      (is (= (:usageError (body->map response)) "Source and target must both be lowercase a to z with no spaces."))))

  (testing "bad non a-z target"
    (let [response (app
                     (mock/request :get "/scramblep"
                                   {:source "booya"
                                    :target "b y a"}))]
      (is (= (:status response) Unprocessable-Entity))
      (is (= (:usageError (body->map response)) "Source and target must both be lowercase a to z with no spaces."))))

  (testing "bad route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) Not-Found)))))

(def ^:private browsers
  {"aaa-1"   {:br-id         "aaa-1"
              :history       ["aaa.com" "https://aaa.com"]
              :history-index 1
              :timing        [:latest]
              :timestamp     42}
   "aaa-2"   {:br-id         "aaa-2"
              :history       ["https://aaa.com"]
              :history-index 0
              :timing        [:earliest]
              :timestamp     0}

   "bbb-1"   {:br-id         "bbb-1"
              :history       ["bbb.com" "https://bbb.com"]
              :history-index 0
              :timing        [:earliest]
              :timestamp     42}
   "bbb-mid" {:br-id         "bbb-mid"
              :history       ["http://bbb.com" "bbb.com" "https://bbb.com"]
              :history-index 1
              :timing        [:middle]
              :timestamp     67}
   "bbb-2"   {:br-id         "bbb-2"
              :history       ["https://bbb.com" "bbb.com"]
              :history-index 1
              :timing        [:latest]
              :timestamp     99}

   "uniq-0"  {:br-id         "uniq-0"
              :history       ["https://uniq.com" "uniq.com"]
              :history-index 0
              :timing        [:earliest :latest]
              :timestamp     42}})

(defn get-current-url [{:keys [history history-index]}]
  (when (and history-index history)
    (nth history history-index)))

(defn distinct-by-group
  "First group a map's values by property <grouper>,
  then select the optimal within each group as determined
  by property <optimizing> taking the first after
   ordering by <ordering>, which defaults to greater-than."

  ([m grouper optimizing ordering]
   (into {}
         (map (fn [[dup browsers]]
                (first (sort-by #(optimizing (second %))
                                ordering
                                browsers)))
              (group-by
                #(grouper (second %))
                browsers))))

  ([m grouper optimizing]
   (distinct-by-group m grouper optimizing >)))


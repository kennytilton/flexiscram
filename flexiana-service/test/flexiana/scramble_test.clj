(ns flexiana.scramble-test
  (:require [clojure.test :refer :all]
            [flexiana.handler :refer :all]
            [flexiana.handler-test :refer [body->map]]
            [flexiana.scramble :refer :all]
            [ring.mock.request :as mock]))

;; note that we are assuming with the spec that data is all
;; lowercase alpha, so no testing of strings with spaces

(deftest scramble-test
  (testing "empty strings"
    (is (scramble? "" ""))
    (is (scramble? "technically" ""))
    (is (= false (scramble? "" "unlikely"))))

  (testing "positive: exact"
    (is (scramble? "world" "world"))
    (is (scramble? "dlorw" "world")))

  (testing "positive: extra source"
    (is (scramble? "rekqodlw" "world"))
    (is (scramble? "cedewaraaossoqqyt" "codewars")))

  (testing "negative: missing letter"
    (is (= false (scramble? "katas" "steak"))))

  (testing "negative: insufficient count"
    (is (= false (scramble? "kaetasjjj" "steeak")))))

(defn make-random-string
  ([ct] (make-random-string ct "abcdefghijklmnopqrstuvwxyz"))
  ([ct source]
   (let [out (char-array ct)]
     (dotimes [ox ct]
       (aset out ox (nth source (rand-int (count source)))))
     (apply str (seq out)))))

(defn now [] (System/currentTimeMillis))

(defonce huge-m (make-random-string 1000000 "abcdefghijklmnopqrstuvwxy")) ;; no "z"!

;(time (do (frequencies huge-m) nil))
;(time (do (alpha-freq huge-m) nil))
;(time (scramble?-alpha-freq huge-m "yobaxokdmba"))

(deftest speed-check
  (testing "under 500ms for miss against million char source"
    (let [start (now)]
      (is (not (scramble? huge-m "yobaxozkdmba"))) ;; "z"!
      (prn :elapsed-fail (- (now) start))
      (is (> 500 (- (now) start)))))
  (testing "under 5ms for hit against million char source"
    (let [start (now)]
      (is (scramble? huge-m "yobaxokdmba"))
      (prn :elapsed-yes (- (now) start))
      (is (> 5 (- (now) start))))))

(deftest test-scramblep-endpoint
  (testing "main route"
    (let [response (app (mock/request :get "/scramblep?source=cedewaraaossoqqyt&target=codewars"))]
      (is (= (:status response) 200))
      (is (= true (:result (body->map response)))))))
(ns flexiscram.scramble
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [ring.util.response :refer [response]]))

(defn rack-sufficient?
  "'Rack' in the sense of a Scrabble rack.

  Answers whether the goal can be built from the source (rack).
  "[source goal]

  (loop [[sc & srest] source
         needed (transient (frequencies goal))]
    (cond
      (zero? (count needed)) true

      (nil? sc) false

      :default (recur srest
                 (if-let [gct (get needed sc)]
                   ;; note that gct, if found, must be positive because
                   ;; counts start > 0 and are dissoc'ed when they reach zero
                   (let [new (dec gct)]
                     (if (zero? new)
                       (dissoc! needed sc)
                       (assoc! needed sc new)))
                   needed)))))

(defn scramble?
  "Returns whether the goal string can be constructed
  from characters in the source string, without re-use.

  Empty strings are considered constructable."

  ; Developed in contemplation of a huge source string
  ; where computing full frequencies is expensive. (Simpler
  ; approach took frequencies of both strings and compared
  ; counts.)
  ;
  ; Instead we walk the source string checking after each hit if
  ; all counts have been satisfied and stopping if so.
  ;
  ; The "all satisfied" check is simply that the "needed" map
  ; is empty. This works because, once the needed count for a
  ; letter drops to zero and we know that letter's usage can
  ; be satisfied by the source, we drop that letter from the
  ; map entirely, to short-circuit further checking of that letter.
  ;

  [source goal]

  (or
    ;; we consider the null case of an empty target to be satisfiable,
    ;; though the service itself rejects blank source and/or target
    ;; as a usage error.
    (str/blank? goal)

    ;; actually asseess the source and target
    (and
      (<= (count goal) (count source))
      (rack-sufficient? source goal))

    ;; force false as function result. Handy during development, anyway.
    false))

;;; --- handle-scramble? ---------------------------------------------
;;; The corresponding route handler for assessing scramble?-ness

(def CORS-HEADERS {"Content-Type"                 "application/json"
                   "Access-Control-Allow-Headers" "Content-Type"
                   "Access-Control-Allow-Origin"  "*"})

(defn handle-scramble? [req]
  (try
    (let [{:keys [source target]} (:params req)]
      {:status  200
       :headers CORS-HEADERS
       :body    (merge
                  (select-keys (:params req) [:source :target])
                  (cond
                    (some str/blank? [source target])
                    {:usageError "Source and target both required."}

                    (not-every? #(re-matches #"^[a-z]+$" %) [source target])
                    {:usageError "Source and target must both be lowercase a to z with no spaces."}

                    :default
                    (let [result (scramble? source target)]
                      ;; todo log traffic usefully
                      {:result result})))})

    ;; todo log/report error usefully
    (catch Exception e
      {:status  500
       :headers {"Content-Type" "text/html"}
       :body    "Something 500 happened."})))

;;; --- archive -----------------------------------------------
;;; stuff from the development phase that did not make
;;; the team but will be preserved for future generations.

(defn alpha-freq
  "Return a frequencies map of input <word> characters
  in the form of a vector of counts indexed by taking
  the difference of (int <char>) and (int \\a)."
  [word]

  ; We realized early on that we did not want to take the full frequency
  ; map of very large source since that cost (on the order of 100ms for
  ; a million character string) would overwhelm the cost of walking through
  ; the source character by character and stopping if the target were
  ; proven attainable, but it became a challenge to beat Clojure's generic
  ; frequencies by leveraging knowing all chars would be a-z (promised by
  ; the spec, enforced by the service).
  ;
  ; We achieved 10-20% improvement.

  (let [ascii-base (int \a)]
    (persistent!
      (reduce (fn [counts c]
                (let [offset (- (int c) ascii-base)]
                  (assoc! counts offset (inc (get counts offset)))))
        (transient (vec (repeat 26 0)))
        word))))

;;; --- solutions developed before addressing huge sources ------------------

(defn scramble?-naive [a b]
  (or
    (str/blank? b)
    (let [af (alpha-freq a)
          bf (alpha-freq b)]
      (and
        (set/subset? (set (keys bf)) (set (keys af)))
        (every? (fn [[b ct]]
                  (<= ct (get af b))) bf)))))

(defn scramble?-alpha-freq [source goal]
  (or
    (str/blank? goal)
    (let [s-frq (alpha-freq source)
          g-frq (alpha-freq goal)]
      (every? (comp not neg?)
        (map - s-frq g-frq)))))
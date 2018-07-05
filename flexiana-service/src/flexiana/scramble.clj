(ns flexiana.scramble
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [ring.util.response :refer [response]]))

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
    ; The "all satisfied" check is simply for an empty "needed map'.
    ; Satisfied letter counts drop that letter from the "needed"
    ; map to short-circuit further checking of that letter.
    ;

    [source goal]

    (or (str/blank? goal)
      (and
        ;;(<= (count goal) (count source))
        (let [needed (transient (frequencies goal))]
          (some (fn [c]
                  (when-let [gct (get needed c)]
                    ;; note that gct, if found, is positive because counts start > 0
                    ;; and are dissoc'ed from needed when they reach zero
                    (let [new (dec gct)]
                      (if (zero? new)
                        (do
                          (dissoc! needed dissoc c)
                          (zero? (count needed)))
                        (do
                          (assoc! needed c new)
                          nil)))))
            source)))
      false))

(defn handle-scramble? [req]
  (prn :handle-scramble?-sees req)
  (try
    (let [{:keys [params]} req]
      ;;(throw (Exception. "test"))
      (cond
        (not-every? #(contains? params %) [:source :target])
        {:status  422
         :headers {"Content-Type" "application/json"}
         :body    (str {:usageError "Required params aource and/or target not provided."})}

        :default
        (let [{:keys [source target]} params
              result (scramble? source target)]
          ;; todo log traffic usefully
          #_
          (response (merge
                      (select-keys params [:source :target])
                      {:result result}))

          {:status  200
           :headers {"Content-Type" "application/json"
                     "Access-Control-Allow-Headers" "Content-Type"
                     "Access-Control-Allow-Origin" "*"}
           :body                                            ;; (str "<h1>Booya:" result)
                    (merge
                      (select-keys params [:source :target])
                      {:result result})})))

    ;; todo log/report error usefully
    (catch Exception e
      {:status  500
       :headers {"Content-Type" "text/html"}
       :body    "Something 500 happened."})))

;;; --- archive -----------------------------------------------


(defn alpha-freq [word]
  (let [ascii-base (int \a)]
    (persistent!
      (reduce (fn [counts c]
                (let [offset (- (int c) ascii-base)]
                  (assoc! counts offset (inc (get counts offset)))))
        (transient (into [] (repeat 26 0)))
        word))))

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
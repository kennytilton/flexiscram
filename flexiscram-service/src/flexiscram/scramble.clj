(ns flexiscram.scramble
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [ring.util.response :refer [response]]))

(defn rack-sufficient?
  "'Rack' in the sense of a Scrabble rack.

  Answers whether the goal can be built from the source (rack).
  "
  [source goal]

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
    (let [{:keys [source target]} (:params req)
          build-resp (fn [status body-ext]
                       {:status  status
                        :headers CORS-HEADERS
                        :body    (merge
                                   {:source source
                                    :target target}
                                   body-ext)})
          usage-error (fn [msg]
                        (build-resp 422 {:usageError msg}))]
      (cond
        (some str/blank? [source target])
        (usage-error "Source and target both required.")

        (not-every? #(re-matches #"^[a-z]+$" %) [source target])
        (usage-error "Source and target must both be lowercase a to z with no spaces.")

        :default
        (build-resp 200 {:result (scramble? source target)})))

    ;; todo log/report error usefully
    (catch Exception e
      {:status  500
       :headers {"Content-Type"                 "text/html"
                 "Access-Control-Allow-Headers" "Content-Type"
                 "Access-Control-Allow-Origin"  "*"}
       :body    "Something 500 happened."})))
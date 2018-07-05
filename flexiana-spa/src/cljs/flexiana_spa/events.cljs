(ns flexiana-spa.events
  (:require
    [clojure.string :as str]
    [cljs.pprint :as pp]

    [re-frame.core :as rfr]
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]

    [flexiana-spa.db :as db]))

(rfr/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(rfr/reg-event-db
  :typing

  ;; we want to tailor the helpful prompt based on whether
  ;; they have started changing a field. One good example is
  ;; clearing a prior error message once they have begun to
  ; fix it.

  (fn [db [_ prop]]
    (-> db
      (assoc :scramble? :undecided)
      (assoc-in [prop :error] nil))))

(rfr/reg-event-fx
  :term-set
  ;; works for both source and target terms

  (fn [{:keys [db]} [_ prop term]]
    (let [prop-db-defaults {prop       {:term  term
                                        :error nil}
                            :inputs-ready? false
                            :scramble? :undecided}]
      (cond
        (str/blank? term)
        {:db (merge db prop-db-defaults)}

        (re-matches #"^[a-z]+$" term)
        {:db         (merge db
                       prop-db-defaults)
         :dispatch-n (list
                       [:term-history-extend prop term]
                       [:decide-inputs-ready?])}

        :default {:db (merge db
                        prop-db-defaults
                        {prop
                         {:error (str (str/capitalize (name prop))
                                   " must be lowercase letters a to z.")}})}))))

(rfr/reg-event-db
  :term-history-extend
  ;; just a small U/X nicety for fun

  (fn [db [_ prop term]]
    ;; histories are initialized to be sets, so no
    ;; need to check for duplicates
    (update-in db [:history prop] conj term)))

(def HOST_NAME "http://localhost:3000")                     ;; todo lose the hardcode
(def scramblep-uri-template "~a/scramblep?source=~a&target=~a")

(defn gen-scramblep-uri
  "Given source and target terms, build a URI suitable for disoatch."
  [source target]

  (pp/cl-format nil scramblep-uri-template HOST_NAME source target))

(rfr/reg-event-db
  :decide-inputs-ready?
  (fn [db [_]]
    (assoc db :inputs-ready?
              true #_ (every? #(and (not (str/blank? (get-in db [% :term])))
                            (not (get-in db [% :error])))
                [:source :target]))))

(rfr/reg-event-fx
  :scramble?
  (fn [{:keys [db]} _]
    (let [source (get-in db [:source :term])
          target (get-in db [:target :term])]
      (if (some str/blank? [source target])
        {:db db}
        (let [uri (gen-scramblep-uri source target)]
          {:db         (assoc db :inputs-ready? false)
           :http-xhrio {:method          :get
                        :uri             uri
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success      [:scramble-check]
                        :on-failure      [:scramble-http-failure]}})))))


(rfr/reg-event-db
  :scramble-check
  (fn [db [_ result]]
    (assoc db :scramble? (:result result)
              :lookup-error nil)))

(rfr/reg-event-db
  :scramble-http-failure
  (fn [db [_ result]]
    (prn :hfail result)
    (assoc db :lookup-error (:status-text result))))

#_(defn scramble? [source goal]
    (and
      (<= (count goal) (count source))
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
          source))))


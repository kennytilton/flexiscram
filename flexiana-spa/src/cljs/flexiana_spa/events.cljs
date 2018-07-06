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
    (assoc db :scramble? :undecided
              :user-error nil
              :lookup-error nil)))

(rfr/reg-event-fx
  :term-set
  ;; works for both source and target terms

  (fn [{:keys [db]} [_ prop term]]
    {:db       (assoc db prop term
                         :scramble? :undecided)
     :dispatch [:term-history-extend prop term]}))

(rfr/reg-event-db
  :term-history-extend

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

(rfr/reg-event-fx
  :scramble?
  (fn [{:keys [db]} _]
    (let [{:keys [source target]} db]
      (cond
        (some str/blank? [source target])
        {:db (assoc db :user-error "Please provide both source and target for us to check your work.")}

        :default
        (let [uri (gen-scramblep-uri source target)]
          {:db         db                                   ;; need this? todo
           :http-xhrio {:method          :get
                        :uri             uri
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success      [:scramble-check]
                        :on-failure      [:scramble-http-failure]}})))))

(rfr/reg-event-db
  :scramble-check
  (fn [db [_ result]]
    (if-let [ue (:usageError result)]
      (assoc db :user-error ue
                :lookup-error nil)
      (assoc db :scramble? (if (:result result) :ok :ng)
              :lookup-error nil))))

(rfr/reg-event-db
  :scramble-http-failure
  (fn [db [_ result]]
    (assoc db :usage-error nil
              :lookup-error (:status-text result))))
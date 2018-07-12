(ns flexiscram-spa.events
  (:require
    [clojure.string :as str]
    [cljs.pprint :as pp]

    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]

    [flexiscram-spa.db :as db]))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :typing

  ;; we want to clear prior user communications in
  ;; a timely fashion, such as an error message once
  ;; they have begun to fix it.

  (fn [db [_ prop]]
    (assoc db :scramble? :undecided
              :user-error nil
              :lookup-error nil)))

(reg-event-fx
  :term-set

  (fn [{:keys [db]} [_ prop term]]
    {:db       (assoc db prop term
                         :scramble? :undecided)
     :dispatch [:term-history-extend prop term]}))

(reg-event-db
  :term-history-extend

  (fn [db [_ prop term]]
    ;; histories are initialized to be sets, so no
    ;; need to check for duplicates
    (update-in db [:history prop] conj term)))

(def HOST_NAME "http://localhost:3000")
(def scramblep-uri-template "~a/scramblep?source=~a&target=~a")

(defn gen-scramblep-uri
  "Given source and target terms, build a URI suitable for disoatch."
  [source target]

  (pp/cl-format nil scramblep-uri-template HOST_NAME source target))

(reg-event-fx
  :scramble?
  (fn [{:keys [db]} _]
    (let [{:keys [source target]} db]
      (cond
        (some str/blank? [source target])
        {:db (assoc db :user-error "Please provide both source and target for us to check your work.")}

        :default
        (let [uri (gen-scramblep-uri source target)]
          {:db         db
           :http-xhrio {:method          :get
                        :uri             uri
                        :response-format (ajax/ring-response-format)
                        ;(ajax/json-response-format {:keywords? true})
                        :on-success      [:scramble-check]
                        :on-failure      [:scramble-http-failure]}})))))

(reg-event-db
  :scramble-check
  (fn [db [_ result]]
    (prn :raw-result result)
    (if-let [ue (:usageError result)]
      (assoc db :user-error ue
                :lookup-error nil)
      (assoc db :scramble? (if (:result result) :ok :ng)
                :lookup-error nil))))

(reg-event-db
  :scramble-http-failure
  (fn [db [_ result]]
    (prn :fail-raw result)
    (assoc db :usage-error nil
              :lookup-error (:status-text result))))
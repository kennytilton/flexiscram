(ns flexiana-spa.subs
  (:require
   [re-frame.core :as rfr]))

(rfr/reg-sub :prop-term
  (fn [db [_ prop]]
    (get-in db [prop :term])))

(rfr/reg-sub :prop-history
  (fn [db [_ prop]]
    (get-in db [:history prop])))

(rfr/reg-sub :prop-error
  (fn [db [_ prop]]
    (get-in db [prop :error])))

(rfr/reg-sub
  :scramble?
  (fn [db]
    (:scramble? db)))

(rfr/reg-sub
  :inputs-ready?
  (fn [db]
    (:inputs-ready? db)))

(rfr/reg-sub
  :lookup-error
  (fn [db]
    (:lookup-error db)))
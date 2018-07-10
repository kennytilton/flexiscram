(ns flexiscram-spa.subs
  (:require
    [re-frame.core :as rfr]))

(rfr/reg-sub :prop-term
             (fn [db [_ prop]]
               (prop db)))

(rfr/reg-sub :prop-history
             (fn [db [_ prop]]
               (get-in db [:history prop])))

(rfr/reg-sub
  :scramble?
  (fn [db _]
    (:scramble? db)))

(rfr/reg-sub
  :user-error
  (fn [db _]
    (:user-error db)))

(rfr/reg-sub
  :lookup-error
  (fn [db _]
    (:lookup-error db)))
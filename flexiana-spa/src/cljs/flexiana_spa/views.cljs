(ns flexiana-spa.views
  (:require
    [goog.string :as gs]
    [re-frame.core :as rfr]
    [flexiana-spa.subs :as subs]
    [clojure.string :as str]
    [cljs.pprint :as pp]))

;; --- browser utils -------------------------------

(defn unesc
  "React does not like things like &trade;, aka \"entity\".

  (unesc <entity>) to get <entity> past React escaping."
  [entity]
  (gs/unescapeEntities entity))

(defn target-val [e]
  (.-value (.-target e)))

;; --- re-frame utils ------------------------------

(def <sub (comp deref rfr/subscribe))
(def >evt rfr/dispatch)

(defn mk-flex-word [prop autofocus?]
  (let [prop-cap (str/capitalize (name prop))]
    (prn :pcap prop-cap)

    (fn [prop]
      (prn :prop prop)
      [:div {:style {:color       "#000"
                     :margin      "0 9px 0 0"}}

       [:label {:for   prop
                :style {:display :inline-block
                        :min-width "96px"}}
        prop-cap]
       [:input {:id           prop
                :auto-focus   autofocus?

                :style        {:width     "10em"
                               :font-size "1em"
                               :height    "1em"}

                :placeholder  (str prop-cap " term")

                :defaultValue (<sub [:prop-term prop])

                :list         (str prop-cap "-datalist")

                :on-key-press #(when (= "Enter" (js->clj (.-key %)))
                                 (>evt [:term-set prop (str/trim (target-val %))]))

                :on-change    #(>evt [:typing prop])

                :on-blur      #(>evt [:term-set prop (str/trim (target-val %))])

                :on-focus     #(.setSelectionRange (.-target %) 0 999)}]

       [:datalist {:id (str prop-cap "-datalist")}
        (map (fn [h]
               ^{:key h} [:option {:value h}])
          (<sub [:prop-history prop]))]])))

(defn user-communication []
  (fn []
    [:p (let [ue (<sub [:user-error])
              e (<sub [:lookup-error])
              r (<sub [:scramble?])]

          (cond
            ue ue
            e (str "Ugh. FlexiScram authority is unavailable: " e)

            r (case r
                :undecided ""
                :ok "You win!"
                :ng "Bad luck!")

            :default ""))]))

(defn scramblep-check-button []
  ;; todo check for missing param and reject
  (fn []
    [:button {:style {:font-size "1em"
                      :margin "12px 0 0 24px"}
              :on-click #(>evt [:scramble?])}
     "Check My Work"]))

(defn main-panel []
  (fn []
    [:div {:style {:font-size "1.5em"
                   :padding   "48px"}}
     [:h1 "FlexiScram" (unesc "&trade;")]
     [:p "Your mission: enter two purely lowercase alpha terms such that the target
       can be built from the source without reusing the same occurrence. Think Scrabble rack and playable word."]
     [mk-flex-word :source true]
     [mk-flex-word :target]
     [scramblep-check-button]
     [user-communication]]))



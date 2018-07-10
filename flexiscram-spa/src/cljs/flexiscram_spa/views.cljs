(ns flexiscram-spa.views
  (:require
    [goog.string :as gs]
    [re-frame.core :as rfr]
    [flexiscram-spa.subs :as subs]
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

;; --- the components ------------------------------

(defn mk-flex-word [prop autofocus?]
  (let [prop-cap (str/capitalize (name prop))]
    (fn [prop]
      [:div {:style {:color       "#000"
                     :margin      "9px 0 0 0"}}

       [:label {:for   prop
                :style {:display :inline-block
                        :min-width "96px"}}
        prop-cap]
       [:input {:id           prop
                :style        {:width     "10em"
                               :font-size "1em"
                               :height    "1em"}

                :auto-focus   autofocus?
                :placeholder  "letters a-z"
                :defaultValue "" ;; handy during development (<sub [:prop-term prop])
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
    [:p (or
          (<sub [:user-error])
          (when-let [e (<sub [:lookup-error])]
            (str "The FlexiScram authority is unavailable: " e))
          (case (<sub [:scramble?])
            :undecided ""
            :ok "You win!"
            :ng "Bad luck!"
            ;; falling thru would actually signify a bug
            ""))]))

(defn scramblep-check-button []
  (fn []
    [:button {:style {:font-size "1em"
                      :margin "12px 0 0 24px"}
              :on-click #(>evt [:scramble?])}
     "Check My Work"]))

;; -- the app itself --------------------------------------

(defn main-panel []
  (fn []
    [:div {:style {:font-size "1.5em"
                   :padding   "48px"}}
     [:h1 "FlexiScram" (unesc "&trade;")]
     [:p "Your mission: enter two purely lowercase alpha terms such that the target
       can be built from the source without reusing the same occurrence. Think Scrabble rack and non-existent word."]
     [mk-flex-word :source true]
     [mk-flex-word :target]
     [scramblep-check-button]
     [user-communication]]))
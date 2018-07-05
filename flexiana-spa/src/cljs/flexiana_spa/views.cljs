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
    (fn [prop]
      [:div {:style {:color       "#000"
                     :margin      "0 9px 0 0"
                     ;:display     "flex"
                     ;:flex-wrap   "wrap"
                     ;:align-items "center"
                     }}


       [:label {:for   prop
                :style {:width "96px"
                        :color (if (<sub [:prop-error prop])
                                 "red" "black")}}
        (str/capitalize (name prop))]
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
          (<sub [:prop-history prop]))]
       [:p {:style {:min-height  "1.5em"
                    :display     "block"
                    :margin-left "24px"}}
        (when-let [error (<sub [:prop-error prop])]
          (do (prn :errrr error)
              (str "Hmmm... " error))
          )]])))

(defn user-communication []
  (fn []
    [:p (let [g (<sub [:prop-term :target])
              ge (<sub [:prop-error :target])
              s (<sub [:prop-term :source])
              se (<sub [:prop-error :source])
              e (<sub [:lookup-error])
              r (<sub [:scramble?])]

          (cond
            e (str "Ugh. FlexiScram authority is unavailable: " e)

            ;se "Ok, let's fix that source"
            ;(str/blank? s) "First we need a source rack..."

            ;ge "Gotta fix that target syntax!"
            ;(str/blank? g) "Great source. Now try to find a word in there..."

            r (case r
                :undecided "..."
                true "You win!"
                "Bad luck!")

            :default "nada"
            #_
            (case r
              :undecided "Take your time. Hit 'Enter' when you are sure..."
              true "You win!"
              "Bad luck!")))]))

(defn scramblep-check-button []
  (fn []
    (when true ;; (<sub [:inputs-ready?])

      [:button {:style {:font-size "1em"
                        :margin "12px 0 0 24px"}
                ;; :disabled (not (<sub [:inputs-ready?]))
                :on-click #(>evt [:scramble?])}
       "Check My Work"])))

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



(ns swiss-maker.views
  (:require
   [re-frame.core :as re-frame]
   [swiss-maker.subs :as subs]
   ["@material-ui/core" :as mui]
   [swiss-maker.components.modal :refer [modal]]
   [swiss-maker.components.form-group :refer [form-group]]
   [reagent.core :as r]
   [swiss-maker.events :as events]))


;; home
(defn create-tournament []
  (let [initial-values {:tournament-name "ff"
                        :number-of-players 0
                        :number-of-rounds 0}
        values (r/atom initial-values)
        open-modal (fn [{:keys [modal-name tournament]}]
                     (re-frame/dispatch [::events/open-modal modal-name])
                     (reset! values tournament))]
    (fn []
      [:div
       [:> mui/Button {:variant "contained"
                       :color "secondary"
                       :on-click #(open-modal {:modal-name :create-tournament
                                               :tournament @values})}
        "Create tournament!"]
       [modal {:modal-name :create-tournament
               :dialog-title "Create tournament"
               :dialog-header ""
               :body [:div [:form {:no-validate true}
                            [form-group {:id :tournament-name
                                         :label "Tournament name"
                                         :type "text"
                                         :values values}]
                            [form-group {:id :number-of-players
                                         :label "Number of players"
                                         :type "number"
                                         :values values}]
                            [form-group {:id :number-of-rounds
                                         :label "Number of rounds"
                                         :type "number"
                                         :values values}]
                            ]]
               :dialog-actions [:<>
                                [:> mui/Button
                                 {:on-click #(re-frame/dispatch [::events/close-modal])}
                                 "Close"]
                                ;; dispatch create tournament event here
                                [:> mui/Button  "Save"]]}]]
      )))

(defn home-panel []
  (let [tourneys @(re-frame/subscribe [::subs/tournaments])]
    (fn []
      [:<>
       [:> mui/Typography {:variant "h5"} "Active tournaments:"]
       [:> mui/List {:component "nav" :aria-label "tournament list"}
        (for [[tourney-name _] tourneys]
          ^{:key tourney-name}
          [:> mui/ListItemText {:primary tourney-name}])]
       [create-tournament]])))

  ;; about

  (defn about-panel []
    [:div
     [:h1 "This is the About Page."]

     [:div
      [:a {:href "#/"}
       "go to Home Page"]]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []

  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:<>
     [:> mui/Grid {:container true :justify "center"}
      [:> mui/Grid {:item true}
       [show-panel @active-panel]]]
     ]))

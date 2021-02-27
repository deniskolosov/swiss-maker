(ns swiss-maker.views
  (:require ["@material-ui/core" :as mui]
            [clojure.string :as str]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [swiss-maker.components.form-group :refer [form-group]]
            [swiss-maker.components.modal :refer [modal]]
            [swiss-maker.events :as events]
            [swiss-maker.subs :as subs]))

;; home
(defn create-tournament []
  (let [initial-values {:tournament-name ""
                        :number-of-rounds 0}
        values (r/atom initial-values)
        open-modal (fn [{:keys [modal-name tournament]}]
                     (re-frame/dispatch [::events/open-modal modal-name])
                     (reset! values tournament))
        save (fn [event {:keys [tournament-name num-of-rounds]}]
               (.preventDefault event)
               (re-frame/dispatch [::events/upsert-tournament {:tournament-name (str/trim tournament-name)
                                                               :num-of-rounds (js/parseInt num-of-rounds)}])
               (reset! values initial-values))]
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
               :body [:div [:form {:no-validate true
                                   :on-submit #(save % @values)}
                            [:> mui/Grid {:container true
                                          :direction "column"
                                          :align-items "center"
                                          :justify "center"}
                             [form-group {:id :tournament-name
                                          :label "Tournament name"
                                          :type "text"
                                          :values values}]
                             [form-group {:id :number-of-rounds
                                          :label "Number of rounds"
                                          :type "number"
                                          :values values}]]]]
               :dialog-actions [:<>
                                [:> mui/Button
                                 {:on-click #(re-frame/dispatch [::events/close-modal])}
                                 "Close"]
                                ;; dispatch create tournament event here
                                [:> mui/Button
                                 {:on-click #(save % @values)}
                                 "Add tournament"]]}]]
      )))

(defn home-panel []
  (let [tourneys (re-frame/subscribe [::subs/tournaments])]
    (fn []
      [:<>
       [:> mui/Typography {:variant "h5"} "Active tournaments:"]
       [:> mui/List {:component "nav" :aria-label "tournament list"}
        (for [[tourney-id tournament] @tourneys]
              ^{:key tourney-id}
              [:> mui/ListItemText {:primary (:tournament-name tournament)}])
        [create-tournament]]])))

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

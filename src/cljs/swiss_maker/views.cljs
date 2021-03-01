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
(defn tournament-editor []
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
      [:> mui/Box {:mt 5}
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
                                 "Add tournament"]]}]])))

;; todo subscribe to active player instead of passing values


(defn home-panel []
  (let [tourneys (re-frame/subscribe [::subs/tournaments])]
    (fn []
      [:<>
       [:> mui/Typography {:variant "h5"} "Active tournaments:"]
       (for [[tourney-id tournament] @tourneys]
         ^{:key tourney-id}
         [:a {:href (str "#tournaments/" (name tourney-id))}
          [:> mui/Card {:variant "outlined"
                        :class "editable"}
           [:> mui/CardContent
            [:> mui/Grid {:container true :direction "row"}
             [:> mui/Typography
              {:variant "body1"} (:tournament-name tournament)]]]]])

       [tournament-editor]])))

;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:href "#/"}
     "go to Home Page"]]])


(defn player-row [values {:keys [id player-name rating score] :as player}]
  (let [open-modal (fn [{:keys [modal-name]}]
                     (re-frame/dispatch [::events/open-modal modal-name])
                     (re-frame/dispatch [::events/set-active-player id])
                     (reset! values player))]
      [:> mui/TableRow
       [:> mui/TableCell player-name]
       [:> mui/TableCell rating]
       [:> mui/TableCell score]
       [:> mui/TableCell [:> mui/Button {:on-click #(open-modal
                                                     {:modal-name :edit-player})} "Edit"]]]))

(defn tournament-panel []
  (let [active-tournament (re-frame/subscribe [::subs/tournament])
        initial-values {:player-name ""
                        :rating 0
                        :score 0}
        values (r/atom initial-values)
        save-player (fn [event {:keys [id player-name rating score]}]
                      (.preventDefault event)
                      (re-frame/dispatch [::events/upsert-player
                                          {:id id
                                           :player-name (str/trim player-name)
                                           :rating (js/parseInt rating)
                                           :score (js/parseInt score)}])
                      (reset! values initial-values))]
    (fn []
      [:> mui/Grid {:container true}
       [:a {:href "#/"}
        "go to Home Page"]
       [:> mui/Grid {:container true :direction "row"}
        [:> mui/TableContainer {:component mui/Paper}
         [:> mui/Table {:size "small"}
          [:> mui/TableHead
           [:> mui/TableRow
            [:> mui/TableCell "Player"]
            [:> mui/TableCell "Rating"]
            [:> mui/TableCell "Points"]]]
          [:> mui/TableBody
           (for [[player-id {:keys [id player-name rating score]}] (:players @active-tournament)]
             ^{:key player-id}
             [player-row values {:id id
                                 :player-name player-name
                                 :rating rating
                                 :score  score}])]]]

        [:> mui/Button {:variant "contained"} "Add players" ]]
       [:> mui/Box {:mt 5}
        [:> mui/Button
         {:variant "contained"
          :color "secondary"
          :size "small"
          :on-click #()}
         "Delete tournament"]]

       [modal {:modal-name :edit-player
               :dialog-title "Edit players"
               :dialog-header ""
               :body [:> mui/Box [:form {:no-validate true
                                         :on-submit #(save-player % @values)}
                                  [:> mui/Grid {:container true
                                                :direction "column"
                                                :align-items "center"
                                                :justify "center"}
                                   [form-group {:id :player-name
                                                :label "Player name"
                                                :type "text"
                                                :values values}]
                                   [form-group {:id :rating
                                                :label "Player rating"
                                                :type "number"
                                                :values values}]]]]
               :dialog-actions [:<>
                                [:> mui/Button
                                 {:on-click #(re-frame/dispatch [::events/close-modal])}
                                 "Close"]
                                [:> mui/Button
                                 {:on-click #(save-player % @values)}
                                 "Save player"]]}]
       ])))

;; main


(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    :tournament-panel [tournament-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []

  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:<>
     [:> mui/Grid {:container true :justify "center"}
      [:> mui/Grid {:item true}
       [show-panel @active-panel]]]]))

(ns swiss-maker.views
  (:require ["@material-ui/core" :as mui]
            ["@material-ui/icons" :as mui-icons]
            [clojure.string :as str]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [swiss-maker.components.form-group :refer [form-group]]
            [swiss-maker.components.modal :refer [modal]]
            [swiss-maker.components.select :refer [selector]]
            [swiss-maker.events :as events]
            [swiss-maker.subs :as subs]))



;; home
(defn tournament-editor []
  (let [initial-values {:tournament-name ""
                        :num-of-rounds   0}
        values         (r/atom initial-values)
        open-modal     (fn [{:keys [modal-name tournament]}]
                         (re-frame/dispatch [::events/open-modal modal-name])
                         (reset! values tournament))
        save           (fn [event {:keys [tournament-name num-of-rounds]}]
                         (.preventDefault event)
                         (re-frame/dispatch [::events/create-tournaments {:tournament-name (str/trim tournament-name)
                                                                          :num-of-rounds   (js/parseInt num-of-rounds)}])
                         (reset! values initial-values))]
    (fn []
      [:> mui/Box {:mt 5}
       [:> mui/Button {:variant  "contained"
                       :color    "secondary"
                       :on-click #(open-modal {:modal-name :create-tournament
                                               :tournament @values})}
        "Create tournament!"]
       [modal {:modal-name     :create-tournament
               :dialog-title   "Create tournament"
               :dialog-header  ""
               :body           [:div [:form {:no-validate true
                                             :on-submit   #(save % @values)}
                                      [:> mui/Grid {:container   true
                                                    :direction   "column"
                                                    :align-items "center"
                                                    :justify     "center"}
                                       [form-group {:id     :tournament-name
                                                    :label  "Tournament name"
                                                    :type   "text"
                                                    :values values}]
                                       [form-group {:id     :num-of-rounds
                                                    :label  "Number of rounds"
                                                    :type   "number"
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
       [:> mui/Button {:on-click #(re-frame/dispatch [::events/get-players-for-tournament 1])} "Get players"]
       [:> mui/Button {:on-click #(re-frame/dispatch [::events/get-tournaments])} "Get tournaments"]
       [:> mui/Button {:on-click #(re-frame/dispatch [::events/get-pairings-for-tournament 1 1])} "Get pairings"]
       [:> mui/Typography {:variant "h5"} "Active tournaments:"]
       (for [[tourney-id tournament] @tourneys]
         ^{:key tourney-id}
         [:<>
          [:a {:href (str "#tournaments/" tourney-id)}

           [:> mui/Card {:variant "outlined"
                         :class   "editable"}
            [:> mui/CardContent
             [:> mui/Grid {:container true :direction "row"}
              [:> mui/Typography
               {:variant "body1"} (:name tournament)]]]]]])

       [tournament-editor]])))

;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:href "#/"}
     "go to Home Page"]]])


(defn player-row [values {:keys [id name rating current-score] :as player}]
  (let [open-modal (fn [{:keys [modal-name]}]
                     (re-frame/dispatch [::events/open-modal modal-name])
                     (re-frame/dispatch [::events/set-active-player id])
                     (reset! values player))]
    [:> mui/TableRow
     [:> mui/TableCell name]
     [:> mui/TableCell rating]
     [:> mui/TableCell current-score]
     [:> mui/TableCell [:> mui/Button {:on-click #(open-modal
                                                    {:modal-name :edit-player})} "Edit"]]]))

(defn current-round
  []
  (let [current-round (re-frame/subscribe [::subs/current-round])
        pairings      @(re-frame/subscribe [::subs/current-pairings])

        players       @(re-frame/subscribe [::subs/players])
        tournament-id (re-frame/subscribe [::subs/active-tournament])
        update-result (fn [event board-no]
                        (let [event-value (.. event -target -value)]
                          (.preventDefault event)
                          (when (not= event-value "")
                            (re-frame/dispatch [::events/update-scores {:board-no      board-no
                                                                        :result        event-value
                                                                        :tournament-id @tournament-id}])
                            (re-frame/dispatch [::events/update-pairings-for-tournament {:round-no      @current-round
                                                                                         :board-no      board-no
                                                                                         :result        (float event-value)
                                                                                         :tournament-id @tournament-id}]))))]
    [:> mui/Grid {:container true}
     [:> mui/Grid {:item true }
      [:> mui/Typography {:variant "h5"} "Current round pairs:"]
      [:form {:no-validate true}
       (for [[board-no result-map] pairings]
         (let [white-id (:white-id result-map)
               black-id (:black-id result-map)]
           ^{:key white-id}
           [:> mui/Paper
            [:> mui/Typography {:variant "body1"}
             (str "Board #" board-no ": "
                  (get-in players [white-id :name]) " (white) -- "
                  (get-in players [black-id :name])" (black)")]

            [:> mui/FormControl {:variant  "outlined"
                                 :disabled (if (= (:result result-map) -1) false true)}
             [:> mui/InputLabel "Result"]
             [:> mui/Select {:value     (:result result-map)
                             :id        "result-select"
                             :on-change #(update-result % board-no)
                             :label     "Result"}

              [:> mui/MenuItem {:value -1} "Select result" ]
              [:> mui/MenuItem {:value 0} "White won" ]
              [:> mui/MenuItem {:value 1} "Black won" ]
              [:> mui/MenuItem {:value 0.5} "Draw" ]]]]))]
      (when (not= pairings {})
        [:> mui/Button {:variant  "contained"
                        :on-click #(re-frame/dispatch [::events/finish-round @current-round])} "Finish round"])]
     ]))




(defn tournament-panel []

  (let [active-tournament (re-frame/subscribe [::subs/tournament])
        tournament-id     (re-frame/subscribe [::subs/active-tournament])
        num-of-rounds     (re-frame/subscribe [::subs/num-of-rounds])

        current-round-no (re-frame/subscribe [::subs/current-round])
        initial-values   {:player-name ""
                          :rating      0
                          :score       0}
        values           (r/atom initial-values)
        open-modal       (fn [{:keys [modal-name]}]
                           (re-frame/dispatch [::events/open-modal modal-name]))
        save-player      (fn [event {:keys [id player-name rating score]}]
                           (.preventDefault event)
                           (re-frame/dispatch [::events/create-players-for-tournament
                                               {:tournament-id @tournament-id
                                                :player-name   (str/trim player-name)
                                                :rating        (js/parseInt rating)
                                                :score         (js/parseInt score)}])
                           (reset! values initial-values))]

    (re-frame/dispatch [::events/get-players-for-tournament @tournament-id])
    (re-frame/dispatch [::events/get-pairings-for-tournament {:tournament-id @tournament-id}])
    (fn []
      (let [{player-id :id player-name :name} @(re-frame/subscribe [::subs/player])
            ]

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
             (for [[player-id {:keys [id name rating current-score]}] (:players @active-tournament)]
               ^{:key player-id}
               [player-row values {:id            id
                                   :name          name
                                   :rating        rating
                                   :current-score current-score}])]]]

          [:> mui/Button {:variant  "contained"
                          :on-click #(open-modal {:modal-name :edit-player }) } "Add player" ]
          (when-not (= @num-of-rounds @current-round-no)
            [:> mui/Button {:variant  "contained"
                            :on-click #(re-frame/dispatch [::events/start-round @tournament-id @current-round-no]) } "Start round" ])]
         [:> mui/Box {:mt 5}]
         [:> mui/Button
          {:variant  "contained"
           :color    "secondary"
           :size     "small"
           :on-click #(when (js/confirm "Are you sure? This cannot be undone")
                        (re-frame/dispatch [::events/delete-tournament @tournament-id])
                        (set! (.. js/window -location -href) "/"))}
          "Delete tournament"]

         ;; display previous results
         [:> mui/Grid {:container true}
          (let [results @(re-frame/subscribe [::subs/results])]

            [:> mui/Accordion
             [:> mui/AccordionSummary {:expand-icon (r/as-element [:> mui-icons/ExpandMore])}
              [:> mui/Typography "Previous rounds results:"]]
             (for [[round-no results-map] results]
               ^{:key round-no}
               [:<>
                [:> mui/Typography (str "Round " round-no " results:")]
                [:> mui/AccordionDetails

                 (js/console.log "results" results-map)
                 ;; todo rewrite using List
                 (for [[board-id res] results-map]
                   ^{:key board-id}
                   [:<>
                    (js/console.log "bid" board-id "res" res)
                    [:> mui/Box
                     [:> mui/Typography (str "board " board-id " : ")
                      (when-not (= -1 res)
                        (case res
                          0   (str "1 - 0")
                          1   (str "0 - 1")
                          0.5 (str "1/2 - 1/2")))]]])]])])]
         ;; display current pairings
         [current-round]

         [modal {:modal-name     :edit-player
                 :dialog-title   "Edit players"
                 :dialog-header  ""
                 :body           [:> mui/Box [:form {:no-validate true
                                                     :on-submit   #(save-player % @values)}
                                              [:> mui/Grid {:container   true
                                                            :direction   "column"
                                                            :align-items "center"
                                                            :justify     "center"}
                                               [form-group {:id     :player-name
                                                            :label  "Player name"
                                                            :type   "text"
                                                            :values values}]
                                               [form-group {:id     :rating
                                                            :label  "Player rating"
                                                            :type   "number"
                                                            :values values}]]]]
                 :dialog-actions [:<>
                                  (when player-name [:> mui/Button
                                                     {:color    "secondary"
                                                      :on-click #(when (js/confirm "Are you sure?")
                                                                   (re-frame/dispatch [::events/delete-player player-id]))}
                                                     "Delete player"])
                                  [:> mui/Button
                                   {:on-click #(re-frame/dispatch [::events/close-modal])}
                                   "Close"]
                                  [:> mui/Button
                                   {:on-click #(save-player % @values)}
                                   "Save player"]]}]
         ]))))

;; main


(defn- panels [panel-name]
  (case panel-name
    :home-panel       [home-panel]
    :about-panel      [about-panel]
    :tournament-panel [tournament-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []

  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (re-frame/dispatch [::events/get-tournaments])
    [:<>
     [:> mui/Grid {:container true :justify "center"}
      [:> mui/Grid {:item true}
       [show-panel @active-panel]]]]))

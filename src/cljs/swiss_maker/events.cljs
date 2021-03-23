(ns swiss-maker.events
  (:require
   [re-frame.core :as re-frame]
   [swiss-maker.db :as db]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [clojure.string :as string]
   [clojure.walk :as w]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def tournament-endpoint "http://localhost:3000/v1/tournaments")

(def player-endpoint "http://localhost:3000/v1/players")

(def pairing-endpoint "http://localhost:3000/v1/pairings")

(defn renamer [m]
  (reduce-kv (fn [accum k v]
               (assoc accum (keyword (string/replace (name k) "_" "-")) v)) {} m))

(defn prepare-tournaments
  "
  Convert from
  [{
             'tournament/id':1,
             'tournament/name':'First tournament',
             'tournament/num_of_rounds':1,
             'tournament/current_round':0
         }]}
  to
  [{:tournament-01 {:id :tournament-01
                   :name 'First tournament'
                   :current-round 0
                   :results {}
                   :pairings {}
                   :players {}}}]
  "
  [tournaments-response]
  (into {}
        (map (fn [t]
               (let [tournament (renamer t)]

                 {(:id tournament) (-> tournament
                                       (assoc :results {})
                                       (assoc :results {})
                                       (assoc :pairings {})
                                       (assoc :players {})
                                       )}))  tournaments-response)))

(defn prepare-players
  "
  to
  {:player-01
  {:id :player-01 :name 'Ivan Ivanov' :rating 1100 :score 0}
  :player-02
  {:id :player-02 :name 'Petr Petrov' :rating 1200 :score 0}
  :player-03
  {:id :player-03 :name 'Semen Fedorov' :rating 1250 :score 0}
  :player-04
  {:id :player-04 :name 'Anatoliy Sidorov' :rating 1600 :score
  0}

                                 }
  "
  [players-response]
  (into {}
        (map (fn [p]
               (let [pid    (:player/id p)
                     player (renamer p)]

                 {pid player})) players-response)))

(defn prepare-pairings
  "
                   :pairings      {:board-1 {:white  :player-01
                                             :black  :player-02
                                             :result ''}}
  "

  [pairing-response]
  (into {}
        (map (fn [p]
               (let [pid     (:pairing/id p)
                     pairing (renamer p)]

                 {pid pairing})) pairing-response)))

(comment
  (def tournaments {"tournaments" [{
                                    :tournament/id            1
                                    :tournament/name          "First tournament"
                                    :tournament/num_of_rounds 1
                                    :tournament/current_round 0
                                    }
                                   {
                                    :tournament/id            2
                                    :tournament/name          "Second tournament"
                                    :tournament/num_of_rounds 5
                                    :tournament/current_round 6
                                    }]})
  (prepare-tournaments (get tournaments "tournaments"))
  (def players [ {:player/id            "c6d60a7a-8997-419f-8a34-5edf719f0b5b"
                  :player/name          "Ivan Ivanov"
                  :player/rating        1000
                  :player/current-score 0
                  :player/tournament-id 1}
                {:player/id            "d6d60a7a-8997-419f-8a34-5edf719f0b5b"
                 :player/name          "Petr Petrov"
                 :player/rating        1200
                 :player/current-score 3
                 :player/tournament-id 1}])
  (prepare-players players)


  (def pairings [{:pairing/id       1
                  :pairing/white-id "c6d60a7a-8997-419f-8a34-5edf719f0b5b"
                  :pairing/black-id "df2330a4-0f4d-4603-a3a6-502b6a990dd1"
                  :pairing/board-no 1
                  :pairing/result   -1 }])

  (prepare-pairings pairings)
  )

(re-frame/reg-event-fx
  ::get-tournaments
  (fn [{:keys [db]} _]
    {:db         (assoc-in db [:loading :tournaments] true)
     :http-xhrio {:method          :get
                  :uri             tournament-endpoint
                  :response-format (ajax/json-response-format)
                  :on-success      [::get-tournaments-success]
                  :on-failure      [::endpoint-request-error ::get-tournaments]}}))


(re-frame/reg-event-fx
  ::get-players-for-tournament
  (fn [{:keys [db]} [_ tournament-id]]
    {:db         (assoc-in db [:loading :players] true)
     :http-xhrio {:method          :get
                  :uri             (str  player-endpoint "/" tournament-id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-players-success]
                  :on-failure      [::endpoint-request-error ::get-players-for-tournament]}}))


(re-frame/reg-event-fx
  ::get-pairings-for-tournament
  (fn [{:keys [db]} [_ {:keys [tournament-id round-no]}]]

    (js/console.log "in in xhrio" round-no)
    {:db         (assoc-in db [:loading :pairings] true)
     :http-xhrio {:method          :get
                  :uri             (str  pairing-endpoint "/" tournament-id "/" round-no)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-pairings-success]
                  :on-failure      [::endpoint-request-error ::get-pairings-for-tournament]}}))

(re-frame/reg-event-fx
  ::create-pairings-for-tournament
  (fn [{:keys [db]} [_ {:keys [tournament-id round-no]}]]
    (js/console.log "in create" tournament-id round-no)

    {:db         (assoc-in db [:loading :pairings] true)
     :http-xhrio {:method          :post
                  :uri             (str  pairing-endpoint "/" tournament-id "/" round-no)
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-pairings-success]
                  :on-failure      [::endpoint-request-error ::get-pairings-for-tournament]}}))

(re-frame/reg-event-db
  ::get-pairings-success
  (fn [db [_ pairings]]
    (let [pairings      (:pairing pairings)
          tournament-id (:pairing/tournament-id (first pairings))]
      (js/console.log "in get pairings" pairings)
      (-> db
          (assoc-in [:loading :pairings] false)
          (assoc-in [:tournaments tournament-id :pairings] (prepare-pairings
                                                             pairings))))))

(re-frame/reg-event-db
  ::get-tournaments-success
  (fn [db [_ tournaments]]
    (js/console.log "in get tournaments success")
    (-> db
        (assoc-in [:loading :tournaments] false)
        (assoc-in [:tournaments] (prepare-tournaments
                                   (w/keywordize-keys (get tournaments "tournaments")) )))))

(re-frame/reg-event-db
  ::get-players-success
  (fn [db [_ players-resp]]
    (let [players       (:players players-resp)
          tournament-id (:player/tournament-id (first players))]

      (-> db
          (assoc-in [:loading :players] false)
          (assoc-in [:tournaments tournament-id :players] (prepare-players players))))))

(re-frame/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
             db/default-db))

(re-frame/reg-event-db
::set-active-panel
(fn-traced [db [_ {:keys [active-panel tournament-id]}]]
           (-> db
               (assoc :active-panel active-panel)
               (assoc :active-tournament tournament-id))))

(re-frame/reg-event-db
  ::set-active-player
  (fn-traced [db [_ player-id]]
             (-> db
                 (assoc :active-player player-id))))

(re-frame/reg-event-db
  ::close-modal
  (fn-traced [db [_ active-panel]]
             (-> db
                 (assoc :active-modal nil)
                 (assoc :active-player nil))))

(re-frame/reg-event-db
  ::open-modal
  (fn-traced [db [_ modal-name]]
             (assoc db :active-modal modal-name)))

(re-frame/reg-event-db
  ::upsert-tournament
  (fn [db [_ tournament]]

    (let [tournament-id   (get tournament "tournament/id")
          tournament-name (get tournament "tournament/name")
          num-of-rounds   (get tournament "tournament/num-of-rounds")]
      (-> db (update-in [:tournaments tournament-id] merge {:id            tournament-id
                                                            :name          tournament-name
                                                            :num-of-rounds num-of-rounds
                                                            :players       []})
          ;; close modal
          (assoc-in [:active-modal] nil)))))
(re-frame/reg-event-db
  ::delete-tournament
  (fn [db [_ tournament-id]]
    (-> db
        (update-in [:tournaments] dissoc tournament-id)
        (assoc-in [:active-tournament] nil)
        (assoc-in [:active-panel] :home-panel))))

(re-frame/reg-event-db
  ::upsert-player
  (fn [db [_ player]]
    (js/console.log "player resp" player)
    (let [tournament-id (:player/tournament-id player)
          player-id     (:player/id player)
          player-name   (:player/name player)
          player-rating (:player/rating player)
          player-score  (:player/current-score player)]
      (-> db (update-in [:tournaments tournament-id :players player-id] merge {:id            player-id
                                                                               :name          player-name
                                                                               :rating        player-rating
                                                                               :current-score player-score})
          ;; close modal
          (assoc-in [:active-modal] nil)))))


(re-frame/reg-event-fx
  ::create-tournaments
  (fn [{:keys [db]} [_ {:keys [tournament-name num-of-rounds]}]]
    (let [data {:name          tournament-name
                :num-of-rounds num-of-rounds}]
      {:db         (assoc-in db [:loading :tournaments] true)
       :http-xhrio {:method          :post
                    :uri             tournament-endpoint
                    :params          data
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format)
                    :on-success      [::upsert-tournament]
                    :on-failure      [::endpoint-request-error ::get-tournaments]}})))

(re-frame/reg-event-fx
  ::create-players-for-tournament
  (fn [{:keys [db]} [_ {:keys [tournament-id player-name rating score]}]]
    (let [data {:name          player-name
                :rating        rating
                :current-score score}]
      {:db         (assoc-in db [:loading :players] true)
       :http-xhrio {:method          :post
                    :uri             (str  player-endpoint "/" tournament-id)
                    :params          data
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::upsert-player]
                    :on-failure      [::endpoint-request-error ::create-players-for-tournament]}})))

(re-frame/reg-event-db
  ::delete-player
  (fn [db [_ player-id]]
    (let [active-tournament (get-in db [:active-tournament])]
      (-> db
          (update-in [:tournaments active-tournament :players] dissoc player-id)
          (assoc-in [:active-player] nil)
          (assoc-in [:active-modal] nil)))))



(re-frame/reg-event-fx
  ::start-round
  (fn [{:keys [db]} [_ tournament-id current-round]]
    (js/console.log "HELLO cur round" current-round)
    {:db       (-> db
                   (update-in [:tournaments tournament-id :current-round] inc))
     :dispatch [::create-pairings-for-tournament {:tournament-id tournament-id
                                                  :round-no      (inc current-round)}]}))


(re-frame/reg-event-db
  ::update-scores
  (fn [db [_ {:keys [board-no result tournament-id]}]]

    (let [players (  (juxt :white-id :black-id) (get-in db [:tournaments tournament-id :pairings board-no]))]
      ;; update pairings map ;; (o white won, 0.5 draw, 1 black won)
      (if-let [winner (case result 0 (first players) 1 (second players) 0.5 nil)]
        (let [upd (update-in db [:tournaments tournament-id :players winner :current-score] inc)] (js/console.log upd) upd)
        (reduce (fn [db player-id] (update-in db [:tournaments tournament-id :players player-id :current-score] (partial + 0.5)))
                db players)))))

(re-frame/reg-event-db
  ::update-results
  (fn [db [_ {:keys [board-no result tournament-id]}]]
    (js/console.log (get-in db [:tournaments tournament-id :pairings board-no :result] ))
    (assoc-in db [:tournaments tournament-id :pairings board-no :result] result)))

(re-frame/reg-event-db
  ::finish-round
  (fn [db [_ current-round]]
    (let [tournament-id (get db :active-tournament)
          pairings      (get-in db [:tournaments tournament-id :pairings])]
      (-> db
          (assoc-in [:tournaments tournament-id :results current-round]
                    (reduce-kv (fn [acc board-no pairing]
                                 (assoc-in acc [board-no] (:result pairing))) {} pairings))
          (assoc-in [:tournaments tournament-id :pairings] {})))))

(comment
  (reduce #(assoc-in % [(first %2)] (:result (second %2))) {} pairings) ;; was before
  (def pairs {1 {:result 0} 2 {:result 1}})
  (reduce-kv (fn [acc board-no pairing]
               (prn acc board-no pairing)
               (assoc-in acc [1 board-no] (:result pairing))) {} pairs)
  (update-in {:foo {1 {:quux 1}}} [:foo 1 :quux]  inc)
  )

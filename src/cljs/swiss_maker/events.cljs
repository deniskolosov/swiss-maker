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
  [players-response]
  (into {}
        (map (fn [p]
               (let [pid    (:player/id p)
                     player (renamer p)]
                 {pid player})) players-response)))

(defn prepare-pairings
  [pairing-response]
  (into {}
        (map (fn [p]
               (let [board-no (:pairing/board-no p)
                     pairing  (renamer p)]

                 {board-no pairing})) pairing-response)))

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
  (fn [{:keys [db]} [_ {:keys [tournament-id]}]]

    (js/console.log "in in xhrio" tournament-id )
    {:db         (assoc-in db [:loading :pairings] true)
     :http-xhrio {:method          :get
                  :uri             (str  pairing-endpoint "/" tournament-id)
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

(re-frame/reg-event-fx
  ::update-pairings-for-tournament
  (fn [{:keys [db]} [_ data]]
    (js/console.log "in update pairings" data )

    {:db         (assoc-in db [:loading :pairings] true)
     :http-xhrio {:method          :put
                  :uri             (str  pairing-endpoint "/" (:tournament-id data))
                  :format          (ajax/json-request-format)
                  :params          data
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::update-results data]
                  :on-failure      [::endpoint-request-error ::update-pairings-for-tournament]}}))

(re-frame/reg-event-db
  ::get-pairings-success
  (fn [db [_ pairings]]
    (let [pairings      (:pairing pairings)
          tournament-id (:pairing/tournament-id (first pairings))
          round-no      (:pairing/round-no (:first pairings))]
      (js/console.log "in get pairings" pairings)
      (-> db
          (assoc-in [:loading :pairings] false)
          (assoc-in [:tournaments tournament-id :pairings] (prepare-pairings
                                                             pairings))
          (assoc-in [:tournaments tournament-id :results] (into {} (map (fn [p]
                                                                          {(:pairing/round-no p)
                                                                           {(:pairing/board-no p)
                                                                            (:pairing/result p)}})
                                                                        pairings)))))))


(comment
  ;; pairing resp -> {:round-no {:board-no result}}
  (def p-resp [{:pairing/id       3
                :pairing/white-id "sdfs-123"
                :pairing/black-id "sfsdf22"
                :pairing/board-no 1
                :pairing/result   1
                :pairing/round-no 1}
               {:pairing/id       4
                :pairing/white-id "sdfs-123"
                :pairing/black-id "sfsdf22"
                :pairing/board-no 1
                :pairing/result   0.5
                :pairing/round-no 2}])
  (into {} ( map (fn [p] {(:pairing/round-no p) {(:pairing/board-no p) (:pairing/result p)}}) p-resp))

  )

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
          num-of-rounds   (get tournament "tournament/num-of-rounds")
          current-round   (get tournament "tournament/current-round")]
      (-> db (update-in [:tournaments tournament-id] merge {:id            tournament-id
                                                            :name          tournament-name
                                                            :current-round current-round
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
  ::update-current-round
  (fn [{:keys [db]} [_ {:keys [tournament-id current-round]}]]
    (js/console.log "tid" tournament-id "cur round" current-round)
    (let [data {:current-round current-round}]
      {:db         (assoc-in db [:loading :tournaments] true)
       :http-xhrio {:method          :put
                    :uri             (str tournament-endpoint "/" tournament-id)
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
  (fn [{:keys [db]} [_ tournament-id current-round-no]]
    (js/console.log "HELLO cur round" current-round-no)
    {:db         (-> db
                     (update-in [:tournaments tournament-id :current-round] inc))
     :dispatch-n [[::create-pairings-for-tournament {:tournament-id tournament-id
                                                     :round-no      (inc current-round-no)}]
                  [::update-current-round {:tournament-id tournament-id
                                           :current-round (inc current-round-no)}]]}))


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
(fn [db [_ {:keys [tournament-id board-no result]}]]
  (js/console.log "results in update results db event" tournament-id board-no result)
  (assoc-in db [:tournaments tournament-id :pairings board-no :result] result)
  ))

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

(ns swiss-maker.events
  (:require
   [re-frame.core :as re-frame]
   [swiss-maker.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

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
 (fn [db [_ {:keys [tournament-name num-of-rounds]}]]
   (let [tournament-id (get-in db [:active-tournament])
         id (or tournament-id (keyword (str "tournament-" (random-uuid))))]
     (-> db (update-in [:tournaments id] merge {:id id
                                                :tournament-name tournament-name
                                                :num-of-rounds num-of-rounds
                                                :players []})
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
 (fn [db [_ {:keys [id player-name rating score]}]]
   (let [tournament-id (get-in db [:active-tournament])
         player-id (get-in db [:active-player])
         id (or player-id (keyword (str "player-" (random-uuid))))]
     (-> db (update-in [:tournaments tournament-id :players id] merge {:id id
                                                                       :player-name player-name
                                                                       :rating rating
                                                                       :score (or score 0)})
         ;; close modal
         (assoc-in [:active-modal] nil)))))

(re-frame/reg-event-db
 ::delete-player
 (fn [db [_ player-id]]
   (let [active-tournament (get-in db [:active-tournament])]
     (-> db
         (update-in [:tournaments active-tournament :players] dissoc player-id)
         (assoc-in [:active-player] nil)
         (assoc-in [:active-modal] nil)))))



(re-frame/reg-event-db
 ::start-round
 (fn [db [_ tournament-id]]
   (-> db
       (update-in [:tournaments tournament-id :current-round] inc)
       ;; fixture
       (update-in [:tournaments tournament-id :pairings] merge {:board-1 {:white :player-01
                                                                          :black :player-02
                                                                          :result ""}
                                                                :board-2 {:white :player-03
                                                                          :black :player-04
                                                                          :result ""}}))))


(re-frame/reg-event-db
 ::update-scores
 (fn [db [_ {:keys [board-no result tournament-id]}]]

   (let [players ((juxt :white :black) (get-in db [:tournaments tournament-id :pairings board-no]))]
     ;; update pairings map ;; (o white won, 0.5 draw, 1 black won)

     (if-let [winner (case result 0 (first players) 1 (second players) 0.5 nil)]
       (update-in db [:tournaments tournament-id :players winner :score] inc)
       (reduce #(update-in % [:tournaments :tournament-01 :players %2 :score] (partial + 0.5))
               db players)))))

(re-frame/reg-event-db
 ::update-results
 (fn [db [_ {:keys [board-no result tournament-id]}]]
   (assoc-in db [:tournaments tournament-id :pairings board-no :result] result)))

(re-frame/reg-event-db
 ::finish-round
 (fn [db [_ current-round]]
   (let [tournament-id (get db :active-tournament)
         pairings (get-in db [:tournaments tournament-id :pairings])]
     (-> db
         (assoc-in [:tournaments tournament-id :results current-round]
                   (reduce #(assoc-in % [(first %2)] (:result (second %2))) {} pairings))
         (assoc-in [:tournaments tournament-id :pairings] {})))))

(comment
  )

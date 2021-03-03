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
       (update-in [:tournaments tournament-id :pairings] merge {1 { :player-01 :player-02}}))))

;; (re-frame/reg-event-db
;;  ::create-pairings
;;  (fn [db [_ tournament-id]]
;;    (update-in db [:tournaments tournament-id :pairings] merge {:player-01 :player-02})))



(comment
  (random-uuid)
  (update-in {:hello {:world 1}} [:hello :world] inc)
  (update-in {:hello {:world 1 :bar {}}} [:hello :bar] merge {:foo :baz})
  )

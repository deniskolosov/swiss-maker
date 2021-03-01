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


;; {:db (update-in db [:tournaments id] merge {:id tournament-id
;;                                             :tournament-name tournament-name
;;                                             :players []
;;                                             :num-of-rounds num-of-rounds})
;;  :dispatch [::close-modal]}
(comment
  (random-uuid)
  (update-in {:tournaments {:tourn-1 {:players {:name "Denis"}}}} [:tournaments :new-id] merge {:id "new-id"
                                                                                                 :name "Denis"})
  )

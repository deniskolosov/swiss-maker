(ns swiss-maker.events
  (:require
   [re-frame.core :as re-frame]
   [swiss-maker.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn-traced [db [_ active-panel]]
   (assoc db :active-panel active-panel)))


(re-frame/reg-event-db
 ::close-modal
 (fn-traced [db [_ active-panel]]
            (assoc db :active-modal nil)))

(re-frame/reg-event-db
 ::open-modal
 (fn-traced [db [_ modal-name]]
            (assoc db :active-modal modal-name)))

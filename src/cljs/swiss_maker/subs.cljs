(ns swiss-maker.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::tournaments
 (fn [db]
   (:tournaments db)))

(re-frame/reg-sub
 ::active-modal
 (fn [db]
   (:active-modal db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::active-tournament
 (fn [db _]
   (:active-tournament db)))

(re-frame/reg-sub
 ::tournament
 :<- [::tournaments]
 :<- [::active-tournament]
 (fn [[tournaments active-tournament] _]
   (get tournaments active-tournament)))


(re-frame/reg-sub
 ::players
 :<- [::tournament]
 (fn [tournament _]
   (:players tournament)))

(re-frame/reg-sub
 ::active-player
 (fn [db _]
   (:active-player db)))

(re-frame/reg-sub
 ::player
 :<- [::players]
 :<- [::active-player]
 (fn [[players active-player] _]
   (get players active-player)))

(comment
  (get {:tournament-01 {:id :tournament-01 :players [{:name "Denis"}]}} ::tournament-01))

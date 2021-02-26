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

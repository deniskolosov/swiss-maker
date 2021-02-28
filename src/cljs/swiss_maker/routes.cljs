(ns swiss-maker.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [swiss-maker.events :as events]
   ))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token ^js event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here

  (defroute "/" []
    (re-frame/dispatch [::events/set-active-panel {:active-panel :home-panel}]))

  (defroute "/about" []
    (re-frame/dispatch [::events/set-active-panel {:active-panel :about-panel}]))


  (defroute "/tournaments/:tournament-id"  [tournament-id]
    (re-frame/dispatch [::events/set-active-panel {:active-panel :tournament-panel
                                                   :tournament-id (keyword tournament-id)}]))


  ;; --------------------
  (hook-browser-navigation!))

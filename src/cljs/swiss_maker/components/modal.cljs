(ns swiss-maker.components.modal
  (:require [re-frame.core :as rf]
            ["@material-ui/core" :as mui]
            [swiss-maker.events :as events]
            [swiss-maker.subs :as subs]))

(defn modal
  [{:keys [modal-name dialog-title dialog-header body dialog-actions]}]
  ;; Helper function for displaying modals with material ui, pass
  ;; pass parts as reagent components [:> Component]
  (let [active-modal @(rf/subscribe [::subs/active-modal])]
    [:> mui/Dialog {:open (= active-modal modal-name)
                    :on-close #(rf/dispatch [::events/close-modal])}
     [:> mui/DialogTitle {:id dialog-title} dialog-title]
     [:> mui/DialogContent
      [:> mui/DialogContentText
       dialog-header]
      body]
     [:> mui/DialogActions
      dialog-actions]]))

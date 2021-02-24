(ns swiss-maker.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [swiss-maker.subs :as subs]
   ["@material-ui/core" :as mui]
   ))


;; home

(defn home-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:> mui/Typography "Create your tournament!"]
     [:> mui/Table
      [:> mui/TableHead
       [:> mui/TableRow
        [:> mui/TableCell "Name"]
        [:> mui/TableCell "Rating"]]]
      [:> mui/TableBody
       [:> mui/TableRow]]]
     [:div
      [:a {:href "#/about"}
       "go to About Page"]]]
    ))


;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:href "#/"}
     "go to Home Page"]]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []

  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:<>
     [show-panel @active-panel]
     ]))

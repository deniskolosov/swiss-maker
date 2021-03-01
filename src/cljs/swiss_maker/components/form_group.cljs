(ns swiss-maker.components.form-group
  (:require ["@material-ui/core" :as mui]))

(defn form-group
  [{:keys [id label type values]}]

  [:> mui/TextField {:id id
                     :label label
                     :type type
                     :value (id @values)
                     :variant "outlined"
                     :on-change #(swap! values assoc id (.. % -target -value))}]
  )

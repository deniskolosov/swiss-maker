(ns swiss-maker.components.select
  (:require ["@material-ui/core" :as mui]
            [reagent.core :as r]))

;; <FormControl variant="outlined" className={classes.formControl}>
  ;; <InputLabel id="demo-simple-select-outlined-label">Age</InputLabel>
    ;; <Select
      ;; labelId="demo-simple-select-outlined-label"
      ;; id="demo-simple-select-outlined"
      ;; value={age}
      ;; onChange={handleChange}
      ;; label="Age"
      ;; >
        ;; <MenuItem value="">
        ;; <em>None</em>
        ;; </MenuItem>
        ;; <MenuItem value={10}>Ten</MenuItem>
        ;; <MenuItem value={20}>Twenty</MenuItem>
        ;; <MenuItem value={30}>Thirty</MenuItem>
    ;; </Select>
;; </FormControl>

(defn selector
  [{:keys [id values label menu-items]}]

  [:> mui/FormControl {:variant "outlined"}
   [:> mui/InputLabel
    [:> mui/Select {:value 0
                    :on-change #(swap! values assoc id (.. % -target -value))
                    :label label}
     [:> mui/MenuItem {:value 0} "White won" ]
     [:> mui/MenuItem {:value 1} "Black won" ]
     [:> mui/MenuItem {:value 2} "Draw" ]]]])

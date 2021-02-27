(ns swiss-maker.db)

(def default-db
  {:tournaments {:tournament-01 {:id :tournament-01
                                 :tournament-name "First tournament"
                                 :players [{:name "Ivan Ivanov"
                                            :rating 1100
                                            :score 0}
                                           {:name "Petr Petrov"
                                            :rating 1200
                                            :score 0}]}}
   :active-modal nil})

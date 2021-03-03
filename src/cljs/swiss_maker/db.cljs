(ns swiss-maker.db)

(def default-db
  {:tournaments {:tournament-01 {:id :tournament-01
                                 :tournament-name "First tournament"
                                 :current-round 0
                                 :pairings {1 {}}
                                 :players {:player-01 {:id :player-01
                                                       :player-name "Ivan Ivanov"
                                                       :rating 1100
                                                       :score 0}
                                           :player-02 {:id :player-02
                                                       :player-name "Petr Petrov"
                                                       :rating 1200
                                                       :score 0}}}}
   :active-modal nil})

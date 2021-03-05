(ns swiss-maker.db)

(def default-db
  {:tournaments {:tournament-01 {:id :tournament-01
                                 :tournament-name "First tournament"
                                 :current-round 0
                                 :results {1 {:board-1 0}}
                                 :pairings {:board-1 {:white :player-01
                                                      :black :player-02
                                                      :result ""}}
                                 :players {:player-01 {:id :player-01
                                                       :player-name "Ivan Ivanov"
                                                       :rating 1100
                                                       :score 0}
                                           :player-02 {:id :player-02
                                                       :player-name "Petr Petrov"
                                                       :rating 1200
                                                       :score 0}
                                           :player-03 {:id :player-03
                                                       :player-name "Semen Fedorov"
                                                       :rating 1250
                                                       :score 0}
                                           :player-04 {:id :player-04
                                                       :player-name "Anatoliy Sidorov"
                                                       :rating 1600
                                                       :score 0}

                                           }}}
   :active-modal nil})

(ns demo.rpc
  (:require-macros
    [tailrecursion.javelin :refer [defc defc=]])
  (:require
   [tailrecursion.javelin]
   [tailrecursion.castra :refer [mkremote]]))

(defc state {:random nil})
(defc error nil)
(defc loading [])

(defc= random-number (get state :random))

(def get-state
  (mkremote 'demo.api/get-state state error loading))

(def up-vote! (mkremote 'demo.api/up-vote! state error loading))

(def down-vote! (mkremote 'demo.api/down-vote! state error loading))

(defn init []
  (get-state)
  (js/setInterval get-state 1000))

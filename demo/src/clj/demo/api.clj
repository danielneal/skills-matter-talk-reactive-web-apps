(ns demo.api
  (:require [tailrecursion.castra :refer [defrpc]]
            [datomic.api :as d]
            [clojure.algo.generic.functor :refer [fmap]]))

(def schema
  [{:db/id (d/tempid :db.part/db)
    :db/ident :option/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The title of an option"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :option/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The description of an option"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :user/name
    :db/unique :db.unique/identity
    :db/doc "The name of the user"
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :user/max-votes
    :db/doc "The maximum number of votes this user can have"
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :vote/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user casting the vote"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :vote/option
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The option being voted for"
    :db.install/_attribute :db.part/db}])

(def seed-data
  [{:db/id (d/tempid :db.part/user -1)
    :option/title "The Fat Duck"
    :option/description "Heston Blumenthal's flagship. Might involve eating table service."}
   {:db/id (d/tempid :db.part/user -2)
    :option/title "Dans le Noir"
    :option/description "Dine in the complete darkness. Be accompanied to your seat by blind waiters."}
   {:db/id (d/tempid :db.part/user -3)
    :option/title "42 degree raw"
    :option/description "Featuring food not heated above 42 degrees"}
   {:db/id (d/tempid :db.part/user -4)
    :option/title "The restaurant at the end of the universe"
    :option/description "Meet and dine with a fascinating cross-section of the entire population of space and time."}
   {:db/id (d/tempid :db.part/user -5)
    :user/name "Arthur Dent"
    :user/max-votes 5}
   {:db/id (d/tempid :db.part/user -6)
    :user/name "Ford Prefect"
    :user/max-votes 5}
   {:db/id (d/tempid :db.part/user -7)
    :user/name "Zaphod Beeblebrox"
    :user/max-votes 5}
   {:db/id (d/tempid :db.part/user -8)
    :user/name "Marvin the Robot"
    :user/max-votes 0} ; What would be the point in voting anyway, here I am brain the size of a planet ...
   ])


(let [uri "datomic:mem://demo"]
  (d/delete-database uri)
  (d/create-database uri)
  (def conn (d/connect uri))
  (d/transact conn schema)
  (d/transact conn seed-data))

(defn visible-entities
  "Return all the entities in the database that are relevant to this application"
  [db]
  (d/q '[:find ?e
         :in $ %
         :where (visible ?e)]
       db
       '[[(visible ?u)
          [?u :user/name _]]
         [(visible ?v)
          (?v :vote/option)]
         [(visible ?o)
          (?o :option/title)]]))

(defn load-entity
  "Loads an entity and its attributes. Keep in the db/id
  and replace references with ids (for use by DataScript)"
  [db entity]
  (as->
   (d/entity db entity) e
   (d/touch e)
   (into {:db/id (:db/id e)} e) ; needs to be a hash-map, not an entity map
   (fmap (fn [v]
           (cond (set? v) (mapv #(if (instance? datomic.query.EntityMap %) (:db/id %) %) v)
                 (instance? datomic.query.EntityMap v) (:db/id v)
                 :else v)) e)))

(defrpc get-state
  "End point to supply visible entities to client"
  []
  (let [db (d/db conn)]
    {:entities (map (comp (partial load-entity db) first) (visible-entities db))}))

(defrpc up-vote!
  "Up vote an option"
  [option user]
  (d/transact conn [{:db/id (d/tempid :db.part/user) :vote/option option :vote/user user}])
  (get-state))

(defrpc down-vote!
  "Down vote an option"
  [option user]
  (let [db (d/db conn)
        v (ffirst (d/q '[:find ?v
                         :in $ ?o ?u
                         :where
                         [?v :vote/user ?u]
                         [?v :vote/option ?o]] db option user))]
    (when v (d/transact conn [[:db.fn/retractEntity v]]))
    (get-state)))

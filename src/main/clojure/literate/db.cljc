(ns literate.db
  (:require [datascript.core :as d]))

(defonce conn (d/create-conn {:widget/uuid
                              {:db/unique :db.unique/identity}

                              :widget/parent
                              {:db/valueType :db.type/ref
                               :db/cardinality :db.cardinality/one}

                              :widget/children
                              {:db/valueType :db.type/ref
                               :db/cardinality :db.cardinality/many}}))

(defn retract-entity [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))

(defn all-widgets
  "Finds all Widgets."
  []
  (d/q '[:find [(pull ?e [:*]) ...]
         :in $
         :where
         [?e :widget/uuid]]
       @conn))

(defn root-widgets
  "Finds all Widgets which doesn't have a parent."
  []
  (d/q '[:find [(pull ?e [:* {:widget/children [*]}]) ...]
         :in $
         :where
         [?e :widget/uuid]
         [(missing? $ ?e :widget/parent)]]
       @conn))
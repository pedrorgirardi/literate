(ns literate.db
  (:require [datascript.core :as d]))

(defonce conn (d/create-conn {:snippet/uuid
                              {:db/unique :db.unique/identity}

                              :card/uuid
                              {:db/unique :db.unique/identity}

                              :card/snippets
                              {:db/valueType :db.type/ref
                               :db/cardinality :db.cardinality/many}}))

(defn all-snippets []
  (d/q '[:find [(pull ?e [*]) ...]
         :in $
         :where
         [?e :snippet/uuid]]
       @conn))

(defn remove-snippet [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))

(defn retract-entity [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))

(defn all-cards []
  (d/q '[:find [(pull ?e [:db/id :card/uuid {:card/snippets [*]}]) ...]
         :in $
         :where
         [?e :card/uuid]]
       @conn))
(ns literate.db
  (:require [datascript.core :as d]))

(defonce conn (d/create-conn {:snippet/uuid
                              {:db/unique :db.unique/identity}

                              :snippet/snippets
                              {:db/cardinality :db.cardinality/many
                               :db/valueType :db.type/ref}}))

(defn all-snippets []
  (d/q '[:find [(pull ?e [* {:snippet/snippets [*]}]) ...]
         :in $
         :where
         [?e :snippet/uuid]]
       @conn))

(defn remove-snippet [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))
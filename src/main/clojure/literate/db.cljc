(ns literate.db
  (:require [datascript.core :as d]))

(defonce conn (d/create-conn {:snippet/uuid
                              {:db/unique :db.unique/identity}}))

(defn all-snippets []
  (d/q '[:find [(pull ?e [*]) ...]
         :in $
         :where
         [?e :snippet/uuid]]
       @conn))

(defn remove-snippet [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))
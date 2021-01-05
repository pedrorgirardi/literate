(ns literate.core
  (:require [literate.server :as server]))

(defn transact
  "Sends a transact event to the client."
  [data]
  (when (seq data)
    (doseq [uid (:any @server/connected-uids)]
      (server/chsk-send! uid [:literate/!transact data]))))

(defn view [& widgets]
  (transact widgets))

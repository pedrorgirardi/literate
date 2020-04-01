(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [literate.server :as server]))

(comment

  (def stop-server (server/run-server))

  (do
    (stop-server)
    (refresh))


  @server/connected-uids

  (doseq [uid (:any @server/connected-uids)]
    (server/chsk-send! uid [:literate/!send "Hello"]))

  )
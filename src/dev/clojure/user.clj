(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [literate.server :as server]))

(comment

  (def stop-server (server/run-server))

  (do
    (stop-server)
    (refresh))

  )
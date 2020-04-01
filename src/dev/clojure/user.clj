(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [literate.server :as server]))

(comment

  (refresh)

  (def server (server/run-server))

  (server))
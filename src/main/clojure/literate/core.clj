(ns literate.core
  (:require [literate.server :as server]))

(defn -main [& args]
  (server/run-server {:port 8080}))

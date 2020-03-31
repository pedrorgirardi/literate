(ns user
  (:require [literate.server :as server]))

(comment

  (def server (server/run-server))

  (server)

  (require '[ring.middleware.anti-forgery :as anti-forgery])

  (force anti-forgery/*anti-forgery-token*)

  )
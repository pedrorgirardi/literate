(ns literate.server
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [cognitect.transit :as transit]
            [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream InputStream)))


(let [{:keys [ch-recv
              send-fn
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server! (get-sch-adapter) {:csrf-token-fn nil})]

  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defn transit-encode
  "Encode `x` in Transit-JSON.

   Returns a Transit JSON-encoded string."
  [x]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer x)
    (.toString out)))

(defn transit-decode [^InputStream x]
  (transit/read (transit/reader x :json)))

(defroutes app
  ;; -- App.
  (GET "/" _ (io/resource "public/index.html"))

  ;; -- WebSocket.
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))

  ;; -- Client API.
  (POST "/api/v1/transact" req
    (try
      (let [data (transit-decode (:body req))]
        (if (seq data)
          (do
            (doseq [uid (:any @connected-uids)]
              (chsk-send! uid [:literate/!transact data]))

            {:status 201
             :headers {"Content-Type" "application/transit+json"}
             :body (transit-encode true)})
          (throw (ex-info "Invalid data." {}))))
      (catch Exception ex
        {:status 400
         :headers {"Content-Type" "application/transit+json"}
         :body (transit-encode {:error {:message (ex-message ex)}})})))

  ;; -- Public resources.
  (route/resources "/")

  (route/not-found "<h1>Page not found</h1>"))

(defn run-server
  "Start Literate HTTP server (default port is 8090).

   Returns `(fn [& {:keys [timeout] :or {timeout 100}}])`
   which you can call to stop the server.

   `options` are the same as org.httpkit.server/run-server."
  [& [options]]
  (let [config (assoc-in site-defaults [:security :anti-forgery] false)]
    (http-kit/run-server (wrap-defaults app config) options)))


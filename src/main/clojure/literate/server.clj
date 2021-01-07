(ns literate.server
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :as page]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [cognitect.transit :as transit])
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

(defn index [req]
  (page/html5
    [:title "Literate"]

    (page/include-css "https://unpkg.com/tailwindcss@2.0/dist/tailwind.min.css")
    (page/include-css "https://fonts.googleapis.com/css2?family=Cinzel&display=swap")
    (page/include-css "https://cdnjs.cloudflare.com/ajax/libs/material-design-iconic-font/2.2.0/css/material-design-iconic-font.min.css")
    (page/include-css "css/codemirror.css")
    (page/include-css "css/leaflet.css")

    [:body.bg-gray-100
     [:div#app.container.mx-auto.h-screen]

     (page/include-js "js/main.js")]))

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
  (GET "/" req (index req))

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


(ns literate.server
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :as page]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))


(let [{:keys [ch-recv
              send-fn
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defn index [req]
  (page/html5
    (page/include-css "https://unpkg.com/tailwindcss@1.0/dist/tailwind.min.css")
    (page/include-css "https://fonts.googleapis.com/css2?family=Cinzel&display=swap")
    (page/include-css "css/codemirror.css")

    [:body.bg-gray-100
     [:div#sente-csrf-token {:data-csrf-token (:anti-forgery-token req)}]

     [:div#app.container.mx-auto.h-screen]

     (page/include-js "js/main/main.js")]))

(defroutes app
  (GET "/" req (index req))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn run-server []
  (http-kit/run-server (wrap-defaults app site-defaults) {:port 8008}))


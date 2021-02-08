(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]

            [literate.server :as server]
            [literate.client.core :as literate]

            [rum.server-render]
            [org.httpkit.client :as http])
  (:import (java.util UUID)))

(def stop-server
  (fn []
    nil))

(defn start-server [& [{:keys [port]}]]
  (alter-var-root #'stop-server (fn [_]
                                  (server/run-server {:port (or port 8118)}))))

(defn reset []
  (stop-server)

  (refresh :after `start-server))

(def l (partial literate/view {:url "http://localhost:8118"}))

(comment

  (reset)

  ;; -- Client API.

  (http/post "http://localhost:8118/api/v1/transact" {:body (server/transit-encode [(literate/code "Hello")])})



  ;; -- Geoplot.

  (l {:widget/uuid (str (UUID/randomUUID))
      :widget/type :widget.type/geoplot
      :widget.geoplot/height "600px"
      :widget.geoplot/center [13.502 -39.155]
      :widget.geoplot/center-wsg84? true
      :widget.geoplot/features
      [{:geoplot.feature/wkt "POLYGON((10.689 -25.092, 34.595 -20.170, 38.814 -35.639, 13.502 -39.155, 10.689 -25.092))"
        :geoplot.feature/data-projection "EPSG:4326"
        :geoplot.feature/feature-projection "EPSG:3857"
        :geoplot.feature/style
        {:geoplot.style/fill {:geoplot/color "rgba(252, 165, 165, 0.2)"}
         :geoplot.style/stroke {:geoplot/color "rgba(239, 68, 68, 1)"}}}]})


  ;; -- Vega Lite.

  (l (literate/vega-lite
       {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
        :description "A simple bar chart with embedded data."
        :data {:values
               [{:a "A" :b 28}
                {:a "B" :b 55}
                {:a "C" :b 43}
                {:a "D" :b 91}
                {:a "E" :b 81}
                {:a "F" :b 53}
                {:a "G" :b 19}
                {:a "H" :b 87}
                {:a "I" :b 52}]}
        :mark "bar"
        :encoding {:x {:field "a"
                       :type "ordinal"}
                   :y {:field "b"
                       :type "quantitative"}}}))

  ;; -- Code.

  (l (literate/code (slurp (io/resource "literate/core.clj"))))


  ;; -- Markdown.

  (l (literate/markdown "**Welcome to Literate**\n\nEval some forms to get started!"))


  ;; -- HTML.

  (l (literate/html
       (rum.server-render/render-static-markup
         [:div.bg-white.p-3
          [:h1.text-6xl "Hello, world!"]
          [:span "Text"]])))


  ;; -- Hiccup.

  (l (literate/hiccup
       [:div.bg-white.p-3
        [:h1.text-6xl "Hello, world!"]
        [:span "Text"]]))


  ;; -- Welcome.

  (l (literate/column
       {}
       (literate/hiccup
         [:div.flex.flex-col.space-y-3.p-3.font-light
          [:h1.text-3xl
           {:style {:font-family "Cinzel"}}
           "Welcome to Literate"]

          [:p.font-semibold
           "Literate is a Clojure & ClojureScript application which you can use to visualize data."]

          [:p.mt-4
           "This interface that you're looking at it's called a " [:span.font-bold "Widget"]
           ", and you can create one from a Clojure REPL."]

          [:p.mt-2.mb1 "There are a few different types of Widgets that are supported:"]

          [:ul.list-disc.list-inside.ml-2
           [:li "Code"]
           [:li "Markdown"]
           [:li "Hiccup"]
           [:li "Vega Lite"]
           [:li "Leaflet"]
           [:li "Column layout"]
           [:li "Row layout"]]])

       (literate/hiccup
         [:span.p-2.text-lg "Vega Lite Widget"])

       (literate/vega-lite
         {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
          :description "A simple bar chart with embedded data."
          :data {:url "https://vega.github.io/editor/data/stocks.csv"}
          :transform [{"filter" "datum.symbol==='GOOG'"}],
          :mark "line"
          :encoding {:x {:field "date"
                         :type "temporal"}
                     :y {:field "price"
                         :type "quantitative"}}})

       (literate/hiccup
         [:span.p-2.text-lg "Code Widget"])

       (literate/code (slurp (io/resource "literate/core.clj")))))

  )
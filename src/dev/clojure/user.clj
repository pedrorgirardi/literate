(ns user
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.java.io :as io]

   [literate.server :as server]
   [literate.client :as l]

   [hiccup.core :as hiccup]
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

(def l (partial l/transact {:url "http://localhost:8118"}))

(defn replace-widget
  "Select widget UUID and merge with replacement.

   Use this function when you want to transact a 'new version' of widget."
  [widget replacement]
  (merge replacement (select-keys widget [:widget/uuid])))

(comment

  (reset)

  ;; -- Client API.

  (http/post "http://localhost:8118/api/v1/transact" {:body (server/transit-encode [(l/codemirror "Hello")])})


  (l (l/table
       {:columns
        [[:a "A"]
         [:b "B"]
         [:c "C"]]

        :rows
        (repeat 100 {:a 1 :b 1 :c 1})}))


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
         :geoplot.style/stroke {:geoplot/color "rgba(239, 68, 68, 1)"}}}

       {:geoplot.feature/wkt "POLYGON((8.689 -23.092, 32.595 -18.170, 36.814 -33.639, 11.502 -37.155, 8.689 -23.092))"
        :geoplot.feature/data-projection "EPSG:4326"
        :geoplot.feature/feature-projection "EPSG:3857"}

       {:geoplot.feature/wkt "POINT(8.689 -23.092)"
        :geoplot.feature/data-projection "EPSG:4326"
        :geoplot.feature/feature-projection "EPSG:3857"}]})


  ;; -- Vega Lite.

  (l (l/vega-lite
       {:description "A simple bar chart with embedded data."
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

  ;; -- Codemirror.

  (l (l/codemirror (slurp (io/resource "literate/core.clj")) {:lineNumbers true}))
  (l (l/codemirror "**Welcome to Literate**\n\nEval some forms to get started!" {:mode "gfm"
                                                                                        :height "auto"}))


  ;; -- Markdown.

  (l (l/markdown "# Welcome to Literate\n\n## This is a markdown Widget"))


  ;; -- HTML.

  (l (l/html
       (hiccup/html
         [:div.bg-white.p-3
          [:h1.text-6xl "Hello, world!"]
          [:span "Text"]])))


  ;; -- Hiccup.

  (l (l/hiccup
       [:div.bg-white.p-3
        [:h1.text-6xl "Hello, world!"]
        [:span "Text"]]))


  ;; -- How to update a Widget.

  @(def html-example (l/hiccup [:h1 "Time is" (java.time.LocalDateTime/now)]))

  (l html-example)

  (l (replace-widget html-example (l/hiccup [:h1 "Time is " (java.time.LocalDateTime/now)])))


  ;; -- Welcome.

  (l (l/column
       {}
       (l/hiccup
         [:div.flex.flex-col.space-y-3.p-3.font-light
          [:h1.text-3xl
           {:style {:font-family "Cinzel"}}
           "Welcome to Literate"]

          [:p.text-xl
           "Literate is a Clojure & ClojureScript application which you can use to create interactive documents."]

          [:p.mt-4
           "This interface that you're looking at it's called a " [:span.font-bold "Widget"]
           ", and you can create one from a Clojure REPL."]

          [:p.mt-2.mb1 "There are a few different types of Widgets that are supported:"]

          [:ul.list-disc.list-inside.ml-2
           [:li "Codemirror"]
           [:li "Markdown"]
           [:li "Hiccup"]
           [:li "Vega"]
           [:li "Map"]
           [:li "Column layout"]
           [:li "Row layout"]]])

       (l/hiccup
         [:span.p-2.text-lg "Vega Lite Widget"])

       (l/vega-lite
         {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
          :description "A simple bar chart with embedded data."
          :data {:url "https://vega.github.io/editor/data/stocks.csv"}
          :transform [{"filter" "datum.symbol==='GOOG'"}],
          :mark "line"
          :encoding {:x {:field "date"
                         :type "temporal"}
                     :y {:field "price"
                         :type "quantitative"}}})

       (l/hiccup
         [:span.p-2.text-lg "Codemirror Widget"])

       (l/codemirror (slurp (io/resource "literate/core.clj"))))))
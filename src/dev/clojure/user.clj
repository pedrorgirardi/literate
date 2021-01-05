(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]

            [literate.db :as db]
            [literate.server :as server]
            [literate.core :as literate]
            [literate.widget :as widget]

            [rum.server-render]
            [datascript.core :as d]))

(def stop-server
  (fn []
    nil))

(defn start-server [& [{:keys [port]}]]
  (alter-var-root #'stop-server (fn [_]
                                  (server/run-server {:port (or port 8080)}))))

(defn reset []
  (stop-server)

  (refresh :after `start-server))

(comment

  (reset)

  ;; -- Vega Lite.

  (literate/view
    (widget/vega-lite
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

  (literate/view
    (widget/code (slurp (io/resource "literate/widget.clj"))))


  ;; -- Leaflet.

  (literate/view
    (widget/leaflet {}))

  (def geojson (json/read (io/reader "src/dev/resources/points.geojson")))

  (def sample (update geojson "features" #(take 10 %)))

  (def center (vec (reverse (get-in geojson ["features" 0 "geometry" "coordinates"]))))

  (def leaflet-widget
    (widget/leaflet
      {:style {:height "600px"}
       :center center
       :zoom 10
       :geojson (update geojson "features" #(take 10 %))}))

  (literate/view leaflet-widget)

  (literate/view
    (merge leaflet-widget {:widget/geojson (update geojson "features" #(take 10 %))}))


  ;; -- Markdown.

  (literate/view
    (widget/markdown "**Welcome to Literate**\n\nEval some forms to get started!"))


  ;; -- HTML.

  (literate/view
    (widget/html
      (rum.server-render/render-static-markup
        [:div.bg-white.p-3
         [:h1.text-6xl "Hello from Hiccup"]
         [:span "Text"]])))


  ;; -- Row.

  (literate/view
    (widget/row
      {}
      (widget/leaflet {})
      (widget/vega-lite
        {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
         :description "A simple bar chart with embedded data."
         :data {:url "https://vega.github.io/editor/data/stocks.csv"}
         :transform [{"filter" "datum.symbol==='GOOG'"}],
         :mark "line"
         :encoding {:x {:field "date"
                        :type "temporal"}
                    :y {:field "price"
                        :type "quantitative"}}})))


  ;; -- Column.

  (literate/view
    (widget/column
      {}
      (widget/leaflet {})
      (widget/vega-lite
        {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
         :description "A simple bar chart with embedded data."
         :data {:url "https://vega.github.io/editor/data/stocks.csv"}
         :transform [{"filter" "datum.symbol==='GOOG'"}],
         :mark "line"
         :encoding {:x {:field "date"
                        :type "temporal"}
                    :y {:field "price"
                        :type "quantitative"}}})))


  ;; -- Welcome.

  (literate/view
    (widget/column
      {}
      (widget/hiccup
        [:div.flex.flex-col.space-y-3.bg-white.p-3.font-light
         [:h1.text-3xl
          {:style {:font-family "Cinzel"}}
          "Welcome to Literate"]

         [:p.font-semibold
          "Literate is a graphical user interface extension for your Clojure REPL."]

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

      (widget/hiccup
        [:span.p-2.text-lg "Vega Lite Snippet"])

      (widget/vega-lite
        {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
         :description "A simple bar chart with embedded data."
         :data {:url "https://vega.github.io/editor/data/stocks.csv"}
         :transform [{"filter" "datum.symbol==='GOOG'"}],
         :mark "line"
         :encoding {:x {:field "date"
                        :type "temporal"}
                    :y {:field "price"
                        :type "quantitative"}}})

      (widget/hiccup
        [:span.p-2.text-lg "Code Snippet"])

      (widget/code (slurp (io/resource "literate/core.clj")))

      (widget/hiccup
        [:span.p-2.text-lg "Leaflet Snippet"])

      (widget/leaflet {:style {:height "400px"}
                       :center [51.505 -0.09]
                       :zoom 10})))

  )
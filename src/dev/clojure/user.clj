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

  (literate/view
    (widget/code (slurp (io/resource "literate/core.clj"))))



  (literate/markdown "**Welcome to Literate**\n\nEval some forms to get started!")

  (literate/html (rum.server-render/render-static-markup [:div.bg-white.p-2
                                                          [:h1.text-6xl "Hello from Hiccup"]
                                                          [:span "Text"]]))

  (literate/hiccup [:div.bg-white.p-2.font-thin
                    [:h1.text-3xl {:style {:font-family "Cinzel"}} "Welcome to Literate"]

                    [:p.font-semibold "Literate is a graphical user interface extension for your Clojure REPL."]

                    [:p.mt-4 "This interface that you're looking at it's called a " [:span.font-bold "Snippet"]
                     ", and you can create one from a Clojure REPL."]

                    [:p.mt-2.mb1 "There's a few different types of Snippets that are supported:"]

                    [:ul.list-disc.list-inside.ml-2
                     [:li "Code"]
                     [:li "Markdown"]
                     [:li "Hiccup"]
                     [:li "Vega Lite"]]])

  (literate/code (mapv inc (range 10)))

  (literate/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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
                                      :type "quantitative"}}})

  (literate/transact
    [(literate/card

       ;; -- Hiccup

       (literate/hiccup-snippet
         [:div.bg-white.p-2.font-thin
          [:h1.text-3xl {:style {:font-family "Cinzel"}} "Welcome to Literate"]

          [:p.font-semibold "Literate is a graphical user interface extension for your Clojure REPL."]

          [:p.mt-4 "This interface is a " [:span.font-bold "Card"]
           ", and it contains a set of " [:span.font-bold "Snippets"] ". "
           "You can create Snippets and Cards from your Clojure REPL."]

          [:p.mt-2.mb1 "There's a few different types of Snippets that are supported:"]

          [:ul.list-disc.list-inside.ml-2
           [:li "Code"]
           [:li "Markdown"]
           [:li "Hiccup"]
           [:li "Vega Lite"]
           [:li "Leaflet"]]])

       ;; -- Vega Lite

       (literate/hiccup-snippet
         [:span.p-2.text-lg "Vega Lite Snippet"])

       (literate/vega-lite-snippet
         {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
          :description "A simple bar chart with embedded data."
          :data {:url "https://vega.github.io/editor/data/stocks.csv"}
          :transform [{"filter" "datum.symbol==='GOOG'"}],
          :mark "line"
          :encoding {:x {:field "date"
                         :type "temporal"}
                     :y {:field "price"
                         :type "quantitative"}}})

       ;; -- Code

       (literate/hiccup-snippet
         [:span.p-2.text-lg "Code Snippet"])

       (literate/code-snippet (slurp (io/resource "literate/core.clj")))

       ;; -- Leaflet

       (literate/hiccup-snippet
         [:span.p-2.text-lg "Leaflet Snippet"])

       (literate/leaflet-snippet {:style {:height "400px"}
                                  :center [51.505 -0.09]
                                  :zoom 10}))])


  (def hiccup-snippet-1
    (literate/hiccup-snippet [:span "Foo"]))

  (def hiccup-snippet-2
    (literate/hiccup-snippet [:span "Foo"]))

  (def card-1 (literate/card hiccup-snippet-1))

  (def card-2 (literate/card hiccup-snippet-1
                             hiccup-snippet-2))

  (literate/transact [card-1 card-2])

  ;; -- Update `hiccup-snippet-1` - notice that both cards update.
  (literate/transact [(assoc hiccup-snippet-1 :snippet/html (rum.server-render/render-static-markup [:span "Bar"]))])


  ;; -- Leaflet
  (def geojson (json/read (io/reader "src/dev/resources/points.geojson")))

  (def sample (update geojson "features" #(take 10 %)))

  (def center (vec (reverse (get-in geojson ["features" 0 "geometry" "coordinates"]))))

  (def leaflet-snippet
    (literate/leaflet-snippet
      {:style {:height "600px"}
       :center center
       :zoom 10
       :geojson (update geojson "features" #(take 10 %))}))

  (def leaflet-card
    (literate/card leaflet-snippet))

  (literate/transact [leaflet-card])

  ;; -- Update entity & transact.

  (def leaflet-snippet'
    (merge leaflet-snippet {:snippet/geojson (update geojson "features" #(take 5 %))}))

  (literate/transact [leaflet-snippet'])

  )
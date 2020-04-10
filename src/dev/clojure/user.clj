(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [literate.server :as server]
            [literate.core :as l]
            [rum.server-render]
            [hiccup.core :as hiccup]))

(def stop-server
  (fn []
    nil))

(defn start-server []
  (alter-var-root #'stop-server (fn [_]
                                  (server/run-server {:port 8080}))))

(defn reset []
  (stop-server)

  (refresh :after `start-server))

(comment

  (l/markdown "**Welcome to Literate**\n\nEval some forms to get started!")

  (l/html (rum.server-render/render-static-markup [:div.bg-white.p-2
                                                   [:h1.text-6xl "Hello from Hiccup"]
                                                   [:span "Text"]]))

  (l/hiccup [:div.bg-white.p-2
             [:h1.text-3xl {:style {:font-family "Cinzel"}} "Welcome to Literate"]])

  (l/code (slurp (io/resource "literate/core.clj")))

  (l/code (mapv inc (range 10)))

  (l/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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

  (l/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
                :data {:url "https://vega.github.io/editor/data/movies.json"}
                :mark "bar"
                :encoding {:x {:field "IMDB_Rating"
                               :type "quantitative"
                               :bin true}
                           :y {:aggregate "count"
                               :type "quantitative"}}})

  )
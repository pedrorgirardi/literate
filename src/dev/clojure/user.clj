(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]

            [literate.db :as db]
            [literate.server :as server]
            [literate.core :as l]

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

  (l/markdown "**Welcome to Literate**\n\nEval some forms to get started!")

  (l/html (rum.server-render/render-static-markup [:div.bg-white.p-2
                                                   [:h1.text-6xl "Hello from Hiccup"]
                                                   [:span "Text"]]))

  (l/hiccup [:div.bg-white.p-2.font-thin
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

  (l/transact
    [(l/card
       (l/hiccup-snippet
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
           [:li "Vega Lite"]]])

       (l/hiccup-snippet
         [:span.p-2.text-lg "Vega Lite Snippet"])

       (l/vega-lite-snippet
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
                         :type "quantitative"}}})

       (l/hiccup-snippet
         [:span.p-2.text-lg "Code Snippet"])

       (l/code-snippet (slurp (io/resource "literate/core.clj"))))])


  (def hiccup-snippet-1
    (l/hiccup-snippet [:span "Foo"]))

  (def hiccup-snippet-2
    (l/hiccup-snippet [:span "Foo"]))

  (def card-1 (l/card hiccup-snippet-1))

  (def card-2 (l/card hiccup-snippet-1
                      hiccup-snippet-2))

  (l/transact [card-1 card-2])

  ;; -- Update `hiccup-snippet-1` - notice that both cards update.
  (l/transact [(assoc hiccup-snippet-2 :snippet/html (rum.server-render/render-static-markup [:span "Bar"]))])

  )
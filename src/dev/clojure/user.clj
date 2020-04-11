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

  (def title (l/hiccup-snippet [:h1 "Title"]))

  (l/present [title])

  (let [updated-title (merge title (select-keys (l/hiccup-snippet [:h1 "New title"]) [:snippet/html]))]
    (l/present [updated-title]))

  (l/present
    [(l/hiccup-snippet [:h1 "Title"])

     (l/hiccup-snippet [:h2 "Sub-title"])])

  (l/deck
    (l/hiccup-snippet [:h1 "Title"])

    (l/hiccup-snippet [:h1 "Title"]))

  (l/deck
    (l/hiccup-snippet
      [:div.bg-white.p-2.font-thin
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
                      :type "quantitative"}}}))


  (def example-snippet
    #:snippet {:uuid (str (java.util.UUID/randomUUID))
               :type :snippet.type/code})


  (d/transact! db/conn [(assoc example-snippet :snippet/code "1")])


  (d/q '[:find [(pull ?e [*]) ...]
         :in $
         :where
         [?e :snippet/uuid]]
       @db/conn)

  )
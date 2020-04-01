(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [literate.server :as server]
            [literate.core :as literate]))

(comment

  (def stop-server (server/run-server))

  (do
    (stop-server)
    (refresh))


  (literate/present (literate/code "(map inc [1 2 3])\n\n{:x 1}\n\n[1 2 3]\n\n(def n 1)"))

  (literate/present (literate/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
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

  (literate/present (literate/vega-lite {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
                                         :data {:url "https://vega.github.io/editor/data/movies.json"}
                                         :mark "bar"
                                         :encoding {:x {:field "IMDB_Rating"
                                                        :type "quantitative"
                                                        :bin true}
                                                    :y {:aggregate "count"
                                                        :type "quantitative"}}}))


  )
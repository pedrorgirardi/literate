(ns literate.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :snippet/uuid string?)

(s/def :snippet/type #{:snippet.type/code
                       :snippet.type/markdown
                       :snippet.type/vega-lite
                       :snippet.type/hiccup
                       :snippet.type/html
                       :snippet.type/deck})

(s/def :literate/snippet (s/keys :req [:snippet/uuid
                                       :snippet/type]))

(s/def :literate/snippets (s/coll-of :literate/snippet))

(s/def :card/uuid string?)

(s/def :card/snippets :literate/snippets)

(s/def :literate/card (s/keys :req [:card/uuid
                                    :card/snippets]))

(s/def :literate/cards (s/coll-of :literate/card))



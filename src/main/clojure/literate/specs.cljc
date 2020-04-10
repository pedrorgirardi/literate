(ns literate.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :snippet/uuid string?)

(s/def :snippet/type #{:snippet.type/code
                       :snippet.type/markdown
                       :snippet.type/vega-lite
                       :snippet.type/hiccup
                       :snippet.type/html})

(s/def :literate/snippet (s/keys :req [:snippet/uuid
                                       :snippet/type]))

(s/def :literate/snippets (s/coll-of :literate/snippet))

(s/def :literate/client-state (s/keys :req [:literate/snippets]))

(ns literate.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :widget/uuid string?)

(s/def :widget/type #{:widget.type/code
                      :widget.type/markdown
                      :widget.type/vega-lite
                      :widget.type/hiccup
                      :widget.type/html
                      :widget.type/leaflet})

(s/def :literate/widget (s/keys :req [:widget/uuid
                                      :widget/type]))

(s/def :literate/widgets (s/coll-of :literate/widget))

(ns literate.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :widget/uuid string?)

(s/def :widget/type #{:widget.type/html
                      :widget.type/row
                      :widget.type/column
                      :widget.type/table
                      :widget.type/vega-embed
                      :widget.type/codemirror
                      :widget.type/markdown
                      :widget.type/geoplot})

(s/def :literate/widget (s/keys :req [:widget/uuid
                                      :widget/type]))

(s/def :literate/widgets (s/coll-of :literate/widget))

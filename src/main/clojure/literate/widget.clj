(ns literate.widget
  (:import (java.util UUID)))

(defn vega-lite
  "Returns a Vega Lite Widget entity."
  [vega-lite-spec]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/vega-lite
            :vega-lite-spec vega-lite-spec})
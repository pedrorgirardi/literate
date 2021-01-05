(ns literate.widget
  (:import (java.util UUID)))

(defn vega-lite
  "Returns a Vega Lite Widget entity."
  [vega-lite-spec]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/vega-lite
            :vega-lite-spec vega-lite-spec})

(defn code
  "Returns a Code Widget entity."
  [form]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/code
            :code (str form)})

(defn leaflet
  "Returns a Leaflet Widget entity."
  [{:keys [style center zoom geojson]}]
  (merge #:widget {:uuid (str (UUID/randomUUID))
                   :type :widget.type/leaflet
                   :center (or center [51.505 -0.09])
                   :zoom (or zoom 10)}

         (when style
           {:widget/style style})

         (when geojson
           {:widget/geojson geojson})))
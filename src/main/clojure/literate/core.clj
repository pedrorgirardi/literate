(ns literate.core
  (:require [literate.server :as server]

            [rum.server-render])

  (:import (java.util UUID)))

(defn transact
  "Sends a transact event to the client."
  [data]
  (when (seq data)
    (doseq [uid (:any @server/connected-uids)]
      (server/chsk-send! uid [:literate/!transact data]))))

(defn view [& widgets]
  (transact widgets))

(defn row
  "Returns a Row (horizontal) layout entity."
  [{:keys [gap]} & children]
  {:db/id -1
   :widget/uuid (str (UUID/randomUUID))
   :widget/type :widget.type/row
   :widget/children (map #(assoc % :widget/parent -1) children)})

(defn column
  "Returns a Column (vertical) layout entity."
  [{:keys [gap]} & children]
  {:db/id -1
   :widget/uuid (str (UUID/randomUUID))
   :widget/type :widget.type/column
   :widget/children (map #(assoc % :widget/parent -1) children)})

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

(defn markdown
  "Returns a Markdown Widget entity."
  [markdown]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/markdown
            :markdown markdown})

(defn html
  "Returns an Html Widget entity."
  [html]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/html
            :html html})

(defn hiccup
  "Returns a Hiccup Widget entity."
  [hiccup]
  #:widget {:uuid (str (UUID/randomUUID))
            :type :widget.type/hiccup
            :html (rum.server-render/render-static-markup hiccup)})

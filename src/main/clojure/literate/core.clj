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

(defn card
  "Returns a Card entity."
  [& snippets]
  #:card {:uuid (str (UUID/randomUUID))
          :snippets snippets})

(defn vega-lite-snippet
  "Returns a Vega Lite Snippet entity."
  [vega-lite-spec]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/vega-lite
             :vega-lite-spec vega-lite-spec})

(defn code-snippet
  "Returns a Code Snippet entity."
  [form]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/code
             :code (str form)})

(defn markdown-snippet
  "Returns a Markdown Snippet entity."
  [markdown]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/markdown
             :markdown markdown})

(defn html-snippet
  "Returns an Html Snippet entity."
  [html]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/html
             :html html})

(defn hiccup-snippet
  "Returns a Hiccup Snippet entity."
  [hiccup]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/hiccup
             :html (rum.server-render/render-static-markup hiccup)})

(defn leaflet-snippet
  "Returns a Leaflet Snippet entity."
  [{:keys [style center zoom geojson]}]
  (merge #:snippet {:uuid (str (UUID/randomUUID))
                    :type :snippet.type/leaflet
                    :center (or center [51.505 -0.09])
                    :zoom (or zoom 10)}

         (when style
           {:snippet/style style})

         (when geojson
           {:snippet/geojson geojson})))

(defn code
  "Transacts a Card containing a single Code Snippet."
  [code]
  (transact [(card (code-snippet code))]))

(defn vega-lite
  "Transacts a Card containing a single Vega Lite Snippet."
  [vega-lite-spec]
  (transact [(card (vega-lite-snippet vega-lite-spec))]))

(defn markdown
  "Transacts a Card containing a single Markdown Snippet."
  [markdown]
  (transact [(card (markdown-snippet markdown))]))

(defn html
  "Transacts a Card containing a single Html Snippet."
  [html]
  (transact [(card (html-snippet html))]))

(defn hiccup
  "Transacts a Card containing a single Hiccup Snippet."
  [hiccup]
  (transact [(card (hiccup-snippet hiccup))]))

(defn leaflet
  "Transacts a Card containing a single Leaflet Snippet."
  [{:keys [center zoom] :as m}]
  (transact [(card (leaflet-snippet m))]))

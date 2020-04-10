(ns literate.core
  (:require [literate.server :as server]
            [rum.server-render])
  (:import (java.util UUID)))

(defn present [snippet]
  (doseq [uid (:any @server/connected-uids)]
    (server/chsk-send! uid [:literate/snippet snippet])))

(defn vega-lite-snippet [vega-lite-spec]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/vega-lite
             :vega-lite-spec vega-lite-spec})

(defn code-snippet [form]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/code
             :code (str form)})

(defn markdown-snippet [markdown]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/markdown
             :markdown markdown})

(defn html-snippet [html]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/html
             :html html})

(defn hiccup-snippet [hiccup]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/hiccup
             :html (rum.server-render/render-static-markup hiccup)})

(defn code [form]
  (present (code-snippet form)))

(defn vega-lite [vega-lite-spec]
  (present (vega-lite-snippet vega-lite-spec)))

(defn markdown [markdown]
  (present (markdown-snippet markdown)))

(defn html [html]
  (present (html-snippet html)))

(defn hiccup [hiccup]
  (present (hiccup-snippet hiccup)))

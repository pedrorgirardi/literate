(ns literate.core
  (:require [literate.server :as server]
            [rum.server-render])
  (:import (java.util UUID)))

(defn transact [data]
  (when (seq data)
    (doseq [uid (:any @server/connected-uids)]
      (server/chsk-send! uid [:literate/!transact data]))))

(defn card [& snippets]
  #:card {:uuid (str (UUID/randomUUID))
          :snippets snippets})

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

(defn code [code]
  (transact [(card (code-snippet code))]))

(defn vega-lite [vega-lite-spec]
  (transact [(card (vega-lite-snippet vega-lite-spec))]))

(defn markdown [markdown]
  (transact [(card (markdown-snippet markdown))]))

(defn html [html]
  (transact [(card (html-snippet html))]))

(defn hiccup [hiccup]
  (transact [(card (hiccup-snippet hiccup))]))

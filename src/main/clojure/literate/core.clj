(ns literate.core
  (:require [literate.server :as server]
            [rum.server-render])
  (:import (java.util UUID)))

(defn present [& cards]
  (when (seq cards)
    (doseq [uid (:any @server/connected-uids)]
      (server/chsk-send! uid [:literate/!present cards]))))

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

(defn deck-snippet [& snippets]
  #:snippet {:uuid (str (UUID/randomUUID))
             :type :snippet.type/deck
             :snippets snippets})

(defn code [code]
  (present (card (code-snippet code))))

(defn vega-lite [vega-lite-spec]
  (present (card (vega-lite-snippet vega-lite-spec))))

(defn markdown [markdown]
  (present (card (markdown-snippet markdown))))

(defn html [html]
  (present (card (html-snippet html))))

(defn hiccup [hiccup]
  (present (card (hiccup-snippet hiccup))))

(defn deck [& snippets]
  (present (apply deck-snippet snippets)))

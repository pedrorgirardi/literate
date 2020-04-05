(ns literate.core
  (:require [literate.server :as server])
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

(defn code [form]
  (present (code-snippet form)))

(defn vega-lite [vega-lite-spec]
  (present (vega-lite-snippet vega-lite-spec)))

(defn markdown [markdown]
  (present (markdown-snippet markdown)))

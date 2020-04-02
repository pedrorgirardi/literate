(ns literate.core
  (:require [literate.server :as server])
  (:import (java.util UUID)))

(defn present [literate]
  (doseq [uid (:any @server/connected-uids)]
    (server/chsk-send! uid [:literate/literate literate])))

(defn vega-lite-literate [vega-lite-spec]
  #:literate {:uuid (str (UUID/randomUUID))
              :type :literate.type/vega-lite
              :vega-lite-spec vega-lite-spec})

(defn code-literate [form]
  #:literate {:uuid (str (UUID/randomUUID))
              :type :literate.type/code
              :code (str form)})

(defn markdown-literate [markdown]
  #:literate {:uuid (str (UUID/randomUUID))
              :type :literate.type/markdown
              :markdown markdown})

(defn code [form]
  (present (code-literate form)))

(defn vega-lite [vega-lite-spec]
  (present (vega-lite-literate vega-lite-spec)))

(defn markdown [markdown]
  (present (markdown-literate markdown)))

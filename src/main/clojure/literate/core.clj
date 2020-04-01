(ns literate.core
  (:require [literate.server :as server]))

(defn present [literate]
  (doseq [uid (:any @server/connected-uids)]
    (server/chsk-send! uid [:literate/literate literate])))

(defn vega-lite [vega-lite-spec]
  #:literate {:type :literate.type/vega-lite
              :vega-lite-spec vega-lite-spec})

(defn code [form]
  #:literate {:type :literate.type/code
              :code (str form)})

(defn present-code [form]
  (present (code form)))

(defn present-vega-lite [vega-lite-spec]
  (present (vega-lite vega-lite-spec)))

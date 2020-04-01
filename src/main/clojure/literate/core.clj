(ns literate.core
  (:require [literate.server :as server])
  (:import (java.util UUID)))

(defn present [literate]
  (doseq [uid (:any @server/connected-uids)]
    (server/chsk-send! uid [:literate/literate literate])))

(defn vega-lite [vega-lite-spec]
  #:literate {:uuid (str (UUID/randomUUID))
              :type :literate.type/vega-lite
              :vega-lite-spec vega-lite-spec})

(defn code [form]
  #:literate {:uuid (str (UUID/randomUUID))
              :type :literate.type/code
              :code (str form)})

(defn present-code [form]
  (present (code form)))

(defn present-vega-lite [vega-lite-spec]
  (present (vega-lite vega-lite-spec)))

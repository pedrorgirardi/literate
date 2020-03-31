(ns literate.core
  (:require [rum.core :as rum :refer [defc]]
            ["vega-embed" :default vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]))

(defc Code < {:did-mount (fn [state]
                           (let [[code] (:rum/args state)]
                             (codemirror (rum/dom-node state) #js {"value" code
                                                                   "mode" "clojure"
                                                                   "lineNumbers" true}))

                           state)}
  [code]
  [:div.w-screen])

(defc VegaLite < {:did-mount (fn [state]
                               (let [[vega-lite-spec] (:rum/args state)]
                                 (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                               state)}
  [vega-lite-spec]
  [:div.w-screen.m-2])


;; ---


(defmulti render :literate/type)

(defmethod render :literate.type/code
  [{:literate/keys [code]}]
  (Code code))

(defmethod render :literate.type/vega-lite
  [{:literate/keys [vega-lite-spec]}]
  (VegaLite vega-lite-spec))


;; ---



(def state-ref (atom [#:literate {:type :literate.type/code
                                  :code "(map inc [1 2 3])\n\n{:x 1}\n\n[1 2 3]\n\n(def n 1)"}

                      #:literate {:type :literate.type/vega-lite
                                  :vega-lite-spec {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
                                                   :description "A simple bar chart with embedded data."
                                                   :data {:values
                                                          [{:a "A" :b 28}
                                                           {:a "B" :b 55}
                                                           {:a "C" :b 43}
                                                           {:a "D" :b 91}
                                                           {:a "E" :b 81}
                                                           {:a "F" :b 53}
                                                           {:a "G" :b 19}
                                                           {:a "H" :b 87}
                                                           {:a "I" :b 52}]}
                                                   :mark "bar"
                                                   :encoding {:x {:field "a"
                                                                  :type "ordinal"}
                                                              :y {:field "b"
                                                                  :type "quantitative"}}}}

                      #:literate {:type :literate.type/vega-lite
                                  :vega-lite-spec {"$schema" "https://vega.github.io/schema/vega-lite/v4.json"
                                                   :data {:url "https://vega.github.io/editor/data/movies.json"}
                                                   :mark "bar"
                                                   :encoding {:x {:field "IMDB_Rating"
                                                                  :type "quantitative"
                                                                  :bin true}
                                                              :y {:aggregate "count"
                                                                  :type "quantitative"}}}}]))

(defc App < rum/reactive []
  (into [:div.flex.flex-col.p-10.rounded

         [:span.text-lg.text-gray-700.mb-10
          {:style {:font-family "Cinzel"}}
          "Literate"]]

        (for [cell (rum/react state-ref)]
          [:div.relative.flex.bg-white.mb-10.shadow

           ;; Literate type
           ;;[:span.absolute.top-0.mt-3.right-0.mr-3.text-xs.tracking-wider.uppercase.text-gray-500
           ;; (name (:literate/type cell))]

           (render cell)])))

(defn ^:export init []
  (rum/mount (App) (.getElementById js/document "app")))
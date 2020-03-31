(ns literate.core
  (:require [rum.core :as rum :refer [defc]]
            ["vega-embed" :default vega-embed]))

(defc Code [code]
  [:code.p-3.bg-yellow-100.rounded code])

(defc VegaLite < {:did-mount (fn [state]
                               (let [[vega-lite-spec] (:rum/args state)]
                                 (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                               state)}
  [vega-lite-spec]
  [:div.w-screen])


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
                                  :code "(f x)"}

                      #:literate {:type :literate.type/vega-lite
                                  :vega-lite-spec {"$schema" "https://vega.github.io/schema/vega-lite/v2.0.json"
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
                                                                  :type "quantitative"}}}}]))

(defc App < rum/reactive []
  (into [:div.flex.flex-col.p-10.rounded

         [:span.text-2xl.mb-10
          {:style {:font-family "Cinzel"}}
          "Literate"]]

        (for [cell (rum/react state-ref)]
          [:div.relative.flex.bg-white.p-10.mb-10.shadow
           ;; Cell type
           [:span.absolute.top-0.mt-3.right-0.mr-3.text-xs.tracking-wider.uppercase.text-gray-500
            (name (:literate/type cell))]

           (render cell)])))

(defn ^:export init []
  (rum/mount (App) (.getElementById js/document "app")))
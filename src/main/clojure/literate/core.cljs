(ns literate.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]
            ["vega-embed" :default vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]))


(let [token (when-let [el (.getElementById js/document "sente-csrf-token")]
              (.getAttribute el "data-csrf-token"))

      {:keys [chsk
              ch-recv
              send-fn
              state]}
      (sente/make-channel-socket! "/chsk" token {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))


;; ---


(defc Code < {:did-mount (fn [state]
                           (let [[code] (:rum/args state)]
                             (codemirror (rum/dom-node state) #js {"value" code
                                                                   "mode" "clojure"
                                                                   "lineNumbers" false}))

                           state)}
  [code]
  [:div.w-screen])

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
  (into [:div.flex.flex-col.p-10

         [:span.absolute.top-0.left-0.text-lg.text-gray-700.px-5.py-2
          {:style {:font-family "Cinzel"}}
          "Literate"]]

        (for [cell (rum/react state-ref)]
          [:div.shadow.mb-6.rounded

           [:div.flex.rounded-t.border-b-2
            [:span.font-mono.font-semibold.text-xs.uppercase.text-black.rounded-t.py-1.px-3
             (name (:literate/type cell))]]

           [:div.flex.bg-white.p-2.rounded-b
            (render cell)]])))

(defn ^:export init []
  (sente/start-client-chsk-router! ch-chsk (fn [message]
                                             (js/console.log (select-keys message [:id :event :?data]))))

  (rum/mount (App) (.getElementById js/document "app")))
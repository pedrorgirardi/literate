(ns literate.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]
            ["vega-embed" :default vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]))


(defonce state-ref (atom {:literate/literates []}))

(defn literates [state]
  (:literate/literates state))

(defn add-literate [state literate]
  (update state :literate/literates conj literate))


;; ---


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

(def sente-router-ref (atom (fn [] nil)))


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


(defc App < rum/reactive []
  (into [:div.flex.flex-col.p-10

         [:span.absolute.top-0.left-0.text-lg.text-gray-700.px-5.py-2
          {:style {:font-family "Cinzel"}}
          "Literate"]]

        (map-indexed
          (fn [index literate]
            [:div.shadow.mb-6.rounded

             [:div.flex.items-center.justify-between.rounded-t.border-b-2.px-3.py-1
              [:span.font-mono.font-semibold.text-xs.uppercase.text-black.rounded-t
               (name (:literate/type literate))]

              [:i.zmdi.zmdi-close.text-gray-500.hover:text-red-700.cursor-pointer
               {:on-click #(js/console.log index)}]]

             [:div.flex.bg-white.p-2.rounded-b
              (render literate)]])
          (literates (rum/react state-ref)))))


(defn handler [{:keys [?data]}]
  (let [[event data] ?data]
    (when (= :literate/literate event)
      (swap! state-ref add-literate data))))


(defn ^:dev/before-load stop-sente-router []
  (@sente-router-ref))

(defn ^:export init []
  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (rum/mount (App) (.getElementById js/document "app")))
(ns literate.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]
            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]))


(defonce state-ref (atom {:literate/literates {}}))

(defn literates [state]
  (vals (:literate/literates state)))

(defn add-literate [state {:literate/keys [uuid] :as literate}]
  (assoc-in state [:literate/literates uuid] literate))

(defn remove-literate [state uuid]
  (update state :literate/literates dissoc uuid))


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
  [:div])

(defc VegaLite < {:did-mount (fn [state]
                               (let [[vega-lite-spec] (:rum/args state)]
                                 (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                               state)}
  [vega-lite-spec]
  [:div])

(defc Markdown
  [markdown]
  [:div.bg-white.px-2.py-1.font-thin {:dangerouslySetInnerHTML {:__html (marked markdown)}}])


;; ---


(defmulti render :literate/type)

(defmethod render :literate.type/code
  [{:literate/keys [code]}]
  (Code code))

(defmethod render :literate.type/vega-lite
  [{:literate/keys [vega-lite-spec]}]
  (VegaLite vega-lite-spec))

(defmethod render :literate.type/markdown
  [{:literate/keys [markdown]}]
  (Markdown markdown))


;; ---


(defc App < rum/reactive []
  (into [:div.flex.flex-col.pt-24

         [:div.flex.bg-white.border-b.border-gray-200.fixed.top-0.inset-x-0.z-100.h-16.items-center.justify-between.px-6
          [:span.text-lg.text-gray-700
           {:style {:font-family "Cinzel"}}
           "Literate"]

          [:div
           [:span.text-gray-600.hover:text-gray-900.cursor-default "Import"]
           [:span.text-gray-600.hover:text-gray-900.cursor-default.ml-4 "Export"]]]]

        (for [{:literate/keys [uuid] :as literate} (literates (rum/react state-ref))]
          [:div.flex.mb-6.shadow

           [:div.bg-gray-200.px-2.py-1
            [:div.rounded-full.hover:bg-gray-400.h-5.w-5.flex.items-center.justify-center
             {:on-click #(swap! state-ref remove-literate uuid)}
             [:i.zmdi.zmdi-close.text-gray-600]]]

           [:div.w-full
            (render literate)]])))


(defn handler [{:keys [?data]}]
  (let [[event data] ?data]
    (when (= :literate/literate event)
      (swap! state-ref add-literate data))))


(defn ^:dev/before-load stop-sente-router []
  (@sente-router-ref))

(defn ^:export init []
  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (rum/mount (App) (.getElementById js/document "app")))
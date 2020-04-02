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
  [:div.w-screen])

(defc VegaLite < {:did-mount (fn [state]
                               (let [[vega-lite-spec] (:rum/args state)]
                                 (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                               state)}
  [vega-lite-spec]
  [:div.w-screen])

(defc Markdown
  [markdown]
  [:div.font-thin {:dangerouslySetInnerHTML {:__html (marked markdown)}}])


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
  (into [:div.flex.flex-col.p-10.mt-10

         [:span.absolute.top-0.left-0.text-lg.text-gray-700.px-5.py-2
          {:style {:font-family "Cinzel"}}
          "Literate"]]

        (for [{:literate/keys [uuid] :as literate} (literates (rum/react state-ref))]
          [:div.mb-6

           [:div.flex.mb-2
            [:span.text-xs.uppercase.text-gray-500
             (name (:literate/type literate))]

            [:i.zmdi.zmdi-close.ml-4.text-gray-300.hover:text-gray-700.cursor-pointer
             {:on-click #(swap! state-ref remove-literate uuid)}]]

           [:div.flex
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
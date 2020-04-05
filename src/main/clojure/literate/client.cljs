(ns literate.client
  (:require [cljs.spec.alpha :as s]
            [cljs.pprint :as pprint]

            [literate.specs]
            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]))


(defonce state-ref (atom {:literate/snippets []}))

(add-watch state-ref ::state (fn [_ _ _ state]
                               (when-not (s/valid? :literate/client-state state)
                                 (js/console.error "Invalid state" (s/explain-str :literate/client-state state)))))

(defn snippets [state]
  (:literate/snippets state))

(defn add-snippet [state snippet]
  (update state :literate/snippets (fnil conj []) snippet))

(defn remove-snippet [state uuid]
  (update state :literate/snippets (fn [snippets]
                                     (filterv #(not= uuid (:snippet/uuid %)) snippets))))


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


(defmulti render :snippet/type)

(defmethod render :snippet.type/code
  [{:snippet/keys [code]}]
  (Code code))

(defmethod render :snippet.type/vega-lite
  [{:keys [:snippet/vega-lite-spec]}]
  (VegaLite vega-lite-spec))

(defmethod render :snippet.type/markdown
  [{:snippet/keys [markdown]}]
  (Markdown markdown))


;; ---


(defc App < rum/reactive []
  [:div.flex.flex-col.pt-24

   ;; -- Nav

   [:div.flex.bg-white.border-b.border-gray-200.fixed.top-0.inset-x-0.z-100.h-16.items-center.justify-between.px-6
    [:span.text-lg.text-gray-700
     {:style {:font-family "Cinzel"}}
     "Literate"]

    [:div
     [:span.text-gray-600.hover:text-gray-900.cursor-default "Import"]
     [:span.text-gray-600.hover:text-gray-900.cursor-default.ml-4 "Export"]]]


   ;; -- Literates

   (for [{:keys [:snippet/uuid] :as snippet} (snippets (rum/react state-ref))]
     [:div.flex.mb-6.shadow
      {:key (:snippet/uuid snippet)}

      [:div.bg-gray-200.px-2.py-1
       [:div.rounded-full.hover:bg-gray-400.h-5.w-5.flex.items-center.justify-center
        {:on-click #(swap! state-ref remove-snippet uuid)}
        [:i.zmdi.zmdi-close.text-gray-600]]]

      [:div.w-full
       (render snippet)]])


   ;; -- Debug

   [:div.fixed.bottom-0.right-0.mr-4.mb-4
    [:div.rounded-full.h-8.w-8.flex.items-center.justify-center.text-2xl.hover:bg-green-200
     {:on-click #(swap! state-ref add-snippet #:snippet {:uuid (str (random-uuid))
                                                         :type :snippet.type/code
                                                         :code (with-out-str (pprint/pprint @state-ref))})}
     [:i.zmdi.zmdi-bug.text-green-500]]]])


(defn handler [{:keys [?data] :as m}]
  (let [[event data] ?data]
    (js/console.group "Sente")
    (js/console.log (select-keys m [:id :?data]))
    (js/console.table (clj->js (:?data m)))
    (js/console.groupEnd)

    (when (= :literate/snippet event)
      (swap! state-ref add-snippet data))))


(defn ^:dev/before-load stop-sente-router []
  (@sente-router-ref))

(defn ^:export init []
  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (rum/mount (App) (.getElementById js/document "app")))
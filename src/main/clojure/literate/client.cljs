(ns literate.client
  (:require [cljs.spec.alpha :as s]
            [cljs.pprint :as pprint]

            [literate.db :as db]
            [literate.specs]

            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]
            [datascript.core :as d]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
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

(defc Html
  [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])

(defc Card [{:card/keys [snippets]}]
  [:div.w-full
   (for [{:snippet/keys [uuid type code markdown vega-lite-spec html]} snippets]
     [:div.mb-4 {:key uuid} (case type
                              :snippet.type/code
                              (Code code)

                              :snippet.type/markdown
                              (Markdown markdown)

                              :snippet.type/vega-lite
                              (VegaLite vega-lite-spec)

                              :snippet.type/hiccup
                              (Html html)

                              :snippet.type/html
                              (Html html)

                              [:div [:span "Unknown Snippet type " [:code (str type)]]])])])

;; ---


(defc App []
  [:div.flex.flex-col.pt-24

   ;; -- Nav

   [:div.flex.bg-white.border-b.border-gray-200.fixed.top-0.inset-x-0.z-100.h-16.items-center.justify-between.px-6
    [:span.text-lg.text-gray-700
     {:style {:font-family "Cinzel"}}
     "Literate"]]


   ;; -- Literates

   (for [{:db/keys [id] :as card} (db/all-cards)]
     [:div.flex.mb-6.shadow
      {:key id}

      [:div.bg-gray-200.px-2.py-1
       [:div.rounded-full.hover:bg-gray-400.h-5.w-5.flex.items-center.justify-center
        {:on-click #(db/retract-entity id)}
        [:i.zmdi.zmdi-close.text-gray-600]]]

      (Card card)])


   ;; -- Debug

   [:div.fixed.bottom-0.right-0.mr-4.mb-4
    [:div.rounded-full.h-8.w-8.flex.items-center.justify-center.text-2xl.hover:bg-green-200
     {:on-click #(d/transact! db/conn [#:card {:uuid (str (random-uuid))
                                               :snippets
                                               [#:snippet {:uuid (str (random-uuid))
                                                           :type :snippet.type/code
                                                           :code (with-out-str (pprint/pprint (db/all-cards)))}]}])}
     [:i.zmdi.zmdi-bug.text-green-500]]]])


(defn handler [{:keys [?data] :as m}]
  (let [[event data] ?data]
    (js/console.group "Sente")
    (js/console.log (select-keys m [:id :?data]))
    (js/console.groupEnd)

    (when (= :literate/!transact event)
      (d/transact! db/conn data))))

(defn mount []
  (rum/mount (App) (.getElementById js/document "app")))

(defn ^:dev/before-load stop-sente-router []
  (@sente-router-ref))

(defn ^:export init []
  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (d/listen! db/conn (fn [_]
                       (js/console.log "Will re-render...")

                       (mount)))

  (mount))
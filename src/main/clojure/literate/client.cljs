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
            ["leaflet" :as leaflet]
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


(defc Code < {:did-mount
              (fn [state]
                (let [{:snippet/keys [code]} (first (:rum/args state))]
                  (codemirror (rum/dom-node state) #js {"value" code
                                                        "mode" "clojure"
                                                        "lineNumbers" false}))

                state)}
  [_]
  [:div])

(defc VegaLite < {:did-mount
                  (fn [state]
                    (let [{:snippet/keys [vega-lite-spec]} (first (:rum/args state))]
                      (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                    state)}
  [_]
  [:div])

(defc Markdown
  [e]
  [:div.bg-white.px-2.py-1.font-thin
   {:dangerouslySetInnerHTML
    {:__html (marked (:snippet/markdown e))}}])

(defc Html
  [e]
  [:div
   {:dangerouslySetInnerHTML
    {:__html (:snippet/html e)}}])

(defc Leaflet < {:did-mount
                 (fn [state]
                   (let [[{:snippet/keys [center zoom geojson]}] (:rum/args state)

                         M (-> leaflet
                               (.map (rum/dom-node state))
                               (.setView (clj->js center) zoom))

                         tile-url-template "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                         tile-options #js {:attribution "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors"}
                         tile-layer (.tileLayer leaflet tile-url-template tile-options)

                         ;; GeoJSON (optional)
                         geojson-layer (when geojson
                                         (.geoJSON leaflet (clj->js geojson)))]

                     (.addTo tile-layer M)

                     (when geojson-layer
                       (.addTo geojson-layer M))

                     (assoc state ::M M
                                  ::tile-layer tile-layer
                                  ::geojson-layer geojson-layer)))

                 :did-update
                 (fn [state]
                   (let [{M ::M
                          geojson-layer ::geojson-layer
                          args :rum/args} state

                         {:snippet/keys [center zoom geojson]} (first args)

                         geojson-layer' (when geojson
                                          (.geoJSON leaflet (clj->js geojson)))]

                     ;; Remove old GeoJSON layer.
                     (when geojson-layer
                       (.removeLayer M geojson-layer))

                     ;; Add GeoJSON layer.
                     (when geojson-layer'
                       (.addTo geojson-layer' M))

                     (assoc state ::geojson-layer geojson-layer')))}
  [snippet]
  [:div
   {:style
    {:height (or (get-in snippet [:snippet/style :height]) "320px")}}])

(defc Card [{:card/keys [snippets]}]
  [:div.w-full.flex.flex-col.space-y-6
   (for [{:snippet/keys [uuid type] :as snippet} snippets]
     [:div {:key uuid}
      (case type
        :snippet.type/code
        (Code snippet)

        :snippet.type/markdown
        (Markdown snippet)

        :snippet.type/vega-lite
        (VegaLite snippet)

        :snippet.type/hiccup
        (Html snippet)

        :snippet.type/html
        (Html snippet)

        :snippet.type/leaflet
        (Leaflet snippet)

        [:div [:span "Unknown Snippet type " [:code (str type)]]])])])


;; ---


(defc App []
  [:div.flex.flex-col

   ;; -- Header

   [:span.text-lg.text-gray-700.py-6
    {:style {:font-family "Cinzel"}}
    "Literate"]


   ;; -- Cards

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
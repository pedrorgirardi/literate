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
                (let [{:widget/keys [code]} (first (:rum/args state))]
                  (codemirror (rum/dom-node state) #js {"value" code
                                                        "mode" "clojure"
                                                        "lineNumbers" false}))

                state)}
  [_]
  [:div.flex-1])

(defc VegaLite < {:did-mount
                  (fn [state]
                    (let [{:widget/keys [vega-lite-spec]} (first (:rum/args state))]
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
                   (let [[{:widget/keys [center zoom geojson]}] (:rum/args state)

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

                         {:widget/keys [center zoom geojson]} (first args)

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
  [:div.flex-1
   {:style
    {:height (or (get-in snippet [:widget/style :height]) "320px")}}])

(declare Widget)

(defc Row
  [e]
  [:div.flex.flex-1.space-x-2
   (for [child (:widget/children e)]
     [:div.flex-1 (Widget child)])])

(defc Widget [e]
  (let [{widget-uuid :widget/uuid
         widget-type :widget/type} e

        component (case widget-type
                    :widget.type/row
                    (Row e)

                    :widget.type/vega-lite
                    (VegaLite e)

                    :widget.type/code
                    (Code e)

                    :snippet.type/markdown
                    (Markdown e)

                    :snippet.type/hiccup
                    (Html e)

                    :snippet.type/html
                    (Html e)

                    :widget.type/leaflet
                    (Leaflet e)

                    [:div [:span "Unknown Widget type " [:code (str widget-type)]]])]

    (rum/with-key component widget-uuid)))


;; ---


(defc App []
  [:div.flex.flex-col

   ;; -- Header

   [:span.text-lg.text-gray-700.py-6
    {:style {:font-family "Cinzel"}}
    "Literate"]


   ;; -- Widgets

   (for [{:db/keys [id] :as e} (db/root-widgets) :let [_ (js/console.log "Root Widget" e)]]
     [:div.flex.mb-6.shadow
      {:key id}

      [:div.bg-gray-200.px-2.py-1
       [:div.rounded-full.hover:bg-gray-400.h-5.w-5.flex.items-center.justify-center
        {:on-click #(db/retract-entity id)}
        [:i.zmdi.zmdi-close.text-gray-600]]]

      (Widget e)])


   ;; -- Debug

   [:div.fixed.bottom-0.right-0.mr-4.mb-4
    [:div.rounded-full.h-8.w-8.flex.items-center.justify-center.text-2xl.hover:bg-green-200
     {:on-click #(d/transact! db/conn [#:widget {:uuid (str (random-uuid))
                                                 :type :widget.type/code
                                                 :code (with-out-str (pprint/pprint (db/all-widgets)))}])}
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
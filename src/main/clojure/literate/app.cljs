(ns literate.app
  (:require [cljs.spec.alpha :as s]
            [cljs.pprint :as pprint]

            [literate.db :as db]
            [literate.specs]

            [taoensso.sente :as sente :refer (cb-success?)]
            [rum.core :as rum :refer [defc]]
            [datascript.core :as d]
            [reagent.core :as r]
            [reagent.dom :as dom]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]

            ["ol/Map" :default Map]
            ["ol/View" :default View]
            ["ol/format/WKT" :default WKT]
            ["ol/source" :as ol-source]
            ["ol/layer" :as ol-layer]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" nil {:type :auto})]
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
  [:div.w-full])

(defc VegaLite < {:did-mount
                  (fn [state]
                    (let [{:widget/keys [vega-lite-spec]} (first (:rum/args state))]
                      (vega-embed (rum/dom-node state) (clj->js vega-lite-spec)))

                    state)}
  [_]
  [:div])

(defn Markdown
  [e]
  [:div.flex-1.bg-white.px-3.py-1.font-thin
   {:dangerouslySetInnerHTML
    {:__html (marked (:widget/markdown e))}}])

(defn Html
  [e]
  [:div.flex-1
   {:dangerouslySetInnerHTML
    {:__html (:widget/html e)}}])


(defn Geoplot [{geoplot-height :widget.geoplot/height
                geoplot-wkt :widget.geoplot/wkt
                geoplot-center :widget.geoplot/center
                geoplot-zoom :widget.geoplot/zoom}]
  [:div.w-full
   {:style {:height (or geoplot-height "500px")}
    :ref
    (fn [e]
      (when e
        (let [feature (.readFeature (WKT.) geoplot-wkt #js {"dataProjection" "EPSG:4326"
                                                            "featureProjection" "EPSG:3857"})]

          (Map. #js {:target e

                     :layers
                     #js [(ol-layer/Tile. #js {:source (ol-source/OSM.)})

                          (ol-layer/Vector. #js {:source (ol-source/Vector. #js {"features" #js [feature]})})]

                     :view (View. #js {:center (clj->js geoplot-center)
                                       :zoom (or geoplot-zoom 4)})}))))}])

(declare Widget)

(defc Row
  [e]
  [:div.flex.flex-1.space-x-2
   (for [child (:widget/children e)]
     [:div.flex-1
      {:key (:widget/uuid child)}
      (Widget child)])])

(defc Column
  [e]
  [:div.flex.flex-col.flex-1.space-y-2
   (for [child (:widget/children e)]
     [:div.flex-1
      {:key (:widget/uuid child)}
      (Widget child)])])

(defn Widget [e]
  (let [{widget-uuid :widget/uuid
         widget-type :widget/type} e

        Component (case widget-type
                    :widget.type/html
                    Html

                    :widget.type/row
                    Row

                    :widget.type/column
                    Column

                    :widget.type/vega-lite
                    VegaLite

                    :widget.type/code
                    Code

                    :widget.type/markdown
                    Markdown

                    :widget.type/geoplot
                    Geoplot

                    [:div.p-2 [:span "Unknown Widget type " [:code (str widget-type)]]])]

    (js/console.log "Render" (name widget-type))

    ^{:key widget-uuid}
    [Component e]))


;; ---


(defn App [widgets]
  [:div.flex.flex-col

   ;; -- Header

   [:span.text-lg.text-gray-700.py-6
    {:style {:font-family "Cinzel"}}
    "Literate"]


   ;; -- Widgets

   (for [{:db/keys [id] :as e} widgets]
     [:div.flex.flex-col.p-1.mb-6.border-l-2.border-transparent.hover:border-blue-500
      {:key id}

      [:div.text-gray-600.rounded.bg-gray-200.hover:bg-gray-300.h-5.w-5.flex.items-center.justify-center.mb-1.cursor-pointer
       {:on-click #(db/retract-entity id)}
       [:i.zmdi.zmdi-close]]

      [:div.flex.flex-1.overflow-x-auto
       (Widget e)]])


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
  (let [widgets (sort-by :db/id (db/root-widgets))]
    (dom/render [App widgets] (.getElementById js/document "app"))))

(defn ^:dev/before-load stop-sente-router []
  (@sente-router-ref))

(defn ^:export init []
  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (d/listen! db/conn (fn [_]
                       (js/console.log "Will re-render...")

                       (mount)))

  (mount))
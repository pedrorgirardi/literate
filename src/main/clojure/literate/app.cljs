(ns literate.app
  (:require [cljs.spec.alpha :as s]
            [cljs.pprint :as pprint]
            [clojure.string :as str]

            [literate.db :as db]
            [literate.specs]

            [taoensso.sente :as sente]
            [datascript.core :as d]
            [reagent.dom :as dom]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]

            ["ol/Map" :default Map]
            ["ol/View" :default View]
            ["ol/format/WKT" :default WKT]
            ["ol/source" :as ol-source]
            ["ol/layer" :as ol-layer]
            ["ol/proj" :as ol-proj]
            ["ol/color" :as ol-color]
            ["ol/style" :as ol-style]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" nil {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(def sente-router-ref (atom (fn [] nil)))


;; ---


(defn Code [{:widget/keys [code]}]
  [:div.w-full
   {:ref
    (fn [e]
      (when e
        (codemirror e #js {"value" code
                           "mode" "clojure"
                           "lineNumbers" false})))}])

(defn VegaLite [{:widget/keys [vega-lite-spec]}]
  [:div
   {:ref
    (fn [e]
      (when e
        (vega-embed e (clj->js vega-lite-spec))))}])

(defn Markdown
  [e]
  [:div.flex-1.bg-white.px-3.py-1.font-thin
   {:dangerouslySetInnerHTML
    {:__html (marked (:widget/markdown e))}}])

(defn Html
  [e]
  [:div.flex-1
   {:dangerouslySetInnerHTML
    {:__html (:widget.html/src e)}}])


(defn Geoplot [{geoplot-height :widget.geoplot/height
                geoplot-wkt :widget.geoplot/wkt
                geoplot-style :widget.geoplot/style
                geoplot-center :widget.geoplot/center
                geoplot-center-wsg84? :widget.geoplot/center-wsg84?
                geoplot-zoom :widget.geoplot/zoom
                geoplot-features :widget.geoplot/features}]
  [:div.w-full
   {:style {:height (or geoplot-height "500px")}
    :ref
    (fn [e]
      (when e
        (let [feature (.readFeature (WKT.) geoplot-wkt #js {"dataProjection" "EPSG:4326"
                                                            "featureProjection" "EPSG:3857"})

              geoplot-center-js (some-> geoplot-center clj->js)

              ;; Provide the coordinates projected into Web Mercator - if center is in WSG84.
              geoplot-center-js (if geoplot-center-wsg84?
                                  (doto (some-> geoplot-center-js ol-proj/fromLonLat)
                                    (#(js/console.log (str "Center projection: " (str/join " " geoplot-center) " (WGS84) to " % " (EPSG:3857)"))))
                                  geoplot-center-js)

              geoplot-center-js (or geoplot-center-js #js [0 0])

              style (when geoplot-style
                      (ol-style/Style.
                        (clj->js
                          (merge {}
                                 ;; Fill color.
                                 (when-let [color (get-in geoplot-style [:fill :color])]
                                   {:fill (ol-style/Fill. #js {:color (ol-color/asString color)})})

                                 ;; Stroke color.
                                 (when-let [color (get-in geoplot-style [:stroke :color])]
                                   {:stroke (ol-style/Stroke. #js {:color (ol-color/asString color)})})))))

              wkt-source (ol-source/Vector. #js {"features" #js [feature]})

              wkt-layer (ol-layer/Vector.
                          (clj->js (merge {:source wkt-source}
                                          (when style
                                            {:style style}))))]

          (Map. #js {:target e

                     :layers
                     #js [(ol-layer/Tile. #js {:source (ol-source/OSM.)})

                          wkt-layer]

                     :view (View. #js {:center geoplot-center-js
                                       :zoom (or geoplot-zoom 4)})}))))}])

(declare Widget)

(defn Row [e]
  [:div.flex.flex-1.space-x-2
   (for [child (:widget/children e)]
     [:div.flex-1
      {:key (:widget/uuid child)}
      [Widget child]])])

(defn Column [e]
  [:div.flex.flex-col.flex-1.space-y-2
   (for [child (:widget/children e)]
     [:div.flex-1
      {:key (:widget/uuid child)}
      [Widget child]])])

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
  [:div.h-screen.flex.flex-col

   ;; -- Header

   [:span.text-lg.text-gray-700.py-6.px-1
    {:style {:font-family "Cinzel"}}
    "Literate"]


   ;; -- Widgets

   (if (seq widgets)
     (for [{:db/keys [id] :as e} widgets]
       [:div.flex.flex-col.p-1.mb-6.border-l-2.border-transparent.hover:border-blue-500
        {:key id}

        [:div.text-gray-600.rounded.bg-gray-200.hover:bg-gray-300.h-5.w-5.flex.items-center.justify-center.mb-1.cursor-pointer
         {:on-click #(db/retract-entity id)}
         [:i.zmdi.zmdi-close]]

        [:div.flex.flex-1.overflow-x-auto
         (Widget e)]])
     [:div.flex.flex-col.flex-1.items-center.justify-center
      [:span.text-lg.text-gray-400
       {:style {:font-family "Cinzel"}}
       "Widgets shall be displayed here."]])


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
  (js/console.log "Welcome to Literate")

  (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler))

  (d/listen! db/conn (fn [_]
                       (js/console.log "Will re-render...")

                       (mount)))

  (mount))
(ns literate.app
  (:require [cljs.spec.alpha :as s]
            [cljs.pprint :as pprint]
            [clojure.string :as str]

            [cognitect.transit :as t]

            [taoensso.sente :as sente]
            [datascript.core :as d]
            [reagent.core :as r]
            [reagent.dom :as dom]

            [literate.db :as db]
            [literate.specs]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/mode/gfm/gfm"]
            ["file-saver" :as FileSaver]

            ["ol/Map" :default Map]
            ["ol/View" :default View]
            ["ol/format/WKT" :default WKT]
            ["ol/source" :as ol-source]
            ["ol/layer" :as ol-layer]
            ["ol/proj" :as ol-proj]
            ["ol/color" :as ol-color]
            ["ol/style" :as ol-style]))

;; WebSocket is only available in 'dev mode' - that's when we're authoring the document.
(goog-define ^boolean WS false)

(def transit-json-reader (t/reader :json))
(def transit-json-writer (t/writer :json))

(when WS
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client! "/chsk" nil {:type :auto})]
    (def chsk chsk)
    (def ch-chsk ch-recv)
    (def chsk-send! send-fn)
    (def chsk-state state)))

(def sente-router-ref (atom (fn [] nil)))


;; ---

(defn reagent-class-component [render rmount]
  (r/create-class
    {:reagent-render
     render

     :component-did-mount
     (fn [this]
       (rmount this (dom/dom-node this)))}))


(defn Codemirror [{:widget.codemirror/keys [height
                                            width
                                            value
                                            mode
                                            lineNumbers]}]
  (reagent-class-component
    (fn [_]
      [:div])

    (fn [_ node]
      (doto (codemirror node #js {"value" value
                                  "mode" mode
                                  "lineNumbers" lineNumbers})
        (.setSize width height)))))

(defn VegaEmbed [{:widget.vega-embed/keys [spec]}]
  (reagent-class-component
    (fn [_]
      [:div])

    (fn [_ node]
      (vega-embed node (clj->js spec)))))

(defn Markdown
  [e]
  [:div.flex-1.bg-white.px-3.py-1.font-thin
   {:dangerouslySetInnerHTML
    {:__html (marked (:widget/markdown e))}}])

(defn Html
  [e]
  [:div
   {:dangerouslySetInnerHTML
    {:__html (:widget.html/src e)}}])


(defn geoplot-feature-js [feature]
  (let [{:geoplot.feature/keys [wkt
                                data-projection
                                feature-projection
                                style]} feature]
    (cond
      wkt
      (let [options (merge {}
                           (when data-projection
                             {"dataProjection" data-projection})

                           (when feature-projection
                             {"featureProjection" feature-projection}))

            feature (.readFeature (WKT.) wkt (clj->js options))]

        (when style
          (.setStyle feature (ol-style/Style.
                               (clj->js
                                 (merge {}
                                        ;; Fill color.
                                        (when-let [color (get-in style [:geoplot.style/fill :geoplot/color])]
                                          {:fill (ol-style/Fill. #js {:color (ol-color/asString color)})})

                                        ;; Stroke color
                                        (when-let [color (get-in style [:geoplot.style/stroke :geoplot/color])]
                                          {:stroke (ol-style/Stroke. #js {:color (ol-color/asString color)})}))))))

        feature)

      :else
      nil)))

(defn Geoplot [{geoplot-height :widget.geoplot/height
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
        (let [geoplot-center-js (some-> geoplot-center clj->js)

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

              source (ol-source/Vector. (clj->js {"features" (mapv geoplot-feature-js geoplot-features)}))

              layer (ol-layer/Vector.
                      (clj->js (merge {:source source}
                                      (when style
                                        {:style style}))))]

          (Map. #js {:target e

                     :layers
                     #js [(ol-layer/Tile. #js {:source (ol-source/OSM.)})

                          layer]

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

                    :widget.type/vega-embed
                    VegaEmbed

                    :widget.type/codemirror
                    Codemirror

                    :widget.type/markdown
                    Markdown

                    :widget.type/geoplot
                    Geoplot

                    [:div.p-2 [:span "Unknown Widget type " [:code (str widget-type)]]])]
    ^{:key widget-uuid}
    [Component e]))


;; ---

(defn Import []
  [:div
   {:class "flex justify-center items-center w-1/2 h-1/3 border-4 border-dashed rounded"}
   [:input
    {:type "file"
     :on-change
     (fn [e]
       (when-some [f (-> e .-target .-files first)]
         (let [reader (js/FileReader.)]
           (js/console.log "Import..." f)

           (set! (.-onload reader) (fn [e]
                                     (let [datoms (t/read transit-json-reader (-> e .-target .-result))
                                           datoms (mapv
                                                    (fn [datom]
                                                      (apply d/datom datom))
                                                    datoms)]
                                       (d/reset-conn! db/conn (d/init-db datoms db/schema)))))

           (set! (.-onerror reader) (fn [e]
                                      (js/console.log (-> e .-target .-error))))

           (.readAsText reader f))))}]])


(defn IconDocumentDownload [& [attrs]]
  [:svg.w-6.h-6
   (merge {:fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg"} attrs)
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}]])

(defn App [widgets]
  [:div.h-screen.flex.flex-col

   ;; -- Header

   [:div.flex.justify-between.items-center.py-6.px-1
    [:span.text-lg.text-gray-700
     {:style {:font-family "Cinzel"}}
     "Literate"]

    ;; -- Export
    [:div.flex
     [:button
      {:class "inline-flex items-center px-3 py-2 border border-transparent shadow-sm text-sm leading-4 font-medium rounded-md text-white bg-teal-600 hover:bg-teal-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500"
       :on-click #(let [encoded (t/write
                                  transit-json-writer
                                  (map
                                    (fn [{:keys [e a v]}]
                                      [e a v])
                                    (d/datoms @db/conn :eavt)))

                        blob (js/Blob. #js [encoded] #js {"type" "application/transit+json"})]

                    (FileSaver/saveAs blob "widgets.json"))}

      [IconDocumentDownload {:class "mr-2"}]
      "Download"]]]


   ;; -- Widgets

   (if (seq widgets)
     [:div.flex.flex-col.items-start.overflow-auto
      (for [{:db/keys [id] :as e} widgets]
        [:div.flex.flex-col.p-1.mb-6.border-l-2.border-transparent.hover:border-blue-500.last:mb-36
         {:key id}

         [:div.text-gray-600.rounded.bg-gray-200.hover:bg-gray-300.h-5.w-5.flex.items-center.justify-center.mb-1.cursor-pointer
          {:on-click #(db/retract-entity id)}
          [:i.zmdi.zmdi-close]]

         (Widget e)])]
     [:div.flex.flex-col.flex-1.items-center.justify-center
      [Import]])


   ;; -- Debug

   [:div.fixed.bottom-0.right-0.mr-4.mb-4
    [:div.rounded-full.h-8.w-8.flex.items-center.justify-center.text-2xl.hover:bg-green-200
     {:on-click #(d/transact! db/conn [{:widget/uuid (str (random-uuid))
                                        :widget/type :widget.type/codemirror
                                        :widget.codemirror/mode "clojure"
                                        :widget.codemirror/height "auto"
                                        :widget.codemirror/lineNumbers false
                                        :widget.codemirror/value (with-out-str (pprint/pprint (db/all-widgets)))}])}
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
  (js/console.log "Welcome to Literate" (if goog.DEBUG
                                          "(Dev build)"
                                          "(Release build)"))

  (when WS
    (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler)))

  (d/listen! db/conn #(mount))

  (mount))
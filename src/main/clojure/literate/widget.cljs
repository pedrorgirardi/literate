(ns literate.widget
  (:require [clojure.string :as str]

            [reagent.core :as r]
            [reagent.dom :as dom]

            [literate.specs]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/mode/gfm/gfm"]
            ["react-window" :as react-window]

            ["ol/Map" :default Map]
            ["ol/View" :default View]
            ["ol/format/WKT" :default WKT]
            ["ol/source" :as ol-source]
            ["ol/layer" :as ol-layer]
            ["ol/proj" :as ol-proj]
            ["ol/color" :as ol-color]
            ["ol/style" :as ol-style]))

(defn Table [{:widget.table/keys [height
                                  width
                                  row-height
                                  columns
                                  rows]}]
  [:div.flex.flex-col.text-sm

   ;; -- Header
   [:div.flex.border-b.border-teal-500.pb-1.mb-1
    (for [[_ column-label] columns]
      ^{:key column-label}
      [:div.flex-1
       [:span.font-bold column-label]])]

   ;; -- Body
   [:> react-window/FixedSizeList
    {:height height
     :width width
     :itemSize row-height
     :itemCount (count rows)}
    (r/reactify-component
      (fn [{:keys [index style]}]
        [:div.flex.hover:bg-teal-50
         {:style (js->clj style)}
         (let [row (nth rows index nil)]
           (for [[column-key column-label] columns]
             ^{:key column-label}
             [:div.flex-1
              [:span (if (vector? column-key)
                       (get-in row column-key)
                       (get row column-key))]]))]))]])

(defn Codemirror [{:widget.codemirror/keys [height
                                            width
                                            value
                                            mode
                                            lineNumbers]}]
  (r/create-class
    {:reagent-render
     (fn [_]
       [:div])

     :component-did-mount
     (fn [this]
       (doto (codemirror (dom/dom-node this) #js {"value" value
                                                  "mode" mode
                                                  "lineNumbers" lineNumbers})
         (.setSize width height)))}))

(defn VegaEmbed [{:widget.vega-embed/keys [spec]}]
  (r/create-class
    {:reagent-render
     (fn [_]
       [:div])

     :component-did-mount
     (fn [this]
       (vega-embed (dom/dom-node this) (clj->js spec)))}))

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
                geoplot-width :widget.geoplot/width
                geoplot-style :widget.geoplot/style
                geoplot-center :widget.geoplot/center
                geoplot-center-wsg84? :widget.geoplot/center-wsg84?
                geoplot-zoom :widget.geoplot/zoom
                geoplot-features :widget.geoplot/features}]
  
  (r/create-class
    {:reagent-render
     (fn [_]
       [:div
        {:style
         {:height (or geoplot-height "500px")
          :width (or geoplot-width "1200px")}}])
     
     :component-did-mount
     (fn [this]
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
                                  {:style style}))))
             
             esri-world-imagery #js {:url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                                     :attributions "Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}
             
             esri-world-imagery-source (ol-source/XYZ. esri-world-imagery)
             
             #_#_osm-source (ol-source/OSM.)]
         
         (Map. #js {:target (dom/dom-node this)
                    
                    ;; TODO: UI to switch layers.
                    :layers
                    #js [(ol-layer/Tile. #js {:source esri-world-imagery-source})
                         
                         layer]
                    
                    :view (View. #js {:center geoplot-center-js
                                      :zoom (or geoplot-zoom 4)})})))}))

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

                    :widget.type/table
                    Table

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

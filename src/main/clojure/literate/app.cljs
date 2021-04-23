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
            [literate.widget :as widget]
            [literate.specs]

            ["marked" :as marked]
            ["vega-embed" :as vega-embed]
            ["codemirror" :as codemirror]
            ["codemirror/mode/clojure/clojure"]
            ["codemirror/mode/gfm/gfm"]
            ["file-saver" :as FileSaver]
            ["react-tippy" :as tippy]

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


(defn IconClose [& [attrs]]
  [:svg
   (merge {:fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg"} attrs)
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M6 18L18 6M6 6l12 12"}]])

(defn IconDocumentDownload [& [attrs]]
  [:svg.w-6.h-6
   (merge {:fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg"} attrs)
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}]])

(defn IconDatabase [& [attrs]]
  [:svg.w-6.h-6
   (merge {:fill "none" :stroke "currentColor" :viewBox "0 0 24 24" :xmlns "http://www.w3.org/2000/svg"} attrs)
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"}]])


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

(defn WidgetContainer [e]
  (r/with-let [mouse-over-ref? (r/atom false)]
    [:div.flex.space-x-2.mb-2.border-l-2.border-transparent.hover:border-teal-500.transition.duration-200.ease-in-out
     {:on-mouse-over #(reset! mouse-over-ref? true)
      :on-mouse-leave #(reset! mouse-over-ref? false)}

     [:div.flex.flex-col
      {:class (when @mouse-over-ref? "bg-teal-50")}
      [:button.text-gray-600.rounded.bg-gray-200.hover:bg-gray-300.h-5.w-5.flex.items-center.justify-center.m-1.focus:outline-none
       {:class (when-not @mouse-over-ref? "invisible")
        :on-click #(db/retract-entity (:db/id e))}
       [IconClose {:class "w-5 h-5"}]]]

     [widget/Widget e]]))

(defn App [widgets]
  [:div.h-screen.flex.flex-col

   ;; -- Header

   [:div.flex.justify-between.items-center.px-10.py-2.border-b.h-14
    [:span.text-lg.text-gray-700
     {:style {:font-family "Cinzel"}}
     "Literate"]

    [:div.flex.space-x-2

     ;; -- Export
     [:> tippy/Tooltip
      {:title "Download document"
       :size "small"}
      [:button
       {:key "download"
        :class [(if (empty? widgets)
                  "text-gray-300 pointer-events-none"
                  "text-gray-600")
                "inline-flex items-center px-3 py-2 border hover:text-gray-800 rounded-md hover:shadow-md hover:bg-gray-100 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition duration-200 ease-in-out"]
        :on-click #(let [encoded (t/write
                                   transit-json-writer
                                   (map
                                     (fn [{:keys [e a v]}]
                                       [e a v])
                                     (d/datoms @db/conn :eavt)))

                         blob (js/Blob. #js [encoded] #js {"type" "application/transit+json"})]

                     (FileSaver/saveAs blob "widgets.json"))}

       [IconDocumentDownload]]]

     ;; -- Database
     [:> tippy/Tooltip
      {:title "View database"
       :size "small"}
      [:button
       {:key "database"
        :class "inline-flex items-center px-3 py-2 border text-gray-600 hover:text-gray-800 rounded-md hover:shadow-md hover:bg-gray-100 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition duration-200 ease-in-out"
        :on-click #(d/transact! db/conn [{:widget/uuid (str (random-uuid))
                                          :widget/type :widget.type/codemirror
                                          :widget.codemirror/mode "clojure"
                                          :widget.codemirror/lineNumbers false
                                          :widget.codemirror/value (with-out-str (pprint/pprint (db/all-widgets)))}])}

       [IconDatabase]]]]]


   ;; -- Widgets
   (if (seq widgets)
     [:div.overflow-auto
      [:div.flex.flex-col.items-start.container.mx-auto.py-2
       (for [e widgets]
         ^{:key (:db/id e)}
         [WidgetContainer e])]]
     [:div.flex.flex-col.flex-1.items-center.justify-center
      [Import]])])


(defn handler [{:keys [?data] :as m}]
  (let [[event data] ?data]
    ;; WebSocket logging.
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
  (js/console.log "Welcome to Literate"
    (if goog.DEBUG
      "(dev build)"
      "(release build)"))

  (when WS
    (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler)))

  ;; Rerender UI whenever the database changes.
  (d/listen! db/conn #(mount))

  (mount))
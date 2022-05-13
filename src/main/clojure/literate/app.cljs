(ns literate.app
  (:require 
   [cljs.pprint :as pprint]
   
   [cognitect.transit :as t]
   
   [taoensso.sente :as sente]
   [datascript.core :as d]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   
   [literate.db :as db]
   [literate.widget :as widget]
   [literate.specs]
   
   ["codemirror/mode/clojure/clojure"]
   ["codemirror/mode/gfm/gfm"]
   ["file-saver" :as FileSaver]
   ["react-tippy" :as tippy]))

(goog-define ^boolean WS true)

(def transit-json-reader (t/reader :json))
(def transit-json-writer (t/writer :json))

(defonce state-ref (r/atom {}))

(when WS
  (let [{:keys [chsk ch-recv]}
        (sente/make-channel-socket-client! "/chsk" nil {:type :auto})]
    (def chsk chsk)
    (def ch-chsk ch-recv)))

(def sente-router-ref (atom (fn [] nil)))

(defn db-from-serializable
  "Init db from a sequence of datoms.

   It is used when reading an uploaded file,
   or reading a file from a URL."
  [serializable]
  (d/reset-conn! db/conn (d/from-serializable serializable)))


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
           (js/console.log "Processing..." f)
           
           (swap! state-ref merge {:status :processing})
           
           (set! (.-onload reader) 
             (fn [e]
               (db-from-serializable (t/read transit-json-reader (-> e .-target .-result)))
               
               (swap! state-ref merge {:status :ready})))
           
           (set! (.-onerror reader)
             (fn [e]
               (js/console.log (-> e .-target .-error))
               
               (swap! state-ref merge {:status :error})))
           
           (.readAsText reader f))))}]])

(defn WidgetContainer [e]
  (r/with-let [mouse-over-ref? (r/atom false)]
    [:div.flex.border.border-transparent.hover:border-gray-100
     {:on-mouse-over #(reset! mouse-over-ref? true)
      :on-mouse-leave #(reset! mouse-over-ref? false)}

     ;; -- Widget

     [widget/Widget e]

     
     ;; -- Options

     [:div.flex.flex-col.p-1
      {:class (when @mouse-over-ref? "bg-gray-50")}
      [:button
       {:class ["h-6 w-6"
                "inline-flex items-center justify-center"
                "bg-gray-200 hover:bg-gray-300"
                "text-gray-600"
                "focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-teal-500"
                "transition duration-200 ease-in-out"
                "rounded"
                "cursor-default"
                (when-not @mouse-over-ref? "invisible")]
        :on-click #(db/retract-entity (:db/id e))}
       [IconClose {:class "w-5 h-5"}]]]]))

(defn App [widgets]
  [:div.h-screen.flex.flex-col
   
   ;; -- Nav
   
   [:nav.h-14.flex.justify-end.items-center.px-10.py-2

    
    [:div.flex.space-x-2
     
     ;; -- Database
     [:> tippy/Tooltip
      {:title "View database"
       :size "small"}
      [:button.cursor-default
       {:key "database"
        :class "inline-flex items-center px-3 py-2 border text-gray-600 hover:text-gray-800 rounded-md hover:shadow-md hover:bg-gray-100 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition duration-200 ease-in-out"
        :on-click #(d/transact! db/conn [{:widget/uuid (str (random-uuid))
                                          :widget/type :widget.type/codemirror
                                          :widget.codemirror/mode "clojure"
                                          :widget.codemirror/lineNumbers true
                                          :widget.codemirror/value (with-out-str (pprint/pprint (db/all-widgets)))}])}
       
       [IconDatabase]]]
     
     
     ;; -- Export
     [:> tippy/Tooltip
      {:title "Download document"
       :size "small"}
      [:button.cursor-default
       {:key "download"
        :class [(if (empty? widgets)
                  "text-gray-300 pointer-events-none"
                  "text-gray-600")
                "inline-flex items-center px-3 py-2 border hover:text-gray-800 rounded-md hover:shadow-md hover:bg-gray-100 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-teal-500 transition duration-200 ease-in-out"]
        :on-click #(let [encoded (t/write transit-json-writer (d/serializable (d/db db/conn)))
                         
                         blob (js/Blob. #js [encoded] #js {"type" "application/transit+json"})]
                     
                     (FileSaver/saveAs blob "widgets.json"))}
       
       [IconDocumentDownload]]]]]
   
   
   ;; -- Widgets
   
   (let [{:keys [status]} @state-ref]
     (case status
       :loading
       [:div.flex.flex-col.flex-1.items-center.justify-center
        [:span "Loading..."]]
       
       :processing
       [:div.flex.flex-col.flex-1.items-center.justify-center
        [:span "Processing..."]]
       
       :error
       [:div.flex.flex-col.flex-1.items-center.justify-center
        [:span "Error"]]
       
       :ready
       (if (seq widgets)
         [:div.flex.flex-col.items-start.container.mx-auto.py-2.space-y-3
          (for [e widgets]
            ^{:key (:db/id e)}
            [WidgetContainer e])]
         
         ;; Ready - but empty.
         [:div.flex.flex-col.flex-1.items-center.justify-center
          [Import]])))])

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

(def router
  (rf/router ["/"]))

(defn ^:export init []
  (js/console.log "Welcome to Literate"
    (if goog.DEBUG
      "(dev build)"
      "(release build)"))
  
  (let [on-navigate 
        (fn [match _]
          (if-let [url (or (get-in match [:query-params :import])
                         (get-in match [:query-params :transact]))]
            (do
              (js/console.log "Loading..." url)
              
              (swap! state-ref merge {:status :loading})
              
              (.then (js/fetch url)
                (fn [response]                
                  (if (.-ok response)
                    (do
                      (js/console.log "Processing...")
                      
                      (swap! state-ref merge {:status :processing})
                      
                      (.then (.text response)
                        (fn [text]
                          (try
                            (let [data (t/read transit-json-reader text)]
                              (cond
                                (get-in match [:query-params :import])
                                (do
                                  (js/console.log "Importing...")
                                  (db-from-serializable data))

                                (get-in match [:query-params :transact])
                                (do
                                  (js/console.log "Transacting...")
                                  (d/transact! db/conn data))))
                            
                            (swap! state-ref merge {:status :ready})

                            (js/console.log "Ready")

                            (catch js/Error error
                              (js/console.error error)

                              (swap! state-ref merge {:status :error}))))))
                    
                    ;; HTTP error - response is not ok.
                    (swap! state-ref merge {:status :error})))))
            
            (swap! state-ref merge {:status :ready})))]
    
    (rfe/start! router on-navigate {:use-fragment false}))
  
  (when WS
    (reset! sente-router-ref (sente/start-client-chsk-router! ch-chsk handler)))
  
  ;; Rerender UI whenever the database changes.
  (d/listen! db/conn #(mount))
  
  (mount))

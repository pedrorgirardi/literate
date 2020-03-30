(ns literate.core
  (:require [rum.core :as rum :refer [defc]]))

(defmulti render :literate/type)

(defc Code [code]
  [:code.p-3.bg-yellow-100.rounded code])

(defmethod render :literate.type/code
  [{:literate/keys [code]}]
  (Code code))

(def state-ref (atom [#:literate {:type :literate.type/code
                                  :code "(f x)"}

                      #:literate {:type :literate.type/code
                                  :code "(map inc [1 2 3])"}]))

(defc App < rum/reactive []
  (into [:div.flex.flex-col.p-10.rounded]
        (for [cell (rum/react state-ref)]
          [:div.relative.flex.bg-gray-100.p-4.mb-4
           ;; Cell type
           [:span.absolute.right-0.mr-3.text-xs.tracking-wider.uppercase.text-gray-500
            (name (:literate/type cell))]

           (render cell)])))

(defn ^:export init []
  (rum/mount (App) (.getElementById js/document "app")))
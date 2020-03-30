(ns literate.core
  (:require [rum.core :as rum :refer [defc]]))

(defc App []
  [:span.text-3xl "Literate"])

(defn ^:export init []
  (rum/mount (App) (.getElementById js/document "app")))
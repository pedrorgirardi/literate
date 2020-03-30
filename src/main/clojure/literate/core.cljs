(ns literate.core
  (:require [rum.core :as rum :refer [defc]]))

(defc App []
  [:span "Literate"])

(defn ^:export init []
  (rum/mount (App) js/document.body))
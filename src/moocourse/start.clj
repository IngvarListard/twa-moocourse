(ns moocourse.start
  (:require [moocourse.ui :as ui]))

(defn hello [ctx]
  (println ctx)
  (ui/page ctx
           [:div
            "asdfqwerqwer"]))


(def plugin
  {:routes [["/hello" {:get hello}]]})

(ns moocourse.start
  (:require [moocourse.ui :as ui]))

(defn hello [ctx]
  (println ctx)
  (ui/page ctx
           [:div.max-w-sm.p-6.bg-white.border.border-gray-200.rounded-lg.shadow.dark:bg-gray-800.dark:border-gray-700
            "asdfqwerqwer"]))


(def plugin
  {:routes [["/hello" {:get hello}]]})

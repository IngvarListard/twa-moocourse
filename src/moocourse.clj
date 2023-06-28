(ns moocourse
  (:require [com.biffweb :as biff]
            [moocourse.start :as start]
            [clojure.test :as test]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn-repl]
            [malli.core :as malc]
            [malli.registry :as malr]
            [nrepl.cmdline :as nrepl-cmd]
            [ring.adapter.jetty9 :as jetty]))

(defn use-jetty [{:biff/keys [host port handler]
                  :or {host "localhost"
                       port 8080}
                  :as ctx}]
  (let [server (jetty/run-jetty (fn [req]
                                  (handler (merge ctx req)))
                                {:host host
                                 :port port
                                 :join? false
                                 :allow-null-path-info true})]
    (log/info "Jetty running on" (str "http://" host ":" port))
    (update ctx :biff/stop conj #(jetty/stop-server server))))

(defn use-jetty-
  "A Biff component that starts a Jetty web server."
  [{:biff/keys [host port handler]
    :or {host "localhost"
         port 8080}
    :as ctx}]
  (use-jetty ctx))

(def plugins
  [start/plugin])

(def routes [["" {:middleware [biff/wrap-site-defaults]}
              (keep :routes plugins)]
             ["" {:middleware [biff/wrap-api-defaults]}
              (keep :api-routes plugins)]])

(def handler (-> (biff/reitit-handler {:routes routes})
                 biff/wrap-base-defaults))

(def static-pages (apply biff/safe-merge (map :static plugins)))

(defn generate-assets! [ctx]
  (biff/export-rum static-pages "target/resources/public")
  (biff/delete-old-files {:dir "target/resources/public"
                          :exts [".html"]}))

(defn on-save [ctx]
  (biff/add-libs)
  (biff/eval-files! ctx)
  (generate-assets! ctx)
  (test/run-all-tests #"moocourse.test.*"))

(def malli-opts
  {:registry (malr/composite-registry
              malc/default-registry
              (apply biff/safe-merge
                     (keep :schema plugins)))})

(def initial-system
  {:biff/plugins #'plugins
   :biff/handler #'handler
   :biff/malli-opts #'malli-opts
   :biff.beholder/on-save #'on-save})

(defonce system (atom {}))

(def components
  [biff/use-config
   biff/use-secrets
   use-jetty-
   biff/use-beholder])

(defn start []
  (let [new-system (reduce (fn [system component]
                             (log/info "starting:" (str component))
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    (generate-assets! new-system)
    (log/info "Go to" (:biff/base-url new-system))))

(defn -main [& args]
  (start)
  (apply nrepl-cmd/-main args))

(defn refresh []
  (doseq [f (:biff/stop @system)]
    (log/info "stopping:" (str f))
    (f))
  (tn-repl/refresh :after `start))

(comment
  ;; Evaluate this if you make a change to initial-system, components, :tasks,
  ;; :queues, or config.edn. If you update secrets.env, you'll need to restart
  ;; the app.
  (refresh)

  ;; If that messes up your editor's REPL integration, you may need to use this
  ;; instead:
  (biff/fix-print (refresh)))

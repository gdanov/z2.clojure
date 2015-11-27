(import '(java.util.logging Logger))

(def -log (Logger/getLogger "bootstrap-repl"))

(.. -log (info "loading nrepl & cider, will take some time..."))

(ns bootstrap-repl
		(:require
			[clojure.tools.nrepl.server :as nrepl]
			[cider.nrepl :as cider]
			[clojure.core.async.impl.timers]
			[refactor-nrepl.middleware]))

(def cl (.. Thread currentThread getContextClassLoader))

(def port (atom 7888))

(def the-server (atom nil))

(defn set-port [pp]
			(swap! port (fn [_] pp)))

(def handler
	(apply nrepl/default-handler
				 (conj (map resolve cider/cider-middleware) (resolve 'refactor-nrepl.middleware/wrap-refactor))))

(defn ^{:z2-lifecycle :start} start []
			(.. -log (info (str "ich bin das REPL boot script and I listen on port " @port)))
			(swap! the-server
						 (fn [_]
								 (nrepl/start-server
									 :port @port
									 :handler handler))))

(defn ^{:z2-lifecycle :stop} shutdown []
			(.. -log (info "shutting down the REPL..."))
			(when @the-server (clojure.tools.nrepl.server/stop-server @the-server)

						;; TODO that's a hack. handle it better (git ticket, leak detection)
						(if (resolve 'clojure.core.async.impl.timers/timeout-daemon)
							(do
								(.. -log (info "shutting down clojure.core.async.impl.timers/timeout-daemon -> WARN you will see inturrupted exception, that's expected"))
								(doto clojure.core.async.impl.timers/timeout-daemon (.interrupt))))))

 (ns z2-tools
     (:import [com.zfabrik.components IComponentsLookup IComponentsManager]
            [com.zfabrik.components.java IJavaComponent]
            [com.zfabrik.resources IResourceManager IResourceLookup IResourceHandle]
            [comgd IClojureComponent]))

(defn handle [name]
  (.lookup IResourceManager/INSTANCE (str IComponentsLookup/COMPONENTS "/" name) IResourceHandle))

(defn invalidate [comp-name]
  (-> (handle comp-name)
    (.invalidate true)))

(defn comp-lup [name claz]
  (. IComponentsLookup/INSTANCE (lookup name claz)))

(defn injectRepl []
  (let [comps  ["org.clojure:tools.nrepl/java" 
                "org.clojure:core.async/java" 
                "cider:cider-nrepl/java"
                "refactor-nrepl:refactor-nrepl/java"]
        urls (reduce
               (fn [o comp]
                 (concat o (.. (comp-lup comp IJavaComponent) getPublicLoader getURLs)))
               []
               comps)
        target-comp (. IComponentsLookup/INSTANCE (lookup "client/clj" IClojureComponent))
        my-name (.. bootstrap-repl/cl getJavaComponentHandle getResourceInfo getName)
        my-src (java.net.URL. (str (.. IComponentsManager/INSTANCE (retrieve my-name) toURI) "/src.clj/"))]
    
    (. target-comp (injectUrls (into-array java.net.URL
                                 (concat
                                   urls
                                   [my-src]
                                   ))))
    (println "invoking load...")
    (. target-comp (invoke "clojure.core" "load" (into-array String ["/bootstrap_repl"])))
    (. target-comp (invoke "bootstrap-repl" "set-port" (into-array [(Integer. 7889)])))
    (. target-comp (invoke "bootstrap-repl" "start" (into-array [])))))
 


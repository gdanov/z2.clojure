package com.gd;

import clojure.lang.DynamicClassLoader;
import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.impl.components.java.ComponentClassLoader;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.expression.X;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Creates Clojure runtime component. It can have it's own sources like Clojure lib as well as private refs and includes like java comp.<br/>
 */
public class ClojureRTImpl extends Resource implements IClojureComponent {

	static final Logger logger = Logger.getLogger(ClojureRTImpl.class.getName());
	public static final String CLOJURE_LIBS = "clojure.libs";
	private static String rootCompName;
	private String name;
	private IComponentDescriptor compDesc;

	private ComponentClassLoader classLoader;
	public Class<?> rt;

	/**
	 * to be used for injecting resources ad-hoc
	 */
	private DynamicClassLoader injectCl;

	public ClojureRTImpl(String name) throws IOException {
		this.name = name;
		this.compDesc = IComponentsManager.INSTANCE.getComponent(this.name);
		if (rootCompName == null)
			rootCompName = IComponentsManager.INSTANCE.findComponents(
					X.var("comgd.clojurerootcomp").eq(X.val("true"))
			).iterator().next();

		//TODO when developing I always get same instance of the parent java comp????
		IResourceHandle parent = IComponentsLookup.INSTANCE.lookup(rootCompName, IResourceHandle.class);

		logger.info("I am new " + name + " " + this + " " + compDesc.getType() + " using parent factory " + parent.getResourceInfo().getName() + "@" + parent.hashCode());
	}

	@Override
	public void init() {
		try {
			logger.info("starting RT for " + name);
			startRT();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public <T> T as(Class<T> clz) {
		if (IClojureComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		return super.as(clz);
	}

	Object getVar(String ns, String name) {
		try {
			Object var = rt.getMethod("var", String.class, String.class).invoke(null, ns, name);
			return var;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void injectSourceUrls(URL[] urls) {
		for (URL u : Util.filterSourceJars(urls))
			injectCl.addURL(u);
	}

	/**
	 * creates new classloader from the <b>jars</b> with zero clojure classes up
	 * the dependency chain so that we own the classloader in which the clojure
	 * runtime is bootstrapped. If the classloader of the maven component is
	 * used, then we will have shared clojure runtime with potentially
	 * unpredictable consequences
	 * <p/>
	 * // sources // - my bootstrap-comp // - the sources of the component //
	 * parents // - some deps from the factory??? like nrepl // - all private
	 * refs in z.props of the target component // - one classloader with the
	 * clojure deps (transitives! via .getURLs). AND EXCLUDE SOURCES! // TODO
	 * register resource deps, also for clojure deps
	 */
	private void startRT() throws Exception {
		// TODO detect if there is RT loaded already and warn

		fixAwtLeak();

		// TODO add ref to the default java comp automatically
		Collection<String> refs = JavaComponentUtil.parseDependencies(compDesc.getProperty("java.privateReferences"));
		Collection<String> includes = JavaComponentUtil.parseDependencies(compDesc.getProperty(CLOJURE_LIBS));

		Util.registerHandleRefs(handle(), refs);
		Util.registerHandleRefs(handle(), includes);

		//useful during development. maybe z2 adds this anyway
		handle().addDependency(IComponentsLookup.INSTANCE.lookup(rootCompName, IResourceHandle.class));

		Collection<URL> sources = Util.merge(Util.collectSources(includes),
				Arrays.asList(
						new URL(Util.getSharedScriptsPath(rootCompName)) // inject the bootstrap clojure utilities
						, Util.getComponentSources(name)));

		Collection<ClassLoader> refsCl = Util.getJavaCompRefsClassLoaders(refs);
		// hm, it can't see the java refs...
		injectCl = new DynamicClassLoader(Util.createIncludedesCL(includes));

		ClassLoader[] parents = Util.conj(refsCl, injectCl).toArray(new ClassLoader[0]);

		this.classLoader = new ComponentClassLoader(this.handle(), name + " clojure classloader", sources.toArray(new URL[sources.size()]), parents);

		Util.withClassloader(classLoader, new Callable() {

			@Override
			public Object call() throws Exception {

				ClojureRTImpl.this.rt = classLoader.loadClass("clojure.lang.RT");
				rt.newInstance();// run the stat initializer. now clojure.core is up & running
				rt.getMethod("loadResourceScript", String.class).invoke(null, "bootstrap_comp.clj");

				logger.info("handing off to the clojure bootstrap scripts");
				invoke("bootstrap-comp", "initialize", compDesc.getProperty("clojure.main.ns"));

				return null;
			}
		});
	}

	private static void fixAwtLeak() {
		/*
		 * as crazy as it sounds, this was causing leak by holding on to the
		 * classloader created bellow. seems to be not specific to clojure
		 * https://cdivilly.wordpress.com/2012/04/23/permgen-memory-leak/
		 * and might be specific to jdk 1.7
		 */
		Util.withClassloader(IResourceHandle.class.getClassLoader(), new Callable() {
			@Override
			public Object call() throws Exception {
				sun.awt.AppContext.getAppContext();
				return null;
			}
		});
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		synchronized (this) {
			super.invalidate();
			logger.info("invalidating..." + this.name);

			try {
				String mainNs = compDesc.getProperty("clojure.main.ns");
				if (mainNs != null && !mainNs.isEmpty())
					invoke("bootstrap-comp", "shutdown");

				if (this.classLoader != null) {
					classLoader.invalidate();
					this.classLoader = null;
					this.compDesc = null;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Object invoke(final String ns, final String name, final Object... p) {
		// TODO: to switch contextCL or not to switch...?
		// must switch or injecting does not work
		return Util.withClassloader(classLoader, new Callable() {
			@Override
			public Object call() throws Exception {
				Object res;
				try {
					Object v = getVar(ns, name);
					Method invokeMethod = null;
					switch (p.length) {
						case 0:
							invokeMethod = v.getClass().getMethod("invoke");
							break;
						case 1:
							invokeMethod = v.getClass().getMethod("invoke", Object.class);
							break;
						case 2:
							invokeMethod = v.getClass().getMethod("invoke", Object.class, Object.class);
							break;
					}
					if (p.length > 0) res = invokeMethod.invoke(v, (Object[]) p);
					else res = invokeMethod.invoke(v);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				return res;
			}
		});
	}
}

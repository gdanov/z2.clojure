package com.gd;

import clojure.lang.DynamicClassLoader;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentClassLoader;
import com.zfabrik.resources.IResourceHandle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Callable;

import static com.zfabrik.components.java.JavaComponentUtil.fixJavaComponentName;

public class Util {

	public static final String SRC_CLJ = "src.clj";

	public static DynamicClassLoader newLoader(final ClassLoader parent) {

		DynamicClassLoader newCLL = AccessController
				.doPrivileged(new PrivilegedAction<DynamicClassLoader>() {

					@Override
					public DynamicClassLoader run() {
						return new DynamicClassLoader(
								(parent == null ? Thread.currentThread().getContextClassLoader() : parent));
					}
				});
		return newCLL;
	}

	public static void withClassloader(ClassLoader cl, final Runnable r) {
		withClassloader(cl, new Callable() {
			@Override
			public Object call() throws Exception {
				r.run();
				return null;
			}
		});
	}

	public static Object withClassloader(ClassLoader cl, Callable c) {

		ClassLoader origCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(cl);

		try {
			return c.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(origCl);
		}
	}

	static String getSharedScriptsPath(String rootCompName) throws IOException {
		return IComponentsManager.INSTANCE.retrieve(rootCompName).toURI().toURL() + "/src.clj";
	}

	public static URL getComponentSources(String compName) throws IOException {
		return new File(IComponentsManager.INSTANCE.retrieve(fixJavaComponentName(compName)), SRC_CLJ)
				.getCanonicalFile().toURI().toURL();
	}

	public static Collection<URL> collectSources(Collection<String> deps) throws IOException {
		Collection<URL> result = new ArrayList<>();
		for (String s : deps) {
			ClojureLibImpl cmp = IComponentsLookup.INSTANCE.lookup(s, ClojureLibImpl.class);
			if (cmp != null) {
				result.addAll(cmp.getSources());
			}
		}
		return result;
	}

	public static JavaComponentClassLoader getPublicClassLoader(String compName) {
		IJavaComponent comp = IComponentsLookup.INSTANCE.lookup(fixJavaComponentName(compName), IJavaComponent.class);
		if (comp == null) throw new RuntimeException(compName + " cannot be resolved to java component");
		return comp.getPublicLoader();
	}

	public static Collection<ClassLoader> getJavaCompRefsClassLoaders(Collection<String> depsNames) {
		Collection<ClassLoader> depsCLs = new HashSet<>(depsNames.size());
		for (String s : depsNames) {
			depsCLs.add(getPublicClassLoader(s));
		}
		return depsCLs;
	}

	public static Collection<URL> getComponentPublicResUrls(String s) {
		//WARN this method is shared between lib & rt. If you add new comp types make sure it makes sense to both consumers
		String compName = fixJavaComponentName(s);
		if (IComponentsLookup.INSTANCE.lookup(compName, IJavaComponent.class) != null)
			return Arrays.asList(filterSourceJars(getPublicClassLoader(s).getURLs()));
		else if (IComponentsLookup.INSTANCE.lookup(compName, ClojureLibImpl.class) != null)
			return IComponentsLookup.INSTANCE.lookup(compName, ClojureLibImpl.class)
					.getIncludedLibraries();
		else throw new RuntimeException(String.format("cannot resolve %s as java comp or clojure lib", compName));
	}

	public static Collection<URL> collectIncludeRefs(Collection<String> depsNames) {
		Set<URL> urls = new HashSet<>();
		for (String s : depsNames) {
			urls.addAll(getComponentPublicResUrls(s));
		}

		return urls;
	}

	/**
	 * effectively <b>includes</b> all components referenced here.
	 * See java.privateIncludes in the z2 java comp spec.
	 * this is crude re-implementation of the same mechanism for java components.
	 *
	 * @param depsNames
	 * @return new ClassLoader with the referred (directly and transitively) jars. All -source.jar URLs are removed as they trip clojure.lang.RT and cider-nrepl..util/java.clj
	 */
	public static ClassLoader createIncludedesCL(Collection<String> depsNames) {
		Collection<URL> urls = collectIncludeRefs(depsNames);

		return new URLClassLoader(urls.toArray(new URL[0]), IResourceHandle.class.getClassLoader());
	}

	public static <T> Collection<T> merge(Collection<T> col, Collection<T>... e) {
		try {
			Collection<T> result = col.getClass().newInstance();
			result.addAll(col);
			if (e != null)
				for (Collection<T> el : e)
					result.addAll(el);
			return result;
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}

	}

	/**
	 * adds {@code el} to the collection. {@code el} must be single object!
	 *
	 * @param col
	 * @param el
	 * @return
	 */
	public static <T> Collection<T> conj(Collection<T> col, T... el) {
		try {
			//TODO pensistent structs is better
			Collection<T> res = col.getClass().newInstance();
			res.addAll(col);
			if (el != null && el.length > 0)
				res.addAll(Arrays.asList(el));
			return res;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> Collection<T> cons(Collection<T> c1, Collection<T> c2) {
		try {
			Collection<T> res = c1.getClass().newInstance();
			res.addAll(c1);
			res.addAll(c2);
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static URL[] filterSourceJars(URL[] urLs) {
		LinkedList<URL> list = new LinkedList<>();
		for (URL u : urLs)
			if (!u.toString().endsWith("-sources.jar"))
				list.add(u);
		return list.toArray(new URL[0]);
	}

	public static void registerHandleRefs(IResourceHandle handle, Collection<String> refs) {
		for (String s : refs) {
			IResourceHandle rh = IComponentsLookup.INSTANCE.lookup(s, IResourceHandle.class);
			if (rh == null) throw new RuntimeException(String.format("could not resolve %s", s));
			// I am not as picky as JavaComponentImpl...
			handle.addDependency(rh);
		}
	}
}

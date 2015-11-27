package com.gd;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ClojureLibImpl extends Resource {

	Logger log = Logger.getLogger(ClojureLibImpl.class.getName());

	public static final String TYPE = "comgd.clojure.lib";

	private final String name;
	private final IComponentDescriptor compDesc;

	private Collection<URL> deps;

	public ClojureLibImpl(String name) {
		this.name = name;
		this.compDesc = IComponentsManager.INSTANCE.getComponent(this.name);
	}

	public Collection<URL> getSources() throws IOException {
		Set<URL> result = new HashSet<>();

		Path p = Paths.get(IComponentsManager.INSTANCE.retrieve(name).getCanonicalPath()).resolve(Util.SRC_CLJ);
		if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)) result.add(p.toUri().toURL());

		result.addAll(Util.collectSources(JavaComponentUtil.parseDependencies(compDesc.getProperty(ClojureRTImpl.CLOJURE_LIBS))));

		return result;
	}

	public Collection<URL> getIncludedLibraries() {

		if (deps == null) {
			Collection<String> depsNames = JavaComponentUtil.parseDependencies(
					compDesc.getProperty(ClojureRTImpl.CLOJURE_LIBS));

			deps = Collections.unmodifiableCollection(
					Util.collectIncludeRefs(depsNames));

			Util.registerHandleRefs(handle(), depsNames);
		}
		return deps;
	}

	@Override
	public <T> T as(Class<T> clz) {
		log.info(String.format("I am %s as %s %d", name, clz.getName(), this.hashCode()));
		if (clz.equals(this.getClass())) {
			return clz.cast(this);
		} else return super.as(clz);
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		super.invalidate();
	}
}

package com.gd;

import com.zfabrik.impl.components.java.ComponentClassLoader;

import java.net.URL;

public interface IClojureComponent {

    Object invoke(String ns, String name, Object... p);

	/**
	 * to be used only for clojure sources
	 * @param urls
	 */
	void injectSourceUrls(URL[] urls);
}

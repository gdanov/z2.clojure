package com.gd;

import com.zfabrik.resources.provider.Resource;

/**
 * see https://groups.google.com/d/topic/clojure/zl2cmsjs8Do/discussion and
 * https://groups.google.com/forum/#!topic/clojure/0AgUIiY8BQ8
 * <p/>
 * goal — be able to boot each clojure module in a repl. The only reuse possible
 * is clojure -> .jar libs. Use either the java.privateReferences (reused as in
 * java comp) or the clojure.references (reused via inclusion)
 * <p/>
 * this class is factory for several component types:
 * <ul>
 * <li>library (comgd.clojure.lib)</li>
 * <li>runtime (comgd.clojure.rt)</li>
 * <li>repl (comgd.clojure.repl)</li>
 * </ul>
 * <p/>
 * possible dependencies:
 * <ul>
 * <li>lib -> lib</li>
 * <li>rt -> lib</li>
 * <li>repl -> rt</li>
 * </ul>
 *
 * @author gdanov
 */

// TODO: solve the problem of loading clojure.pprint/pprint without explicit
// require. is that actually expected to work for any other namespace except for
// clojure's own?
public class CP extends Resource {


	@Override
	public void init() {
		super.init();
	}

	@SuppressWarnings({"unchecked", "unused"})
	@Override
	public <T> T as(Class<T> clz) {
		synchronized (this) {
/*
			logger.info(name + " as " + clz + " " + this);

			if (classLoader == null)
				try {
					startRT();
				} catch (Exception e) {
					logger.log(Level.WARNING, "could not start the runtime", e);
				}

			if (clz.equals(IClojureComponent.class)) {
				logger.info("gettin component");

				return clz.cast(this);
			} else if (IJavaComponent.class.equals(clz)) {
				return clz.cast(this);
			} else if (IDependencyComponent.class.equals(clz)) {
				return (T) new IDependencyComponent() {

					@Override
					public void prepare() {

					}
				};
			}

*/
			return (T) "hehllo";
		}
	}


}

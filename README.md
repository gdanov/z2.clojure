# z2.clojure
##clojure integration for Z2 

Enables clojure to run on top of [Z2](http://www.z2-environment.eu)

The repo contains the core components ([maven repo](com.gd.z2.clojure.mvn/) and [the component factory](com.gd.z2.clojure/)) as well as two sample components.

The quality is beta-level. Expect memory leaks that typically manifist themselves as permGen OOM. Increasing the heap and permGen spaces of your webWorker will improve the [MTBF](https://en.wikipedia.org/wiki/Mean_time_between_failures).

To install just

1. [clone Z2](https://redmine.z2-environment.net/projects/z2-environment/wiki/Step_2_-_Install_and_run_in_5_minutes)
1. clone this repository next to Z2 so that your folder looks like that:
```
+ project folder
  + z2.clojure
  |-+ com.gd.z2.clojure
  |-+ com.gd.z2.mvn
  |-+ com.gd.z2.sample...
  + z2-base.core
```

At the first run Z2 little longer while it downloads the dependencies. Eventually you will see lines that look like this:
```
11/27 09:49:48 [1]com.gd.ClojureRTImpl [800]: starting RT for com.gd.z2.clojure/systemRepl
11/27 09:49:50 [35]      bootstrap-repl [800]: loading nrepl & cider, will take some time...
11/27 09:50:10 [35]      bootstrap-repl [800]: ich bin das REPL boot script and I listen on port 7888
11/27 09:50:10 [35]      bootstrap-comp [800]: init done for [bootstrap-repl]
```
as well as good deal "hello world" messages from the sample components. Once you see the "init done" message everything is ready.

## What's in the box

Two [component factories](http://www.z2-environment.eu/v24doc#__RefHeading__3627_2054128055) that allow the creation of *runtime* and *library* clojure components. The major difference between the two is that *runtimes* have their own copy of the clojure runtime (clojure.lang.RT & co.) classes, while the *library* is passive contributor of clojure source files (namespaces) and references. 

At the moment this is the supported dependency matrix (include/reference):

==>             | Java | Clojure Runtime | Clojure Library
--- | --- | --- | ---
Java            | i/r  | -- | --
Clojure Runtime | i/r  | -- | i
Clojure Library | i  | -- | i

The property name `clojure.libs` for clojure components is used to include and the standard `java.privateReferences` is used to reference.

## More on the *runtime* component

Even though Clojure compiles to java classes (on-the-fly or offline), the result is classes that require the presence of the core Clojure library in order to bootstrap them. Even the simplest `(println "Hello world")` sample requires the [clojure.lang](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/) package. The reason is that even the compiled .class files use Clojure's [Var](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Var.java), [Symbol](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Symbol.java) and the rest of the facilities that make Clojure dynamic.

Key feature of Clojure is it's heavy reliance on Symbols and Vars to late bind (and resolve) practically everything, including functions and namespaces. [clojure.lang.RT](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java) is central Symbol/Var registry implemented as static structures. Think of it as global uber Spring context implemented as a static map field on a class. As a result, within one classloader hierarchy there can be only one runtime context (Symbol/Var set) active. This is major dealbraker if we want to have basic modularization as it requires some visibility isolation between the modules. Secondary challenge would be the component reload (major feature of Z2) — [it's not perfect](http://dev.clojure.org/display/design/Never+Close+a+REPL) in Clojure itself, so I did not want to rely on something like [this](https://github.com/clojure/tools.namespace) for managing component lifecycle. It's fine library when programming in the REPL but it can't provide robustness comparable to the one of Java component reload.

The solution I went with is to allow for many clojure runtimes to exists within one Z2 worker by making the *runtime* z2 component have it's own copy of the core clojure classes loaded but not shared with other runtimes. On one side it creates the illusion that each *runtime* component is an isolated island in the z2 ocean, on the other side it requires lots of attention when sharing classes or instances due to the problems arising from having classes loaded more than once in different loaders. 

Sharing harmless .class files (anything non-clojure) between runtimes and java components is trivial (like the javax or z2 core interfaces) as long as they are [*referenced* and not *included*](http://www.z2-environment.eu/v24doc#__RefHeading__3643_2054128055). Everything else is to be avoided.

Exchanging "harmless" **instances** of Clojure thingies is different thing. It's a challenge to sanitize these shared classes and instances, so for now the responsibility is 100% on the user's side. It's too complicated for me to be able to explain what's safe, so for now it's danger zone.

Further reading on that problem:

* [Clojure Compilation: Parenthetical Prose to Bewildering Bytecode](http://blog.ndk.io/2014/01/26/clojure-compilation.html)
* [The static initializer of clojure.lang.RT](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java#L301)
* [Thread from the clojure group](https://groups.google.com/d/topic/clojure/0AgUIiY8BQ8/discussion)
* [Alternative design](https://groups.google.com/d/topic/clojure/0AgUIiY8BQ8/discussion)


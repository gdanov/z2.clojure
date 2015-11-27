package com.gd.test;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.resources.IResourceManager;
import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

import com.gd.IClojureComponent;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(Z2UnitTestRunner.class)
@Z2UnitTest(componentName = "spy/java")
public class Spy {
    @Test
    public void startREPL() throws IOException {
        System.out.println("Hello z2Unit");

        List<String> comps = IComponentsLookup.INSTANCE.list();
        for (String n : comps) {
            // System.out.println(n);
        }

        // System.out.println(IComponentsManager.INSTANCE.getComponent("spy/client").getType());

        // IComponentsLookup.INSTANCE.lookup("spy/client2", comgd.Happy.class);
        // IComponentsLookup.INSTANCE.lookup("spy/client2", comgd.Happy.class);

        IClojureComponent happy = IComponentsLookup.INSTANCE.lookup("client/clj", IClojureComponent.class);
        System.out.println("=== " + happy);

//        happy.startRepl();

        // System.out.println(IResourceManager.INSTANCE.list());
        // IResourceManager.INSTANCE.invalidate(Arrays.asList(new String[]
        // {"com.zfabrik.components/spy/java"}));
    }

    @Test
    public void invalidate() {
        IResourceManager.INSTANCE.invalidate(Arrays.asList(new String[]{"com.zfabrik.components/client/clj"}));
    }

    @Test
    public void callHello() {
        Runnable happy = IComponentsLookup.INSTANCE.lookup("spy/client2", Runnable.class);
        happy.run();
    }

    @Test
    public void experiment() {

        IJavaComponent cider = IComponentsLookup.INSTANCE.lookup("cider:cider-nrepl/java", IJavaComponent.class);
        System.out.println(cider.getPublicLoader());
    }

    @Test
    public void checkForLeaks() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            IClojureComponent cljComp = IComponentsLookup.INSTANCE.lookup("client/clj", IClojureComponent.class);
//            cljComp.startRepl();
            Thread.sleep(1000);
            IResourceManager.INSTANCE.invalidate(Arrays.asList(new String[]{"com.zfabrik.components/client/clj"}));
        }
    }
}

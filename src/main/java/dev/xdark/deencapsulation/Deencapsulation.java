package dev.xdark.deencapsulation;

import sun.reflect.ReflectionFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public final class Deencapsulation {

	private static MethodHandles.Lookup lookup(Class<?> lookupClass) {
		try {
			Constructor<?> constructor = ReflectionFactory.getReflectionFactory()
					.newConstructorForSerialization(
                            MethodHandles.Lookup.class,
                            MethodHandles.Lookup.class.getDeclaredConstructor(Class.class)
                    );

			return (MethodHandles.Lookup) constructor.newInstance(lookupClass);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("That should have not happened", e);
		}
	}

    private static Set<Module> getModules(Class<?> caller) {
        Set<Module> modules = new HashSet<>();
        Module base = caller.getModule();
        ModuleLayer baseLayer = base.getLayer();

        if (baseLayer != null)
            modules.addAll(baseLayer.modules());

        modules.addAll(ModuleLayer.boot().modules());

        for (ClassLoader cl = caller.getClassLoader(); cl != null; cl = cl.getParent()) {
            modules.add(cl.getUnnamedModule());
        }

        return modules;
    }

	public static void deencapsulate(Class<?> caller) {
        Set<Module> modules = getModules(caller);
        try {
			MethodHandle export = lookup(Module.class)
                    .findVirtual(
                            Module.class,
                            "implAddOpens",
                            MethodType.methodType(void.class, String.class)
                    );

            for (Module module : modules) {
				for (String name : module.getPackages()) {
					export.invokeExact(module, name);
				}
			}
		} catch (Throwable t) {
			throw new IllegalStateException("Could not export packages", t);
		}
	}

    public static void allowNative(Class<?> caller) {
        Set<Module> modules = getModules(caller);
        try {
            MethodHandle export = lookup(Module.class)
                    .findVirtual(
                            Module.class,
                            "implAddEnableNativeAccess",
                            MethodType.methodType(Module.class)
                    );

            for (Module module : modules) {
                export.invokeExact(module);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not export packages", t);
        }
    }
}

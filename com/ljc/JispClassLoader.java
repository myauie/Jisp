package com.ljc;

class JispClassLoader extends ClassLoader {

    JispClassLoader(ClassLoader parent) {
        super(parent);
    }

    Class loadJispClass(String name, byte... data) {
        Class c = defineClass(name, data, 0, data.length);
        resolveClass(c);
        return c;
    }
}

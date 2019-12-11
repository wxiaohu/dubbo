package com.alibaba.dubbo.demo.provider;

// com.alibaba.dubbo.common.bytecode.Wrapper0
// com.alibaba.dubbo.common.bytecode.Wrapper
public class WrapperClass {

    // property name array.
    public static String[] pns = new String[]{};

    // property type map.
    public static java.util.Map pts = new java.util.HashMap();

    // all method name array.
    public static String[] mns = new String[]{"sayHello"};

    // declared method name array.
    public static String[] dmns = new String[]{"sayHello"};

    // 方法参数类型参数类型
    public static Class[] mts0 = new Class[]{String.class};

    public String[] getPropertyNames() {
        return pns;
    }

    public boolean hasProperty(String n) {
        return pts.containsKey(n);
    }

    public Class getPropertyType(String n) {
        return (Class) pts.get(n);
    }

    public String[] getMethodNames() {
        return mns;
    }

    public String[] getDeclaredMethodNames() {
        return dmns;
    }

    public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws java.lang.reflect.InvocationTargetException {
        com.alibaba.dubbo.demo.DemoService w;
        try {
            w = ((com.alibaba.dubbo.demo.DemoService) o);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        try {
            if ("sayHello".equals(n) && p.length == 1) {
                //return ($w) w.sayHello((java.lang.String) v[0]);
                return w.sayHello((java.lang.String) v[0]);
            }
        } catch (Throwable e) {
            throw new java.lang.reflect.InvocationTargetException(e);
        }
        throw new com.alibaba.dubbo.common.bytecode.NoSuchMethodException("Not found method \"" + n + "\" in class com.alibaba.dubbo.demo.DemoService.");
    }

    public void setPropertyValue(Object o, String n, Object v) {
        com.alibaba.dubbo.demo.DemoService w;
        try {
            w = ((com.alibaba.dubbo.demo.DemoService) o);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        throw new com.alibaba.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + n + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

    public Object getPropertyValue(Object o, String n) {
        com.alibaba.dubbo.demo.DemoService w;
        try {
            w = ((com.alibaba.dubbo.demo.DemoService) o);
//            if ($2.equals("A")) {
//                return ($w) w.A;
//            }
//            if ($2.equals("B")) {
//                return ($w) w.B;
//            }
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        throw new com.alibaba.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + n + "\" filed or setter method in class com.alibaba.dubbo.demo.DemoService.");
    }

}

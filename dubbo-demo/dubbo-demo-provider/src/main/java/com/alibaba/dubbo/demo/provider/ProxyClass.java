package com.alibaba.dubbo.demo.provider;

// className com.alibaba.dubbo.common.bytecode.proxy0
// interface com.alibaba.dubbo.rpc.service.EchoService,com.alibaba.dubbo.demo.DemoService
public class ProxyClass {

    public static java.lang.reflect.Method[] methods = {
    public abstract java.lang.String com.alibaba.dubbo.demo.DemoService.sayHello(java.lang.String),
    public abstract java.lang.Object com.alibaba.dubbo.rpc.service.EchoService.$echo(java.lang.Object)}

    private java.lang.reflect.InvocationHandler handler;

    public ProxyClass(java.lang.reflect.InvocationHandler arg0) {
        handler = arg0;
    }

    public java.lang.String sayHello(java.lang.String arg0) {
        Object[] args = new Object[1];
        args[0] = ($w) $1;
        Object ret = handler.invoke(this, methods[0], args);
        return (java.lang.String) ret;
    }

    public java.lang.Object $echo(java.lang.Object arg0) {
        Object[] args = new Object[1];
        args[0] = ($w) $1;
        Object ret = handler.invoke(this, methods[1], args);
        return (java.lang.Object) ret;
    }
}

// className com.alibaba.dubbo.common.bytecode.Proxy0
// superClass com.alibaba.dubbo.common.bytecode.Proxy
class ProxyClass2 {
    public Object newInstance(java.lang.reflect.InvocationHandler h) {
        return new com.alibaba.dubbo.common.bytecode.proxy0($1);
    }

}
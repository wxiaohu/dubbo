### dubbo-config模块

#### 核心作用
1. 在应用启动阶段初始化Dubbo配置，以完成服务的暴露和引用过程。包括XML配置、属性配置、API配置、注解配置、动态配置中心。
2. 所有配置项分为三大类
* 服务发现：表示该配置项用于服务的注册与发现，目的是让消费方找到提供方。
* 服务治理：表示该配置项用于治理服务间的关系，或为开发测试提供便利条件。
* 性能调优：表示该配置项用于调优性能，不同的选项对性能会产生影响。
3. 所有配置最终都将转换为URL表示，并由服务提供方生成，经注册中心传递给消费方，各属性对应URL的参数。
4. 配置覆盖关系：SystemProperty->外部化配置->SpringXML/API->dubbo.properties。
5. 不同粒度配置的覆盖关系：方法级->接口级->全局配置，如果级别一样，则消费方优先，提供方次之。

#### XML解析流程
1. 在`DubboNamespaceHandler`类中按标签分别注册`DubboBeanDefinitionParser`类进行解析。例如`registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true))`;
2. 在`DubboBeanDefinitionParser`类中，循环beanClass的方法，找出属性名称（public修饰只有一个参数的方法，如果没有get方法则不处理）。获取配置文件中的值并设置进去。
3. 返回`BeanDefinition`，将所有的配置Bean交给Spring容器管理，由Spring容器初始化。

#### ServiceBean
1. `service`标签对应的`Class`，由`Spring`工厂通过`BeanDefinition`完成实例初始化。实现了`InitializingBean`,`DisposableBean`,`ApplicationContextAware`,`ApplicationListener<ContextRefreshedEvent>`, `BeanNameAware`,`ApplicationEventPublisherAware`接口。
2. 重写`setApplicationContext`方法获取Spring上下文并设置到`SpringExtensionFactory`，支持`SpringBean`依赖注入。
2. 重写`afterPropertiesSet`方法检查`Provider`,`Application`,`Model`,`Registry`,`Monitor`参数是否为空，为空从Spring容器中获取并设置进去。
3. 所有参数初始化完成之后，调用`ServiceConfig.export()`方法发布服务。如果是延迟暴露，则在ContextRefreshedEvent事件中，调用export方法暴露服务。

#### ReferenceBean
1. `reference`标签对应的`Class`，由`Spring`工厂通过`BeanDefinition`完成实例初始化。实现了`FactoryBean`,`ApplicationContextAware`,`InitializingBean`,`DisposableBean`接口。
2. 重写`setApplicationContext`方法获取Spring上下文并设置到`SpringExtensionFactory`，支持`SpringBean`依赖注入。
3. 重写`afterPropertiesSet`方法检查`Consumer`,`Application`,`Model`,`Registry`,`Monitor`参数是否为空，为空从Spring容器中获取并设置进去。
4. 所有参数初始化完成之后，调用`getObject()`方法返回服务引用，实际调用的`ReferenceConfig.get()`方法。


#### Spring XML 扩展
1. 定义一套`XML Schema`来描述XML文档结构，各种属性等。例如`META-INF/dubbo.xsd`。
2. 创建自定义的`NamespaceHandler`来负责解析用户在XML中的配置。例如`DubboNamespaceHandler`，继承`自NamespaceHandlerSupport`。
3. 创建自定义`BeanDefinitionParser`负责解析xml中的节点信息。例如`DubboBeanDefinitionParser`，继承自`BeanDefinitionParser`。
4. 向Spring容器中注册`Spring handler`和`Spring schema`。例如`META-INF/spring.handlers`，`META-INF/spring.schemas`。

#### Spring Bean 生命周期事件
1. `InitializingBean.afterPropertiesSet()`：Bean初始化完成之后调用该方法，默认构造函数执行之后调用。可用于自定义初始化或者检查实例化是否完整。`init-method`配置在其后执行。对应注解`@PostConstruct`。
2. `DisposableBean.destroy()`：Bean销毁之前执行。`destroy-method`配置之前执行。对应注解`@PreDestroy`。
3. `ApplicationContextAware.setApplicationContext(ApplicationContext applicationContext)`：实现该接口的Bean可以从`applicationContext`获取所有实例化的Bean，必须是同一个上下文环境。
4. `ApplicationListener.onApplicationEvent(ApplicationEvent event)`：事件机制(观察者设计模式的实现)，当完成某种操作时会发出某些事件动作。`ContextRefreshedEvent`事件是`ApplicationContext`容器已就绪可用执行上下文刷新事件。
5. `BeanNameAware.setBeanName(String name)`：初始化回调（`afterPropertiesSet`,`init-method`）之前被调用，当前Bean需要访问配置文件中本身的ID属性。
7. `ApplicationEventPublisherAware.setApplicationEventPublisher(ApplicationEventPublisher var1)`：获取一个`applicationEventPublisher`，发布事件，好像是用于注解驱动。


#### Spring 注解驱动
DOTO


### dubbo-registry模块

#### 抽象接口
1. `RegistryFactory`：注册中心工厂SPI接口，提供getRegistry方法返回Registry实例，默认实现为dubbo。
2. `AbstractRegistryFactory`：注册中心工厂抽象类，加入注册中心集合缓存，重写getRegistry方法。抽象createRegistry方法，由具体的注册中心工厂类去实现。
2. `RegistryService`：注册中心服务接口，规定注册中心功能契约。抽象register，unregister，subscribe，unsubscribe，lookup方法。由具体的注册中心服务去实现。
3. `Registry`：注册中心接口，继承Node和RegistryService。
5. `AbstractRegistry`：注册中心抽象类，实现了把服务URL信息缓存到Properties对象和本地缓存文件中的逻辑。缓存文件结构为serviceKey=通知URL，多个空格分隔。
6. `FailbackRegistry`：注册中心抽象类，支持失败重试。失败后加入失败集合，开线程周期性去扫描失败集合。订阅失败后先从Properties缓存中获取URL，获取失败再进失败列表。
4. `NotifyListener`：通知监听接口，收到服务更改通知时触发。

#### 主要作用
1. 动态加入：服务提供者可动态把服务暴露给消费者，消费者无需更新配置文件。
2. 动态发现：服务消费者可动态感知新的服务提供者，路由规则和新的配置。
3. 动态调整：新增参数自动更新到所有服务节点。
4. 统一配置：统一配置中心，避免本地配置导致每个服务配置不一致。

#### 工作流程
1. 服务提供者启动时，会向注册中心providers节点写入自己元数据，同时订阅configurators节点配置元数据。
2. 服务消费者启动时，会向注册中心consumers节点写入自己元数据，同时订阅服务提供者，路由和配置元数据(providers,routers,configurators)。
3. 服务治理中心(dubbo-admin)启动时，会同时订阅所有服务消费者，服务提供者，路由，配置元数据。
4. 服务提供者离开或者加入时，变化信息会动态通知给消费者和服务治理中心。

#### 订阅发布
1. 订阅发布是整个注册中心的核心功能之一。
2. 服务提供者注册自己是为了让消费者感知服务的存在，服务消费者注册自己是为了让服务治理中心发现自己。
3. 通常有pull和push模式，一种是客户端定时轮询注册中心拉取配置，一种是注册中心主动推送数据给客户端。Dubbo采用第一次启动全量拉取，后续接收事件通知重新拉取数据。这种方式是有局限性的，当服务节点较多时会对网络造成很大压力。
4. 用户可以通过服务治理平台下发路由配置和动态配置，服务内部会通过订阅机制收到变更，更新已经暴露的服务。

#### Zookeeper注册中心
1. Zookeeper是树形结构注册中心，分为持久节点，持久顺序节点，临时节点，临时顺序节点。Dubbo用Zookeeper作为注册中心只会创建持久节点和临时节点，不关心节点顺序。
* 持久节点：保证节点数据不会丢失，即使注册中心重启。持久顺序节点：在持久节点上增加节点顺序。临时节点：连接丢失或者session超时，注册的节点会被自动移除。临时顺序节点：在临时节点上增加节点顺序。
2. Zookeeper每个节点都对应版本号，当版本号发生变化，会触发watcher事件并推送数据给订阅方，版本号强调的是变更次数，即使该节点的值没有变化，只要有更新操作依然会使版本号变化。
3. 数据结构
```
+ /dubbo                        注册中心分组，持久节点。来自用户配置<dubbo:registry>的group属性，默认/dubbo。
    + /c.a.d.d.DemoService      服务接口名，持久节点
        + /providers            持久节点
            + /URL              临时节点，服务提供者URL元数据。dubbo://127.0.0.1:20880/c.a.d.d.DemoService?key=value&.....
        + /consumers            持久节点
            + /URL              临时节点，服务消费者URL元数据。dubbo://127.0.0.1:20/c.a.d.d.DemoService?key=value&.......
        + /routers              路由策略元数据。
        + /configurators        动态配置元数据。
```


#### Redis注册中心
1. 通过Redis的Map结构，使用过期机制和publish/subscribe实现。
2. 数据结构：
* Root,Service,Category组合成Redis的Key，例如`/dubbo/c.a.d.d.DemoService/providers`。
* value是一个Map结构，URL作为Map的Key。超时时间作为Map的value，用于判断脏数据，由服务治理中心删除。
* 通过事件的值区分事件类型：register，unregister，subscribe，unsubscribe。
3. 调用过程：
* 服务提供者启动时，向Redis Key`/root/service/providers`中写入当前服务提供者URL和过期时间，并向Channel`/root/service/providers`中发布register事件消息。
* 服务消费者启动时，从Channel `/root/service/providers`中订阅register和unregister消息，并向Redis Key`/root/service/consumers`中写入当前服务消费者URL和过期时间。
* 服务提供者需要周期性刷新URL的过期时间，通过启动定时调度线程不断延续Key的过期时间，如果服务提供者宕机没有续期，则Key会因为超时而被删除，服务也会被认定为下线。
* 服务消费方首次连接注册中心，会获取全量数据并缓存本地，后续服务列表变化通过publish/subscribe通道广播。

#### 缓存机制
1. 如果每次远程调用都要从注册中心获取一次可调用服务列表，会给注册中心带来很大压力。消费者或者服务治理中心获取注册信息后会做本地缓存，内存中有一份保存在Properties对象中，磁盘上也有一份文件，通过File对象引用。

#### RegistryDirectory
1. 基于注册中心的动态服务目录，可感知注册中心配置的变化，它所持有的Invoker列表会随着注册中心内容的变化而变化。每次变化后RegistryDirectory会动态增删Invoker。
2. 实现了NotifyListener接口，当服务消费方订阅数据时传入当前监听器。

#### RegistryProtocol
1. 基于注册中心注册发现服务的实现协议，不是真正的协议，整合了注册中心和具体的实现协议逻辑。


### dubbo-rpc模块

#### 抽象接口
1. `Invoker`：调用者接口，一次RPC调用过程的抽象，可能是本地实现，远程实现，集群实现。
2. `InvokerListener`：调用者监听，Invoker被引用，被销毁时触发。
3. `Exporter`：暴露者接口，暴露服务接口的抽象，包括获取Invoker对象unexport取消暴露方法。
4. `ExporterListener`：暴露服务的监听接口，监听exported和unexported事件。
5. `Filter`：过滤器SPI接口，对调用过程进行过滤。
6. `Invocation`：一次RPC调用过程中的参数抽象，如方法名，方法参数等。
7. `ProxyFactory`：代理工厂SPI接口，获取Invoker对象，生成接口代理类。
8. `Result`：RPC调用结果接口。
9. `Protocol`：协议SPI接口，提供暴露服务和引用服务方法。

#### 服务暴露
1. 基于Dubbo协议的服务暴露：实例化DubboExporter对象并缓存。调用remoting层绑定端口。
2. 基于Dubbo协议的服务引用：实例化DubboInvoker对象，调用remoting层打开连接。
3. injvm本地暴露：实例化InjvmExporter对象并缓存。
4. injvm本地引用：实例化InjvmInvoker对象，持有InjvmExporter缓存引用。

#### 服务调用
1. 基于Dubbo的服务调用：获取连接发送消息。
2. injvm本地调用：从缓存中通过ServiceKey获取Invoker执行。

#### 过滤器
1. ProtocolFilterWrapper：Protocol包装类，SPI中的Wrapper类。用于在服务调用中构建过滤器链。

#### 监听器
1. ProtocolListenerWrapper：Protocol包装类，SPI中的Wrapper类。用于在服务暴露和引用时加入监听器。

#### 代理工厂
1. 生成服务提供方和消费方代理。JDK和Javassist两种实现，默认Javassist。
2. 服务提供方getInvoker()：先生成Wrapper包装类，再实例化Invoker对象，重写doInvoke()方法调用包装类的invokeMethod()方法。
```
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
```
3. 服务消费方代理getProxy()：
```
// className com.alibaba.dubbo.common.bytecode.proxy0
// interface com.alibaba.dubbo.rpc.service.EchoService,com.alibaba.dubbo.demo.DemoService
public class ProxyClass implements com.alibaba.dubbo.rpc.service.EchoService, com.alibaba.dubbo.demo.DemoService {
    // public abstract java.lang.String com.alibaba.dubbo.demo.DemoService.sayHello(java.lang.String),
    // public abstract java.lang.Object com.alibaba.dubbo.rpc.service.EchoService.$echo(java.lang.Object)
    public static java.lang.reflect.Method[] methods = new Method[]{};

    private java.lang.reflect.InvocationHandler handler;

    public ProxyClass(java.lang.reflect.InvocationHandler arg0) {
        handler = arg0;
    }

    public java.lang.String sayHello(java.lang.String arg0) {
        Object[] args = new Object[1];
        args[0] = arg0;
        Object ret = null;
        try {
            ret = handler.invoke(this, methods[0], args);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (java.lang.String) ret;
    }

    public java.lang.Object $echo(java.lang.Object arg0) {
        Object[] args = new Object[1];
        args[0] = arg0;
        Object ret = null;
        try {
            ret = handler.invoke(this, methods[1], args);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return (java.lang.Object) ret;
    }
}

// className com.alibaba.dubbo.common.bytecode.Proxy0
// superClass com.alibaba.dubbo.common.bytecode.Proxy
class ProxyClass2 extends com.alibaba.dubbo.common.bytecode.Proxy {
    public Object newInstance(java.lang.reflect.InvocationHandler h) {
        //return new com.alibaba.dubbo.common.bytecode.proxy0($1);
        return new ProxyClass(h);
    }
}
```


### dubbo-cluster

#### 抽象接口
1. `Cluster`：集群容错SPI接口，提供抽象方法join，合并Directory中的Invoker List为一个虚拟Invoker。默认扩展为FailoverCluster。
2. `Configurator`：配置规则接口，用于服务治理中心下发配置更新规则。
3. `ConfiguratorFactory`：配置规则工厂SPI接口，提供抽象方法getConfigurator()获取配置规则实例。
4. `Directory`：目录接口，Directory代表了多个 Invoker，并且它的值会随着注册中心的服务变更推送而变化 。一个服务类型对应一个Directory。服务类型和Invoker的映射关系。在软件工程中，一个目录是指一组名字和值的映射。它允许根据一个给出的名字来查找对应的值，与词典相似。
5. `LoadBalance`：负载均衡SPI接口，提供抽象方法select()，根据规则选择一个合适的Invoker。默认扩展为RandomLoadBalance。
6. `Merger`：分组聚合SPI接口，将某对象数组合并为一个对象。
7. `Router`：路由接口。
8. `RouterFactory`：路由规则工厂SPI接口，提供抽象方法getRouter()返回Router实例。

#### 配置规则
1. 根据配置URL协议头确定配置规则。absent://... or override://....。
1. AbsentConfigurator：如果原先存在该属性的配置，则以原先配置的属性值优先，如果原先没有配置该属性，则添加新的配置属性。
2. OverrideConfigurator：直接覆盖。

#### 目录
1. AbstractDirectory：抽象类，核心是list方法，获取服务提供方Invoker集合后，再进过路由规则过滤一遍，返回过滤后的Invoker集合。
2. StaticDirectory：静态目录，用于多注册中心，每个注册中心通过集群负载路由选一个Invoker，放入StaticDirectory。

#### 负载均衡
1. AbstractLoadBalance：负载均衡抽象类，实现select()方法，提供doSelect()方法由具体负载均衡实现。提供权重计算逻辑。
2. 权重计算：
* 获取服务提供者启动时间戳，计算服务提供者运行时长，获取服务预热时间，默认为10分钟，如果服务运行时间小于预热时间，则重新计算服务权重，即降权。
* 该过程主要用于保证当服务运行时长小于服务预热时间时，对服务进行降权，避免让服务在启动之初就处于高负载状态。服务预热是一个优化手段，与此类似的还有JVM预热。主要目的是让服务启动后“低功率”运行一段时间，使其效率慢慢提升至最佳状态。


#### 路由


#### 集群容错

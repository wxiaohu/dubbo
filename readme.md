### dubbo-config模块

#### 核心作用
1. 在应用启动阶段初始化Dubbo配置，以完成服务的暴露和引用过程。所有配置最终都将转换为URL表示，并由服务提供方生成，经注册中心传递给消费方，各属性对应URL的参数。配置方式包括XML配置、属性配置、API配置、注解配置、动态配置中心。
2. 配置覆盖关系：SystemProperty->外部化配置->SpringXML/API->dubbo.properties。不同粒度配置覆盖关系：方法级->接口级->全局配置。如果级别一样，则消费方优先，提供方次之。

#### ServiceBean
1. `<dubbo:service/>`标签对应的`Class`，由`Spring`工厂通过`BeanDefinition`完成实例初始化。实现了`InitializingBean`,`DisposableBean`,`ApplicationContextAware`,`ApplicationListener<ContextRefreshedEvent>`, `BeanNameAware`,`ApplicationEventPublisherAware`接口。

#### ReferenceBean
1. `<dubbo:reference/>`标签对应的`Class`，由`Spring`工厂通过`BeanDefinition`完成实例初始化。实现了`FactoryBean`,`ApplicationContextAware`,`InitializingBean`,`DisposableBean`接口。

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

#### 主要作用
1. 动态加入：服务提供者可动态把服务暴露给消费者，消费者无需更新配置文件。
2. 动态发现：服务消费者可动态感知新的服务提供者，路由规则和新的配置。
3. 动态调整：新增参数自动更新到所有服务节点。
4. 统一配置：统一配置中心，避免本地配置导致每个服务配置不一致。

#### 工作流程
1. 服务提供者启动时，会向注册中心providers节点写入自己元数据，同时订阅configurators节点配置元数据。
2. 服务消费者启动时，会向注册中心consumers节点写入自己元数据，同时订阅服务提供者，路由和配置元数据(providers,routers,configurators)。
3. 服务治理中心(dubbo-admin)启动时，会同时订阅所有服务消费者，服务提供者，路由，配置元数据。用户可以通过服务治理平台下发路由配置和动态配置，服务内部会通过订阅机制收到变更，更新已经暴露的服务。
4. 服务提供者离开或者加入时，变化信息会动态通知给服务消费者和服务治理中心。服务提供者注册自己是为了让消费者感知服务的存在，服务消费者注册自己是为了让服务治理中心发现自己。
5. 获取配置信息通常有pull和push模式，一种是客户端定时轮询注册中心拉取配置，一种是注册中心主动推送数据给客户端。Dubbo采用第一次启动全量拉取，后续接收事件通知重新拉取数据。这种方式是有局限性的，当服务节点较多时会对网络造成很大压力。
6. 服务消费者和服务提供者在收到订阅节点变更通知后会缓存通知URL，内存中有一份保存在Properties对象中，磁盘上也有一份文件，通过File对象引用。当注册中心不可用不影响服务启动和调用逻辑。

#### 抽象接口
1. `RegistryFactory`：注册中心工厂SPI接口，提供getRegistry方法返回Registry实例，默认实现为dubbo。
2. `AbstractRegistryFactory`：注册中心工厂抽象类，加入注册中心集合缓存，重写getRegistry方法。抽象createRegistry方法，由具体的注册中心工厂类去实现。
2. `RegistryService`：注册中心服务接口，规定注册中心功能契约。抽象register，unregister，subscribe，unsubscribe，lookup方法。由具体的注册中心服务去实现。
3. `Registry`：注册中心接口，继承Node和RegistryService。
5. `AbstractRegistry`：注册中心抽象类，实现了把通知URL信息缓存到Properties对象和本地缓存文件中的逻辑。缓存文件结构为serviceKey=通知URL，多个空格分隔。
6. `FailbackRegistry`：注册中心抽象类，支持失败重试。失败后加入失败集合，开线程周期性去扫描失败集合。订阅失败后先从Properties缓存中获取URL。
4. `NotifyListener`：通知监听接口，收到服务更改通知时触发。

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

#### RegistryDirectory
1. 基于注册中心的动态服务目录，可感知注册中心配置的变化，它所持有的Invoker列表会随着注册中心内容的变化而变化。每次变化后RegistryDirectory会动态增删Invoker。
2. 实现了NotifyListener接口，当服务消费方订阅数据时传入当前监听器。

#### RegistryProtocol
1. 基于注册中心注册发现服务的实现协议，不是真正的协议，整合了注册中心和具体的实现协议逻辑。


### dubbo-rpc模块

#### 主要作用
1. 封装 RPC 调用，以 Invocation, Result 为中心，扩展接口为 Protocol, Invoker, Exporter。抽象各种协议，以及动态代理，只包含一对一的调用，不关心集群的管理。

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
1. 基于Dubbo协议的服务暴露：实例化DubboExporter(invoker, key, exporterMap)对象并缓存到`Map<serviceKey,Exporter>`集合。调用remoting层绑定并监听端口。
2. 基于Dubbo协议的服务引用：实例化DubboInvoker(serviceType, url, getClients(url), invokers)对象并缓存到Set集合。调用remoting层打开连接。
3. injvm本地暴露：实例化InjvmExporter对象并缓存。
4. injvm本地引用：实例化InjvmInvoker对象并缓存，持有InjvmExporter缓存引用。

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


### dubbo-remoting

#### 主要功能
1. dubbo-remoting远程通讯模块：相当于Dubbo框架中各种协议的实现，如果RPC用RMI协议则不需要使用此包。
2. Remoting内部再划为Transport传输层和Exchange信息交换层，Transport层只负责单向消息传输，是对Mina，Netty，Grizzly的抽象，它也可以扩展UDP传输，而Exchange层是在Transport之上封装了Request-Response语义。
3. Exchange信息交换层：封装请求响应模式，同步转异步，以Request, Response为中心，扩展接口为Exchanger, ExchangeChannel, ExchangeClient, ExchangeServer。
4. Transport网络传输层：抽象mina和netty为统一接口，以Message为中心，扩展接口为Channel, Transporter, Client, Server, Codec。
5. Client与Channel是一对一关系，所以Client继承至Channel。Server与Channel是一对多关系。
#### 核心流程
##### 服务端初始化，以`DubboProtocol`，`NettyServer`为例
1. 在`DubboProtocol`类中，通过`export(Invoker<T> invoker)`方法发布服务生成`Exporter`对象成功之后，调用`openServer(URL url)`打开服务，如果host:port对应`ExchangeServer`已存在，调用`server.reset(url)`重置服务参数。如果不存在调用`createServer(URL url)`创建服务。
2. 创建Server服务端
- 通过`HeaderExchanger.bind(URL url,ExchangeHandler handler)`方法初始化Exchange层`HeaderExchangeServer`。
- 通过`NettyTransporter.bind(URL url, ChannelHandler listener)`方法初始化Transporter层`NettyServer`。
- 初始化所有ChannelHandler：`MultiMessageHandler`,`HeartbeatHandler`,`AllChannelHandler`,`DecodeHandler`,`HeaderExchangeHandler`,`requestHandler`。采用装饰器模式，组合关系扩展`ChannelHandler`功能，最外层`ChannelHandler`为`MultiMessageHandler`。
- 通过构造方法使`HeaderExchangeServer`持有`NettyServer`引用，`NettyServer`持有`MultiMessageHandler`引用。
- `NettyServer`，`NettyClient`都继承自`AbstractPeer`，那么他们自身就是一个`ChannelHandler`。
- 通过实例化的NettyServer对象初始化`NettyServerHandler`，其继承至`Netty`的`ChannelDuplexHandler`，`NettyServerHandler`首先把与服务端建立连接后到得到`Netty`对应的`Channel`对象转换为`Dubbo`的`NettyChannel`并放入缓存Map中，再调用对应事件处理方法。
- `NettyServerHandler`与`ChannelHandler`事件处理方法对应关系：`channelActive->connected`，`channelInactive->disconnected`，`channelRead->received`，`write->sent`，`exceptionCaught->caught`。

##### 客户端初始化，以`DubboProtocol`，`NettyClient`为例
1. 在`DubboProtocol`类中，通过`refer(Class<T> serviceType, URL url)`方法引用服务生成Invoker对象时，需要获取`ExchangeClient`对象。
2. `ExchangeClient`分两种，共享连接和一个服务一个连接，可通过URL参数控制。共享连接通过map缓存，其他直接创建连接。
3. 创建Client
- 通过`HeaderExchanger.connect(URL url, ExchangeHandler handler)`方法初始化初始化Exchange层`HeaderExchangeClient`。
- 通过`NettyTransporter.connect(URL url, ChannelHandler listener)`方法初始化Transporter层`NettyClient`。
- 初始化所有`ChannelHandler`，和服务端一样。
- 初始化`NettyClientHandler`，事件处理方法和Server端类似。

##### 线程模型实现
1. ThreadPool：线程池SPI接口
- FixedThreadPool：默认线程池，固定数量线程池。

参数 | 值
---|---
corePoolSize | threads=200
maximumPoolSize | threads=200
keepAliveTime | 0
unit | TimeUnit.MILLISECONDS
workQueue | queues == 0 ? SynchronousQueue:(queues < 0 ? LinkedBlockingQueue: LinkedBlockingQueue(queues))
threadFactory | NamedInternalThreadFactory
Rejected |AbortPolicyWithReport

- CachedThreadPool：缓存线程池。

参数 | 值
---|---
corePoolSize | cores=0
maximumPoolSize | threads=Integer.MAX_VALUE
keepAliveTime | 60000
unit | TimeUnit.MILLISECONDS
workQueue | queues == 0 ? SynchronousQueue:(queues < 0 ? LinkedBlockingQueue: LinkedBlockingQueue(queues))
threadFactory | NamedInternalThreadFactory
Rejected |AbortPolicyWithReport

- LimitedThreadPool：

参数 | 值
---|---
corePoolSize | cores=0
maximumPoolSize | threads=200
keepAliveTime | Long.MAX_VALUE
unit | TimeUnit.MILLISECONDS
workQueue | queues == 0 ? SynchronousQueue:(queues < 0 ? LinkedBlockingQueue: LinkedBlockingQueue(queues))
threadFactory | NamedInternalThreadFactory
Rejected |AbortPolicyWithReport

- EagerThreadPool：线程数量达到corePoolSize之后，只有当workqueue满了之后，才会增加工作线程。这个线程池就是对这个特性做了优化。
    - 重写ThreadPoolExecutor，用原子变量记录提交到线程池任务数submittedTaskCount。
    - 重写BlockQueue，offer方法中判断当提交任务数量大于核心线程数小于最大线程数返回false让其创建工作中者线程不进队列。提交任务数达到最大线程数再入队列。

2. AbortPolicyWithReport：线程池拒绝策略：打印warn日志，开一个线程将线程堆栈信息写到文件中。默认user.home目录。用到了Semaphore，保证同一时刻只有一个线程打印日志
3. ChannelEventRunnable:分派到线程池中的工作者线程。根据不同的事件类型做不同的处理。
4. Dispatcher线程池调度器SPI接口，负责将事件和消息处理派发到线程池。
- AllDispatcher：默认调度策略，对应AllChannelHandler，连接、断开连接、捕获异常以及消息处理都派发到线程池。
- ConnectionOrderedDispatcher：对应ConnectionOrderedChannelHandler，连接、取消连接以及消息处理都派发到线程池。
    - 该类自己创建了一个跟连接相关的线程池connectionExecutor，把连接操作和断开连接操派发到该线程池，而消息处理则分发到WrappedChannelHandler的线程池中。
    - connectionExecutor最大线程数和最大核心线程数均为1，采用LinkedBlockQueue阻塞队列，AbortPolicyWithReport拒绝策略。队列长度默认Integer.MAX_VALUE，线程名默认Dubbo，均可通过URL参数修改。
- DirectDispatcher：所有事件和消息处理都不派发到线程池，直接通过I/O线程执行。
- ExecutionDispatcher：对应ExecutionChannelHandler，把接收到的请求消息分派到线程池，而除了请求消息以外，其他消息类型和事件都直接通过I/O线程执行。
- MessageOnlyDispatcher：对应MessageOnlyChannelHandler，所有接收到的消息分发到线程池，其他类型直接通过I/O线程执行。
5. WrappedChannelHandler：ChannelHandler装饰器类，所有Dispatcher对应ChannelHandler的父类。在其构造方法中初始化线程池，分为客户端线程池和服务端线程池。
6. DataStore数据存储SPI接口，只有一个实现SimpleDataStore，一种数据存储结构。线程池初始化完成之后会放进去`<componentKey,<port,executor>>`，用在服务关闭时能拿到这个线程池shutdown。


### dubbo-common

#### SPI扩展点加载机制
##### Java SPI(Service Provider Interface)
1. JDK内置的一种动态加载扩展点的实现。在META-INF/services目录下放置一个与接口同名的文本文件，文件的内容为接口的实现类全路径，多个用换行符分隔。使用java.util.ServiceLoader.load()来加载具体的实现。 
2. 缺点：一次性实例化所有实现类，初始化可能会很耗时。如果扩展加载失败，无法获取到真正的异常信息。扩展如果依赖其他扩展类，无法做到依赖注入和自动装配。不能与其他框架集成，比如依赖SpringBean无法实现。

##### 可扩展性

##### Dubbo SPI
1. 对Java原生SPI机制进行了一些扩展，增加缓存和延迟初始化。增加了对依赖注入和AOP的支持。
2. 在`MEAT-INF/dubbo/`目录下放置一个与接口同名的文本文件，文件内容为key=扩展点实现类全路径名，多个实现用换行符分隔。添加SPI注解`@SPI("key")`在接口上，通过参数key指定默认实现类。通过`ExtensionLoader.getExtensionLoader(Interface.class).getDefaultExtension()`获取默认扩展实现类实例。Dubbo会默认扫描META-INF/dubbo/，META-INF/dubbo/internal/，META-INF/services/这三个目录下的配置文件。
3. 缓存机制
* Class缓存：获取扩展类Class实例先从缓存中读取，缓存不存在则读取配置文件然后把Class实例缓存到内存中。
* 实例缓存：获取扩展实现类的实例先从缓存中读取，缓存不存在则实例化并缓存。按需实例化并缓存，性能更好。
4. 扩展类分类
* 普通扩展类：最基础的，使用`@SPI(defaultImpl)`定义的默认实现类。
* 包装扩展类：Wapper类，通用逻辑的抽象，需要在构造方法中传入一个具体的扩展接口的实现类。扩展点的AOP实现，在扩展类方法执行前后加入处理逻辑进行增强。
* 自适应扩展：一个扩展接口可以有多种实现，具体使用哪一个可不写死在配置或者代码中，通过获取URL中的参数动态确定。
5. 扩展类特性
* 自动包装：在加载扩展类时发现该类包含其他扩展点作为构造函数入参，这个扩展类会被认为是Wapper类，Dubbo会自动注入其他扩展类。
* 自动加载：如果一个扩展类是另一个扩展类的成员属性，并拥有`set`方法，那么Dubbo也会自动注入对应扩展类。
* 自适应：使用`@Adaptive(paramkey)`注解，可动态通过URL中的参数来确定使用哪个扩展类。通过参数匹配对应的扩展类，如果都匹配不成功则使用默认扩展类。
* 自动激活：使用`@Activate`注解，标记对应的扩展类默认被激活启用。可通过条件控制扩展类在不同条件下被激活。

5. 扩展点注解
* `@SPI("key")`：扩展点注解。一般用在接口上，表明该接口是Dubbo的扩展点。通过参数指定默认扩展类，也就是内置实现。
* `@Adaptive([paramkey...])`：扩展点自适应注解。可用在接口，类，方法，枚举类上。在方法级别可通过参数动态获取扩展类，第一次getExtension时，会自动生成和编译一个动态的Adaptive类，达到动态扩展类的效果，装饰器模式的实现。在类级别整个扩展类会直接作为默认实现，不再生成代码，多个扩展类中只能有一个`@Adaptive`注解在类上。
* `@Activate()`：扩展点自动激活注解。可用在类，接口，枚举类和方法上。

#### 核心流程
##### 加载并缓存扩展实现类类Class
1. 从扩展接口上的SPI注解中获取默认扩展实现类key并缓存，例如@SPI("dubbo")，那么该扩展接口默认扩展实现类key为dubbo。
2. 加载配置文件并实例化扩展实现类Class，根据扩展接口全限定名拼接文件路径，Dubbo会默认扫描META-INF/dubbo/，META-INF/dubbo/internal/，META-INF/services/这三个目录下的配置文件。
3. 校验Class：
* 判断扩展实现类clazz是否和扩展接口是同一个类或接口或者是其子类，如不是抛异常。
* 扩展实现类上是否有@Adaptive注解，默认自适应扩展实现类，不需要生成代码。如果有加入自适应类缓存。
* 是否是Wrapper类，clazz是否有一个包含type类型参数的构造器，如果是加入Wrapper类缓存。
* 剩下为普通扩展实现类，必须有无参构造器否则报错，加入缓存。同时判断是否有@Activate自动激活注解，如果有加入自动激活类缓存。

##### 实例化并缓存扩展实现类实例
1. 根据key获取扩展实现类Class对象，实例化扩展实现类并缓存。
2. 扩展类依赖注入，即IOC：
* 判断扩展工厂ExtensionFactory是否为空，通过ExtensionFactory实现依赖注入。支持Dubbo的SPI和Spring的Bean。
* 通过反射获取实例的所有方法，匹配方法名以set开头，参数长度为1，public修饰的并且没有DisableInject注解的方法。
* 匹配出属性值和参数类型，参数类型为扩展接口类型，属性值为扩展实现类key或名字。通过ExtensionFactory实例化，最后调用invoke执行该方法将值set进去。
3. Wrapper依赖注入，即AOP：循环Wrapper类缓存，通过参数为当前type的构造方法实例化Wrapper对象。依次执行依赖注入逻辑。最终返回Wrapper对象。

##### 实例化并缓存自适应扩展实现类实例
1. 如果自适应类缓存有值则直接返回默认自适应扩展实现类实例，否则生成自适应扩展实现类Class并实例化。最后执行依赖注入。
2. 生成自适应扩展实现类Class：
* 循环检查扩展接口所有方法是否有@Adaptive注解，所有方法上都没有该注解者抛出异常拒绝创建自适应扩展类。
* 如果方法上没有@Adaptive，方法体内抛出异常。
* 寻找类型为URL的方法参数，如果没有找到，循环方法的所有参数类型，匹配get开头，public修饰的，返回类型为URL的非静态无参方法。没匹配到，直接抛出异常。
* 获取URL参数，默认为@Adaptive(param)配置的值。如果没有配置，会自动生成一个。扩展点接口名按驼峰分开后用.连接。例如 HasAdaptiveExt == has.adaptive.ext
```
public class HasAdaptiveExt$Adaptive implements com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt {
    public java.lang.String echo(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("has.adaptive.ext");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt) name from url(" + url.toString() + ") use keys([has.adaptive.ext])");
        com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt extension = (com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt.class).getExtension(extName);
        return extension.echo(arg0, arg1);
    }
}
```
##### 实例化自动激活扩展类对象
1. 支持激活多个扩展类，通过URL参数和@Activate注解上配置的参数来控制顺序和分组，默认自动激活扩展类的加载。
2. - 开头的参数，代表不加载某个名称的扩展类。-default 代表不自动加载所有的默认扩展类。-name 代表不加载名称为name的激活扩展类。

#### URL


# Nacos 错误堆栈测试用例

## 测试用例 1：配置未找到异常
**错误堆栈**：
```
com.alibaba.nacos.api.exception.NacosException: null
	at com.alibaba.nacos.client.naming.NacosNamingService.subscribe(NacosNamingService.java:512)
	at com.alibaba.nacos.client.naming.NacosNamingService.subscribe(NacosNamingService.java:485)
	at com.alibaba.cloud.nacos.discovery.NacosWatch.start(NacosWatch.java:137)
	at org.springframework.context.support.DefaultLifecycleProcessor.doStart(DefaultLifecycleProcessor.java:178)
	at org.springframework.context.support.DefaultLifecycleProcessor.access$200(DefaultLifecycleProcessor.java:54)
	at org.springframework.context.support.DefaultLifecycleProcessor$LifecycleGroup.start(DefaultLifecycleProcessor.java:356)
	at java.base/java.lang.Iterable.forEach(Iterable.java:75)
Caused by: com.alibaba.nacos.api.exception.NacosException: Requested resource does not exist!
	at com.alibaba.nacos.client.naming.net.NamingProxy.requestApi(NamingProxy.java:558)
	at com.alibaba.nacos.client.naming.net.NamingProxy.subscribeService(NamingProxy.java:515)
	at com.alibaba.nacos.client.naming.NacosNamingService.subscribe(NacosNamingService.java:506)
	... 6 more
```

**可能原因**：
- 服务名配置错误
- 服务在 Nacos 中不存在
- 命名空间配置错误
- 租户信息（namespace）不正确

---

## 测试用例 2：连接超时异常
**错误堆栈**：
```
java.net.SocketTimeoutException: connect timed out
	at java.base/java.net.PlainSocketImpl.socketConnect(Native Method)
	at java.base/java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:412)
	at java.base/java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:255)
	at java.base/java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:237)
	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
	at java.base/java.net.Socket.connect(Socket.java:633)
	at okhttp3.internal.platform.Platform.connectSocket(Platform.java:131)
	at okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.java:269)
	at okhttp3.internal.connection.RealConnection.connect(RealConnection.java:191)
	at okhttp3.internal.connection.ExchangeFinder.findConnection(ExchangeFinder.java:229)
	at okhttp3.internal.connection.ExchangeFinder.findHealthyConnection(ExchangeFinder.java:108)
	at okhttp3.internal.connection.ExchangeFinder.find(ExchangeFinder.java:88)
	at okhttp3.internal.connection.Transmitter.newExchange(Transmitter.java:169)
	at okhttp3.internal.connection.ConnectInterceptor.intercept(ConnectInterceptor.java:42)
	at com.alibaba.nacos.client.naming.net.NamingProxy.callServer(NamingProxy.java:671)
	at com.alibaba.nacos.client.naming.net.NamingProxy.requestApi(NamingProxy.java:553)
```

**可能原因**：
- Nacos 服务器地址配置错误
- Nacos 服务未启动
- 网络防火墙阻止连接
- 端口配置不正确（默认 8848）
- 服务器宕机

---

## 测试用例 3：认证失败异常
**错误堆栈**：
```
com.alibaba.nacos.api.exception.NacosException: <html><body><h1>Whitelabel Error Page</h1><p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p><div id='created'>Wed Mar 04 15:00:00 CST 2026</div><div>There was an unexpected error (type=Forbidden, status=403).</div></body></html>
	at com.alibaba.nacos.client.naming.net.NamingProxy.requestApi(NamingProxy.java:558)
	at com.alibaba.nacos.client.naming.net.NamingProxy.queryList(NamingProxy.java:438)
	at com.alibaba.nacos.client.naming.NacosNamingService.getAllInstances(NacosNamingService.java:296)
	at com.alibaba.nacos.client.naming.NacosNamingService.getAllInstances(NacosNamingService.java:276)
	at com.alibaba.cloud.nacos.ribbon.NacosServerList.getServers(NacosServerList.java:63)
	at com.alibaba.cloud.nacos.ribbon.NacosServerList.getInitialListOfServers(NacosServerList.java:50)
	at com.netflix.loadbalancer.DynamicServerListLoadBalancer.updateListOfServers(DynamicServerListLoadBalancer.java:259)
	at com.netflix.loadbalancer.DynamicServerListLoadBalancer.restOfInit(DynamicServerListLoadBalancer.java:147)
```

**可能原因**：
- Nacos 开启了认证功能
- 用户名或密码配置错误
- Access Key/Secret Key 无效
- Token 过期
- 权限不足

---

## 测试用例 4：配置解析异常
**错误堆栈**：
```
com.alibaba.nacos.api.exception.NacosException: failed to parse config
	at com.alibaba.nacos.client.config.impl.ClientWorker$ClientConfigInfo.getServerConfig(ClientWorker.java:562)
	at com.alibaba.nacos.client.config.impl.ClientWorker$ClientConfigInfo.process(ClientWorker.java:513)
	at com.alibaba.nacos.client.config.impl.ClientWorker$LongPollingRunnable.run(ClientWorker.java:706)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	at java.base/java.lang.Thread.run(Thread.java:833)
Caused by: java.lang.IllegalArgumentException: While trying to resolve parameter placeholder '${app.name:unknown}' in content, encountered: app.name
	at com.alibaba.nacos.client.utils.PropertyPlaceholderHelper.parseStringValue(PropertyPlaceholderHelper.java:180)
	at com.alibaba.nacos.client.utils.PropertyPlaceholderHelper.replacePlaceholders(PropertyPlaceholderHelper.java:126)
	at com.alibaba.nacos.client.config.impl.ClientWorker$ClientConfigInfo.getServerConfig(ClientWorker.java:556)
	... 8 more
```

**可能原因**：
- 配置中包含未定义的占位符
- 环境变量缺失
- 配置文件格式错误
- 占位符语法不正确
- 依赖的配置项未加载

---

## 测试用例 5：并发修改冲突异常
**错误堆栈**：
```
com.alibaba.nacos.api.exception.NacosException: Conflict, dataId: 'application.yml', group: 'DEFAULT_GROUP'
	at com.alibaba.nacos.client.config.impl.ClientWorker.publishConfig(ClientWorker.java:325)
	at com.alibaba.nacos.client.config.NacosConfigService.publishConfigInner(NacosConfigService.java:237)
	at com.alibaba.nacos.client.config.NacosConfigService.publishConfig(NacosConfigService.java:189)
	at com.alibaba.cloud.nacos.refresh.NacosRefreshHistory.publish(NacosRefreshHistory.java:92)
	at com.alibaba.cloud.nacos.refresh.NacosContextRefresher.registerNacosListener(NacosContextRefresher.java:154)
	at com.alibaba.cloud.nacos.refresh.NacosContextRefresher.onApplicationEvent(NacosContextRefresher.java:107)
	at com.alibaba.cloud.nacos.refresh.NacosContextRefresher.onApplicationEvent(NacosContextRefresher.java:62)
	at org.springframework.context.event.SimpleApplicationEventMulticaster.doInvokeListener(SimpleApplicationEventMulticaster.java:176)
	at org.springframework.context.event.SimpleApplicationEventMulticaster.invokeListener(SimpleApplicationEventMulticaster.java:169)
	at org.springframework.context.event.SimpleApplicationEventMulticaster.multicastEvent(SimpleApplicationEventMulticaster.java:143)
	at org.springframework.context.support.AbstractApplicationContext.publishEvent(AbstractApplicationContext.java:437)
Caused by: com.alibaba.nacos.api.exception.NacosException: [409] publish fail, casMd5: 'd41d8cd98f00b204e9800998ecf8427e', modify-index: '10086'
	at com.alibaba.nacos.client.config.http.ServerHttpAgent.httpPost(ServerHttpAgent.java:228)
	at com.alibaba.nacos.client.config.impl.ClientWorker.publishConfig(ClientWorker.java:319)
	... 10 more
```

**可能原因**：
- 并发修改同一配置
- CAS（Compare-And-Swap）版本冲突
- 另一个实例已更新配置
- MD5 校验失败
- 修改 index 不匹配

---

## 使用说明

1. 启动项目：
   ```bash
   cd backend && mvn spring-boot:run
   cd frontend && npm run dev
   ```

2. 访问 http://localhost:3000

3. 选择 Nacos 仓库

4. 打开"错误堆栈分析"页面

5. 复制上面任意一个错误堆栈粘贴到输入框

6. 点击"分析"按钮

7. 查看系统是否能：
   - ✅ 正确解析堆栈
   - ✅ 定位相关源码文件
   - ✅ 提供合理的原因分析
   - ✅ 关联相关代码片段

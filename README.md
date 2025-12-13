# 坦克大战客户端

## 连接TCP服务器 TcpClientManager
1. 创建一个Socket对象，绑定IP和端口号，连接TcpServer
2. 用阻塞队列存储需要发送到TcpServer的消息
3. 连接服务器后，新建一个Writer线程，循环 将阻塞队列中的消息发送给TcpServer
4. 连接服务器后，新建一个Reader线程，循环 接收TcpServer发来的消息
5. 发送的消息格式为：自定义协议头+消息体

## 自定义协议
1. 协议头为10字节：操作类型(1B) + 版本号(1B) + 请求ID(4B) + 协议体长度(4B) + 校验码(4B)
2. 校验码：将 操作类型、版本号、协议体长度、消息体 进行CRC32加密，得到校验码
3. 消息体：不同的操作类型对应不同的消息体，序列化方式为protobuf

## protobuf
protobuf（Protocol Buffers）是 Google 开发的一种语言无关、平台无关的序列化数据结构的方法。它具有以下几个主要优势：
1. **高效性**：相比 XML 和 JSON，protobuf 序列化后的数据更小、解析速度更快，非常适合对性能要求高的场景。
2. **跨语言支持**：支持多种编程语言（如 Java、C++、Python 等），同一份 .proto 文件可以生成不同语言的代码，方便多语言项目之间的通信。
3. **强类型定义**：使用 .proto 文件定义数据结构，具备严格的类型约束，有助于减少因数据格式不一致导致的错误。
4. **向后兼容性**：可以安全地更新数据结构而不会破坏旧版本的数据读取，非常适用于长期运行的服务或需要升级迭代的系统。
5. **自动生成代码**：通过 protoc 编译器可以根据 .proto 文件自动生成序列化和反序列化的代码，减少了手动编写重复代码的工作量。
6. **适合网络传输与存储**：因为其紧凑性和效率，常被用于微服务之间、客户端与服务器之间的通信协议，以及日志压缩存储等场景。

## 视图View的生命周期 ViewManager
1. ViewManager 在客户端启动时创建，并打开 Login 页面
2. 所有Controller都必须继承ViewLifecycle 并实现 onShow 和 onHide 方法
3. 跳转页面时，使用ViewManager的 show 方法，这个方法里会调用旧页面的onHide 方法，并调用新页面的 onShow 方法

## 消息类型回调监听总线 MsgCallbackEventManager
为了消息发送和接收的解耦，消息监听总线用于管理消息监听器，当接收到对应的消息时，会调用对应的消息监听器进行处理。
1. 获取MsgCallbackEventManager实例
2. 在 controller 中的 实现 ViewLifecycle 的 onShow 方法，调用MsgCallbackEventManager.addMsgCallbackListener(msgType, listener)方法，注册当前页面要监听消息和处理消息的回调
3. 在 controller 中的 实现 ViewLifecycle 的 onHide 方法，在切换页面时，调用MsgCallbackEventManager.removeMsgCallbackListener(msgType)方法，注销当前页面的监听器，避免重复监听
4. 当接收到消息时，会使用MsgType中的Parser解析消息体，并回调所有该MsgType的消息监听器(Consumer)进行处理

## 请求ID回调事件总线 RequestIdCallbackEventManager
1. 获取RequestIdCallbackEventManager实例
2. 调用GameConnectionManager.sendAndListen 发送消息时指定callback逻辑，会将requestId与callback的映射存到map
3. 当返回消息时，会通知requestId对应的callback进行处理
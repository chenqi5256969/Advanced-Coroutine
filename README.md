# Advanced-Coroutine（周末更新）
协程三种针对网络请求的高级使用

### 协程在项目中的高级用法：

#### 场景：当用户点击按钮或者无感知加载，当处于弱网状态，用户发起多次请求，怎么利用协程优雅的处理多次请求

###### 处理一：发起多次请求，取消上次请求，只发起最新的一次请求

###### 处理二：让下一次请求进行排队任务等待

###### 处理三：复用前一次请求，比如前一次任务已经完成了一大半，这时候进来一个新的请求，这次新的请求复用刚刚的请求

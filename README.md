# VariousAsyncHttpClientPerformance

## Scenario:


 10*1000 total requests， 1000 concurrency， server block every request for 10 seconds.
 
 Compare AsyncRestTemplate combined with 4 different AsyncRequestFactory.

## Conclusion
If you just want send several requests, default and okhttp is both ok.

If you want to handle thousands of slow connections simultaneously, you should use CloseableHttpAsyncClient or Netty4.   

If you also want to use proxy, then only CloseableHttpAsyncClient is avaiable.


## Implementation

### SimpleClientHttpRequestFactory
AsyncRestTemplate's default request factory `SimpleClientHttpRequestFactory`， use a simple thread pool and blocking api, so it will use 1000 threads to handle 1000 concurrency.   

It take 101.702 seconds, close to 100 seconds.


Thread stack follows behind:
  
```
"SimpleAsyncTaskExecutor-1001" #1015 prio=5 os_prio=31 tid=0x00007fc9d8949800 nid=0x82d03 runnable [0x000070004e716000]
   java.lang.Thread.State: RUNNABLE
	at java.net.SocketInputStream.socketRead0(Native Method)
	at java.net.SocketInputStream.socketRead(SocketInputStream.java:116)
	at java.net.SocketInputStream.read(SocketInputStream.java:170)
	at java.net.SocketInputStream.read(SocketInputStream.java:141)
	at java.io.BufferedInputStream.fill(BufferedInputStream.java:246)
	at java.io.BufferedInputStream.read1(BufferedInputStream.java:286)
	at java.io.BufferedInputStream.read(BufferedInputStream.java:345)
	- locked <0x0000000773843bb8> (a java.io.BufferedInputStream)
	at sun.net.www.http.HttpClient.parseHTTPHeader(HttpClient.java:704)
	at sun.net.www.http.HttpClient.parseHTTP(HttpClient.java:647)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1535)
	- locked <0x00000006c2f84e38> (a sun.net.www.protocol.http.HttpURLConnection)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1440)
	- locked <0x00000006c2f84e38> (a sun.net.www.protocol.http.HttpURLConnection)
	at java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:480)
	at org.springframework.http.client.SimpleBufferingAsyncClientHttpRequest$1.call(SimpleBufferingAsyncClientHttpRequest.java:94)
	at org.springframework.http.client.SimpleBufferingAsyncClientHttpRequest$1.call(SimpleBufferingAsyncClientHttpRequest.java:77)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.lang.Thread.run(Thread.java:745)
```


### Okhttp3

OkHttp3ClientHttpRequestFactory also use blocking thread pool, 1000 threads to handle 1000 concurrency.   

It takes 101.697 seconds, close to 100 seconds.


Thread stack follows behind:

```
"OkHttp http://127.0.0.1:8080/..." #1018 prio=5 os_prio=31 tid=0x00007fa803526000 nid=0x83103 runnable [0x000070004f9ec000]
   java.lang.Thread.State: RUNNABLE
	at java.net.SocketInputStream.socketRead0(Native Method)
	at java.net.SocketInputStream.socketRead(SocketInputStream.java:116)
	at java.net.SocketInputStream.read(SocketInputStream.java:170)
	at java.net.SocketInputStream.read(SocketInputStream.java:141)
	at okio.Okio$2.read(Okio.java:138)
	at okio.AsyncTimeout$2.read(AsyncTimeout.java:238)
	at okio.RealBufferedSource.indexOf(RealBufferedSource.java:325)
	at okio.RealBufferedSource.indexOf(RealBufferedSource.java:314)
	at okio.RealBufferedSource.readUtf8LineStrict(RealBufferedSource.java:210)
	at okhttp3.internal.http.Http1xStream.readResponse(Http1xStream.java:186)
	at okhttp3.internal.http.Http1xStream.readResponseHeaders(Http1xStream.java:127)
	at okhttp3.internal.http.CallServerInterceptor.intercept(CallServerInterceptor.java:53)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:92)
	at okhttp3.internal.connection.ConnectInterceptor.intercept(ConnectInterceptor.java:45)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:92)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:67)
	at okhttp3.internal.cache.CacheInterceptor.intercept(CacheInterceptor.java:109)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:92)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:67)
	at okhttp3.internal.http.BridgeInterceptor.intercept(BridgeInterceptor.java:93)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:92)
	at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(RetryAndFollowUpInterceptor.java:124)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:92)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.java:67)
	at okhttp3.RealCall.getResponseWithInterceptorChain(RealCall.java:170)
	at okhttp3.RealCall.access$100(RealCall.java:33)
	at okhttp3.RealCall$AsyncCall.execute(RealCall.java:120)
	at okhttp3.internal.NamedRunnable.run(NamedRunnable.java:32)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)

```


###  CloseableHttpAsyncClient


When using CloseableHttpAsyncClient, it only cost 8 threads to handle 1000 concurrency.

It takes 101.103 seconds, close to 100 seconds.


Thread stack follows behind, indeed **nio select**:


```
"I/O dispatcher 8" #24 prio=5 os_prio=31 tid=0x00007fd54f0f2000 nid=0x6f03 runnable [0x000070000ba7e000]
   java.lang.Thread.State: RUNNABLE
	at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
	at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
	at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
	at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
	- locked <0x000000076b0cc5b8> (a sun.nio.ch.Util$2)
	- locked <0x000000076b0cc5a8> (a java.util.Collections$UnmodifiableSet)
	- locked <0x000000076b0cc488> (a sun.nio.ch.KQueueSelectorImpl)
	at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
	at org.apache.http.impl.nio.reactor.AbstractIOReactor.execute(AbstractIOReactor.java:255)
	at org.apache.http.impl.nio.reactor.BaseIOReactor.execute(BaseIOReactor.java:104)
	at org.apache.http.impl.nio.reactor.AbstractMultiworkerIOReactor$Worker.run(AbstractMultiworkerIOReactor.java:588)
	at java.lang.Thread.run(Thread.java:745)
```


### Netty4

When using Netty4ClientHttpRequestFactory, it cost 16 threads to handle 1000 concurrency.

It takes 100.892 seconds, close to 100 seconds.


Thread stack follows behind, indeed **nio select**:


```
"nioEventLoopGroup-2-16" #29 prio=10 os_prio=31 tid=0x00007fdd33232000 nid=0x7903 runnable [0x000070000e941000]
   java.lang.Thread.State: RUNNABLE
	at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
	at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
	at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
	at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
	- locked <0x000000076f5b01f8> (a io.netty.channel.nio.SelectedSelectionKeySet)
	- locked <0x000000076f5b0218> (a java.util.Collections$UnmodifiableSet)
	- locked <0x000000076f5b01a8> (a sun.nio.ch.KQueueSelectorImpl)
	at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
	at io.netty.channel.nio.NioEventLoop.select(NioEventLoop.java:759)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:400)
	at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:873)
	at io.netty.util.concurrent.DefaultThreadFactory$DefaultRunnableDecorator.run(DefaultThreadFactory.java:144)
	at java.lang.Thread.run(Thread.java:745)

```


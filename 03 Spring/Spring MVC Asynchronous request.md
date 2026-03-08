
스프링 MVC에서는 Asynchronous Request를 처리를 위한 Integration을 제공해준다.
컨트롤러 메소드의 반환타입으로 비동기 반환값을 사용할 수 있다. 

ex) DefferedResult, Callable, WebAsyncTask, CompletableFuture 등

문서에서는 CompletableFuture에 대한 설명은 별도로 없고, 대신 DefferedResult 위주로 설명이 되어있다.

```
ListenableFuture<V>, CompletableFuture<V>
Alternative to DeferredResult, as a convenience (for example, when an underlying service returns one of those).
```

DeferredResultMethodReturnValueHandler 파일을 보면 해당 핸들러가 ListenableFuture타입을 지원하고, Future에 Callback을 추가해서 결국 DefferedResult에 setResult를 한다. (일종의 Adapter)

그래서 DefferedResult 처리할 때와 같은 동작을 한다.

---


### 동작

1. 컨트롤러에서 비동기 타입이 반환된다. (하지만 실제 비동기 작업은 아직 완료되지 않음)
2. WebAsyncManager.startDeferredResultProcessing 실행
   2-1. request.async 실행 → request를 ASYNC상태로 지정함
   2-2. **ASYNC 상태로 지정된 request는 쓰레드와 필터를 빠져나간다. 하지만 사용자에게 응답은 가지 않는다.**
   2-3. deferredResult에 콜백을 걸어놓는다. (콜백이름 : setResultHander, setResult가 호출되면 해당 콜백이 실행됨)
   2-4. 이 콜백을 통해서 나중에 비동기 작업이 완료되면, 실제 Response가 전달된다. (전달될 때는 DispatcherServlet을 다시 거친다.)

### 쓰레드

WebAsyncManager라는 클래스가 이 작업을 대부분 전담하게 되는데, 해당 클래스의 주석설명을 보면
T1, T2, T3 3개의 쓰레드에 대한 설명이 나옵니다.

- T1. 최초 요청들어오는 쓰레드
   - 비동기 타입을 반환받고 이 쓰레드는 서블릿과 필터를 빠져나감.

- T2. 비동기 작업을하는 쓰레드

   - CompletableFuture를 쓴다면 forkJoinPool에 있는 쓰레드, 
   - DefferedResult라면 이후 setResult를 수행하는 쓰레드를 의미하는 것 같습니다.

- T3. 비동기 작업이 완료되었을 때 Response를 반환하는 역할을 하는 쓰레드.
   - 비동기 작업이 완료되고 다시 DispatcherServlet으로 요청이 진입했을 때, 이것을 클라이언트에게 반환합니다.

<details>
  <summary>주석 원문</summary>
  
  An async scenario starts with request processing as usual in a thread (T1).  
  Concurrent request handling can be initiated by calling <b>startCallableProcessing</b> or <b>startDeferredResultProcessing</b>, both of which produce a result in a separate thread (T2).  
  The result is saved and the request dispatched to the container, to resume processing with the saved result in a third thread (T3).  
  Within the dispatched thread (T3), the saved result can be accessed via <b>getConcurrentResult()</b> or its presence detected via <b>hasConcurrentResult()</b>.
</details>

### 웹플럭스와 MVC 비동기 요청 처리의 차이

MVC와 웹플럭스 둘 다 비동기와 리액티브 타입의 반환을 지원한다.
하지만 MVC는 비동기 작업을 처리하기 위해 분리된 쓰레드가 필요하며, I/O 동안 blocking 상태에 있는 쓰레드가 생기게 된다. 
반면 웹플럭스는 논블로킹 I/O에 의존하며, I/O작업을 하기 위한 추가 쓰레드가 필요하지 않는다. 

However, individual writes to the response remain blocking (and are performed on a separate thread), unlike WebFlux, which relies on non-blocking I/O and does not need an extra thread for each write.

---
참고자료
https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html#page-title
https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html




```java
WebAsyncManager.java 

//...

public void startDeferredResultProcessing(final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {
	//....	
	
	
	// request.startAsync() 호출
	// request가 asynchronous 상태가 된다.
	// 이렇게 상태를 변경하면 서블릿 스레드가 서블릿과 필터를 빠져나간다. 
	startAsyncProcessing(processingContext); 
	try {
		interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
		deferredResult.setResultHandler(result -> {
			result = interceptorChain.applyPostProcess(this.asyncWebRequest,
										 deferredResult, 
										 result
									 );
			setConcurrentResultAndDispatch(result);
		});
	}
}	

setResultHandler
-> DefferedResult의 resultHandler를 설정한다. 

if (resultToHandle == RESULT_NONE) {  
	// No result yet: store handler for processing once it comes in  
	this.resultHandler = resultHandler;  
	return;  
}

이후 setResult가 호출이 되면 해당 람다식을 실행. 즉,
-> 인터셉터후처리 (applyPostProcess)
-> setConcurrentResultAndDispatch(result)

비로소 해당 결과를 Dispatch함.


DefaultAsyncServerResponse.java

public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)  
throws ServletException, IOException {  
  
	//startDeferredResultProcessing
	writeAsync(request, response, createDeferredResult(request));  
	
	return null;  // 비동기 작업을 하라고 시킨 후, 그냥 null을 return함.  
}


```



```java
* The central class for managing asynchronous request processing, mainly intended  
* as an SPI and not typically used directly by application classes.  
*  
* <p>An async scenario starts with request processing as usual in a thread (T1).  
* Concurrent request handling can be initiated by calling  
* {@link #startCallableProcessing(Callable, Object...) startCallableProcessing} or  
* {@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing},  
* both of which produce a result in a separate thread (T2). The result is saved  
* and the request dispatched to the container, to resume processing with the saved  
* result in a third thread (T3). Within the dispatched thread (T3), the saved  
* result can be accessed via {@link #getConcurrentResult()} or its presence  
* detected via {@link #hasConcurrentResult()}.  
*

public final class WebAsyncManager {
	// ...
}
```



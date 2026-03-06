
스프링 MVC에서는 Asynchronous Request를 처리를 위한 Integration을 제공해준다.
컨트롤러 메소드의 반환타입으로 비동기 반환값을 사용할 수 있다. 

ex) DefferedResult, Callable, WebAsyncTask 등

컨트롤러의 반환값이 비동기 타입일 경우에 디스패처 서블릿에서 
request.startAsync() 메서드를 호출한다.

이게 호출되면 필터-서블릿 체인을 바로 Exit하고, 
Response는 Open 상태로 남아 있는다.



--- 
참고

https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html
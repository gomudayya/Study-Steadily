## Intro

에러코드를 도입하기 전에 리팩토링을 먼저 끝내고, 커스텀을 진행했다.

커스텀 하기 앞서, `Restdocs`의 코드 분석이나, 사전에 알아야할 내용은 [Restdocs 코드 뜯어보기](https://github.com/gomudayya/DevelopNote/blob/main/Spring/Restdocs/Restdocs%20%EC%BD%94%EB%93%9C%20%EB%9C%AF%EC%96%B4%EB%B3%B4%EA%B8%B0.md) 에서 정리하였다.

그래서 이번 글은 구체적으로 어떻게 커스텀을 했는지에 대한 내용이다.

## Snippet 파일 정의하기

`test/resources/org/springframework/restdocs/templates/` 하위 경로에 all-error-codes.snippet.snippet 파일을 만들었다.

이 파일은, 스니펫의 양식을 정의하는 파일인데 머스타치 문법으로 되어있다.

사실 조금 후회하는것은 머스타치보다는 타임리프를 이용해서 커스텀하면 어땠을까 하는 점이다.

(Restdocs에서 템플릿엔진도 바꿀 수 있는것 같음)

왜냐하면 아래 보다시피 머스타치 문법이 워낙 가독성이 떨어지는거 같아서.....

```
{{#allErrorCodes}}

=== {{errorType}}

|====
|에러코드|HTTP 상태코드|설명

{{#errorCodes}}
|{{#tableCellContent}}{{code}}{{/tableCellContent}}
|{{#tableCellContent}}{{httpStatus}}{{/tableCellContent}}
|{{#tableCellContent}}{{description}}{{/tableCellContent}}
{{/errorCodes}}

|====

{{/allErrorCodes}}
```

- `{{변수이름}}` : 머스타치에서는 템플릿엔진의 변수를 이런식으로 처리한다.

- `{{#allErrorCodes}} ... {{/#allErrorCodes}}` 전체에 대한 반복 블록이다.

  - 머스타치는 `{{#key}}` `{{/key}}` 구문을 이용해서 반복문을 정의한다.
 
- `==`, `===` 이것들은 머스타치에서 제목형식을 나타내는 것이다 `=`의 갯수가 작을수록 대제목이다.

  - HTML로 치면 `=` `==` `===` 를 H1 , H2 , H3 로 생각하면 된다.
    
- `(|==== ... |====)` 표에대한 Block을 나타낸다.

<br>

결국 이게 어떤Snippet인지 결과본을 올려보면

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/c6c2d956-3210-44a4-a672-e5d17bd649a8)

요런 형태이다!

에러타입별로 에러코드를 분류하는것이 더 깔끔한 API문서를 만들 수 있을거라 판단해서, 이런식으로 스니펫을 구성해보았다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/a6cca542-bd23-42b5-b6ba-66f71e714998)

## 문서화를 위한 컨트롤러/테스트코드 작성하기

Restdocs는 컨트롤러 계층의 테스트코드가 있어야만 문서화를 할 수 있다.

따라서 껍데기만 있는 컨트롤러와, 껍데기만 있는 테스트코드를 테스트 패키지에 작성하였다. 

내가 하려고했던 것은 어떤 API의 Request와 Response를 토대로 문서화하는 것이 아니라

에러코드를 문서화하는 것이기 때문에, 그냥 껍데기만 있는 형태로 만드는 것이 더 괜찮다고 판단했다.

- 컨트롤러 껍데기

  ![image](https://github.com/gomudayya/DevelopNote/assets/129571789/3885c5df-2652-4b0e-a871-fc77ff91267c)

- 테스트코드 껍데기

  ![image](https://github.com/gomudayya/DevelopNote/assets/129571789/de1b7205-6630-48e5-9ad8-4fcf9c7e9952)

  - `AllErrorCodesSnippet.buildSnippet()` : AllErrorCodeSnippet` 클래스를 생성해서 반환합니다.

     해당 클래스에 대한 구체적인 설명은 아래에서
    

## 커스텀 Snippet 클래스 작성하기

우선 `TemplatedSnippet` 클래스를 상속받는것이 하다.

**그리고 가장 중요한 일인 `Map<String, Object> createModel(Operation operation)` 메서드를 구현해야 한다.**

참고로 별도의 dto를 사용하지 못하고, Map<String, Object>의 형태로 내려줘야하기 때문에 한곳에서 몰아서 작성하면 지저분한 코드가 된다.

속성별로 메서드를 잘 분리해서 구현하는 것이 좋다..

구체적인 설명은 아래 코드의 주석으로 달아놓았다.

```java
public class AllErrorCodesSnippet extends TemplatedSnippet {
    public AllErrorCodesSnippet() {
        /*
        첫번째 파라미터       : 스니펫 이름을 정의합니다. 이 이름을 토대로, 우리가 만든 경로의 .snippet 파일을 찾아갑니다.
        두번째 파라미터(null) : Map<String, Object> attributes 라는 상위타입의 멤버변수를 초기화합니다.
                               restdocs에서는 attributes와 createModel 메서드의 반환결과를 합쳐서 템플릿을 렌더링합니다.
                               createModel만 잘 구현하면 되서 별도로 정의할 필요는 없습니다.
        */
        super("all-error-codes", null); 
    }

    public static AllErrorCodesSnippet buildSnippet() {
        return new AllErrorCodesSnippet();
    }

    @Override
    /*
    가장 중요한부분입니다. map의 key값이 이전에 작성했던 snippet 파일의 변수값과 대응됩니다.
    오히려 타입을 적게되면 복잡해져서 더 헷갈릴 수 있으니, 하위요소들도 Object로 가져가는것이 더 좋습니다.
    Json자료형을 어떻게 자바의 Map<String, Object>로 표현할수 있을까? 로 접근해보면 한결 헷갈리지 않고 작성할 수 있습니다.
    */
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("allErrorCodes", generateAllErrorCodes()); //error-codes.snippet의 {{allErrorCodes}} 에 대응되는 Map 자료형을 만듭니다. 머스타치 문법 참고.
        return model;
    }

    /*
    에러타입을 순회하며, 에러타입과 표에대한 내용을 채웁니다.
    */
    private List<Object> generateAllErrorCodes() {
        List<Object> allErrorCodes = new ArrayList<>();

        for (ErrorType errorType : ErrorType.values()) {
            Map<String, Object> map = new HashMap<>();

            map.put("errorType", errorType.getDescription());
            map.put("errorCodes", generateErrorCodes(errorType));

            allErrorCodes.add(map);
        }

        return allErrorCodes;
    }

    /*
    표의 구체적인 내용(code, httpStatus, description)을 채워줍니다. 
    */
    private List<Object> generateErrorCodes(ErrorType errorType) {
        List<Object> errorsByErrorType = new ArrayList<>();

        for (ErrorCode errorCode : ErrorCode.getErrorCodes(errorType)) {
            Map<String, Object> error = new HashMap<>();

            error.put("code", errorCode.name());
            error.put("httpStatus", errorCode.getStatusCode());
            error.put("description", errorCode.getDescription());

            errorsByErrorType.add(error);
        }

        return errorsByErrorType;
    }
}
```

참고로 ErrorCode의 Enum 은 아래와 같은 형식으로 되어있다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/d6db46b0-fe3a-4eb0-a4e9-2c6c7c2432b6)

- 각각의 Enum상수들은 에러타입(분류), Http 상태코드, 에러메시지, description을 가지고있다.

- 별도의 추가설명이 필요치 않을 때에는 에러메시지로 description을 대체하지만,

  추가 설명이 필요할때에는 description의 설명을 넣어 API문서에 작성하도록 하였다.

- **이렇게하면 에러코드 상수를 하나 늘릴때마다, API문서에 자동으로 반영된다 !**

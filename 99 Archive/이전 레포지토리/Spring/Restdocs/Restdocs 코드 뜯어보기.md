# Restdocs 코드 뜯어보기

프로젝트를 하면서 API 문서화도구로 Spring Restdocs를 사용하는데,

에러코드를 문서화하는 과정에서 기존 Snippet만으로 만족스러운 문서화가 되지않아 커스텀을 해보기로 했다.

그 과정에서 코드를 많이 뜯어보게 되었는데, 정리해보면 꽤 도움이 될 것 같다.

**우선 Restdocs를 커스텀하기 위해 가장 중점적으로 뜯어봐야할 인터페이스는 `Snippet` 이다.**

## Snippet 인터페이스

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/3c5895b8-fc79-4ee4-8b67-eac557e5437b)

이 인터페이스는 Restdocs의 각각의 스니펫과 대응된다.

Restdocs를 생성하는 과정이 담겨있는 RestDocumentationGenerator 클래스에 가보면

**스니펫 목록들을 찾아와서 document메서드를 호출**하는 코드가 있다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/3ef75aac-5137-4730-aae3-869afc57b6a9)

요런 방식으로 스니펫들이 쓰이고, Restdocs의 문서화가 진행된다.

document의 메서드 파라미터로 존재하는 Operation이 궁금할 수 있는데, 

이것은 API 호출로 발생한 Request, Response 등에 대한 정보를 가지고있는 인터페이스이다. 

이 Operation으로 API의 정보를 알아내고 문서화한다.

## Templated Snippet (abstract클래스)

이 클래스는 Snippet 인터페이스 바로 밑에서 document 메서드를 구현한다.

**이름에 Templated가 있는 이유는 템플릿 메소드 패턴이 사용되었기 때문이다.**

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/412005a6-d5cc-458b-bf20-b360a5a349a7)

주석에 쓰인대로 이 클래스는 모든 스니펫들의 Base클래스로서의 역할을한다.

필드부터 살펴보면 `attribute`, `snippetName`, `templateName` 이 있다.

- `String snippetName`

  - 해당 스니펫과 대응되는 스니펫파일의 이름을 적어야한다.

    (참고로 스니펫 파일은 `test/resources/org/springframework/restdocs/templates/` 경로에 만들어야한다.)

  - 이 snippetName 정보를 통해서, 어떤 스니펫파일에 렌더링을 해야하는지 결정한다.
 
- `String templateName` (이 부분은 확실하진 않음)

  - 템플릿엔진의 이름을 적는 필드같다.
  
  - Restdocs에서는 디폴트로 Mustache 템플릿엔진을 사용하는데 다른 템플릿엔진을 사용할 때 기입하는 필드같다.
 
  - 근거는 `writer.append(templateEngine.compileTemplate(this.templateName).render(model));` 요 코드

- `Map<String, Object> attributes`

  - 스프링 Restdocs에서는 템플릿엔진을 이용해서 API문서를 렌더링한다.
  
  - 이 때 템플릿파일의 변수에 대응되는값들이 필요한데 그것을 정리해놓은것이 이 attributes이다.
 
  - **Restdocs를 계속 뜯어보면 이외에도 `Map<String, Object>` 형식의 자료형이 자주 나온다.**
  
    **마찬가지로 템플릿파일의 변수에 대응된다. 커스텀할 때 중요한 내용이니 기억해놓자.**

<br>
<br>

그 다음으로는 메서드들을 살펴보자.

중요한것 두개만 살펴보면 된다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/643578ea-41e3-42e4-86b6-45d1e9f578d3)


- `public void document(Operation operation)`

  - Snippet 인터페이스를 구현. 템플릿메소드의 골격이다.
 
  - 직접 문서에 writing을 하는 부분이다.
  
  - **`createModel(operation)` 메서드를 통해 받아온 Map<String, Object> model과 필드에있는 attributes를 합친다. (putAll)**
 
  - **이렇게 합쳐진 model은 템플릿파일의 변수에 대응시키는데 사용된다.**
  
- `protected abstract Map<String, Object> createModel(Operation operation)`

  - 템플릿 메서드 패턴의 추상메소드이다. 하위 클래스에 의해 재정의된다.
 
  - **즉 `TemplatedSnippet` 하위의 클래스들이 이 메서드를 어떻게 구현했는지가 가장 중요한 포인트이다.**

## API 포맷의 공통점을 묶은 Abstract 계층

`TemplatedSnippet` 하위에는 5가지의 Abstract클래스가 존재한다.

Field형식의 스니펫, 쿼리파라미터 형식의 스니펫, Body형식의 스니펫 등 여러 API 포맷에 따른 스니펫을 만들기 위함이다.

Restdocs에서는 이런 다양한 API포맷의 공통점을 묶어서 또 한번 추상화 계층을 두었다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/85149c5e-17c5-4db8-b9c1-95cb857de23e)

요런식으로...

그리고 각 계층들을 살펴 보면 멤버변수로 `FieldDescriptor`, `HeaderDescriptor` `ParameterDescriptor`등등

각각의 포맷에 맞는 `Descriptor`를 가지고 있다. 

이 Descriptor를 이용해서 `Map<String, Object> createModel(Operation operation)` 메서드를 구현한다.

```java
@Override
	@Override
	protected Map<String, Object> createModel(Operation operation) {
    		// ... API 포맷에 적절한 validation 로직들

		Map<String, Object> model = new HashMap<>();
		List<Map<String, Object>> fields = new ArrayList<>();
		model.put("fields", fields);
		for (FieldDescriptor descriptor : descriptorsToDocument) {
			if (!descriptor.isIgnored()) {
				fields.add(createModelForDescriptor(descriptor));
			}
		}
		return model;
	}
```

이 Descriptor안에는 템플릿 엔진의 변수에 대응될 속성들이 존재하고, 이를 이용해서 `Map<String, Object> model` 을 만든다.

추가로 이 외에도 각각의 포맷에 적절한 Validation 로직들이 존재한다.

## Concrete 계층 

그 다음이 가장 밑단의 `Concrete` 계층이다. 직접적인 구현체들이 포진되어있다.

![image](https://github.com/gomudayya/DevelopNote/assets/129571789/48afbae8-5c05-4036-941f-38e6291bc973)

예시로 몇가지만 추가했는데, 훨씬 더 많은 Snippet들이 존재한다. 

**그리고 위에서 미처 언급하지 않았는데, AbstractSnippet 계층에서도, 추상 메서드가 존재하고 템플릿 메소드 패턴이 사용된다.**

이 때는 `Map<String, Object> createModel(Operation operation)`이 골격을 담은 템플릿 메소드이고,

그 과정에 **구체적인 스니펫별로 다르게 가져가야할 부분을 추상메소드로 분류**하였다.

## 아니 그래서 커스텀은 어떻게 할건데요..

커스텀은 어떻게 해야한가? 에 대한것은 

**위의 상속구조에서 어떤 컴포넌트를 상속받을건지, 그리고 추상메소드를 어떤식으로 구현할건지가 관건이다.**

**이것은 SpringRestdocs 에서만 국한된것이 아니라, 다른 스프링 계열의 기술에서도 동일하게 적용된다.**

결론적으로 말하자면 나는 `TemplatedSnippet`클래스를 상속받아서 커스텀했다.

그 이유는 다음과 같다.

- `TemplatedSnippet` 하위의 템플릿을 상속받는 것은

  기존 스니펫 양식에서, 무언가 추가적인 기능을 넣거나 수정이 필요할때 하는것이 좋다고 판단했다.

- 내가 커스텀하고자 하는 스니펫의 형태는, 완전 새로운 형태로, 기존의 스니펫을 상속받아 활용하기가 어려웠다.

구체적인 커스텀 절차는 아래링크에서 정리했다.

[에러코드 도입기 (2) Restdocs Snippet커스텀 및 에러코드 자동화하기.md](https://github.com/gomudayya/DevelopNote/blob/main/%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8/%EB%8B%A4%EC%9D%BC%EB%A6%AC/%EC%97%90%EB%9F%AC%EC%BD%94%EB%93%9C%20%EB%8F%84%EC%9E%85%EA%B8%B0%20(2)%20Restdocs%20Snippet%EC%BB%A4%EC%8A%A4%ED%85%80%20%EB%B0%8F%20%EC%97%90%EB%9F%AC%EC%BD%94%EB%93%9C%20%EC%9E%90%EB%8F%99%ED%99%94%ED%95%98%EA%B8%B0.md)

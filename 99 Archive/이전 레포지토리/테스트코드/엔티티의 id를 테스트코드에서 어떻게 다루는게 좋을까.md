# 엔티티의 id를 테스트코드에서 어떻게 다루는게 좋을까

## Intro

테스트 코드에서 엔티티의 ID 값을 비교해야 하는 상황이 있다

문제는 **테스트 픽스쳐를 만들 때 id값을 어떻게 지정하냐**는 것이다. 

보통 엔티티객체에는 id에 대한 생성자나 Setter를 열어놓지 않는다.

엔티티객체의 id값은 보통 JPA의 `@GenerateValue`을 이용해서 할당한다. 

DB차원에서 이미 결정한 id를 어플리케이션에서 개발자가 id를 수정할 수 있도록 여지를 주어서는 안된다.

따라서 엔티티객체에서는 엔티티객체를 생성할 때에도 id값을 지정할 수 없고, 생성된 이후에도 id값을 수정할 수 없다.

## 그러면 어떻게?

DB 레벨의 테스트를 하는경우라면 그냥 DB에 넣고 GenerateValue로 생성된 id를 사용하면 된다.

하지만 서비스레벨의 단위 테스트를 하는 경우라면 어쩔수없이 Fixture를 사용해야 한다.

이 때 id값을 지정하는 방법에는 두 가지 정도가 있다.

### Spy 객체 사용 + stubbing

```java
    public static List<PostLike> 좋아요_이력_리스트(int postIdStart, int postIdEnd) {
        List<PostLike> result = new ArrayList<>();

        for (int i = postIdStart; i <= postIdEnd; i++) {
            Post post = Mockito.spy(Post.builder().build()); //spy 객체 생성

            when(post.getId()).thenReturn((long) i); // stubbing

            result.add(PostLike.builder().post(post).build());
        }

        return result;
    }
```

위의 코드는 Fixture를 생성하는 코드인데, `Mockito.spy`를 이용해서 Post의 spy객체를 만들었다.

spy객체는 실제 객체와 똑같이 동작하는데, 특정 메서드에 한해서 Mocking이나 Stubbing을 이용할 수 있다.

따라서 spy객체를 만들고, `getId()` 메서드를 stubbing해서 자신이 원하는 id값을 부여하면 된다.


### ReflectionTestUtils

스프링 프레임워크안에는 `ReflectionTestUtils` 라는 클래스가 있다. 

테스팅을 도와주는 유틸클래스로서 다양한 리플렉션작업을 편리하게 할 수 있게 해준다.

```java
    public static Dailry 일반회원1이_작성한_다일리() {
        Dailry 다일리 = Dailry.builder()
                .title(다일리_제목)
                .member(일반회원1())
                .build();

        ReflectionTestUtils.setField(다일리, "id", DAILRY_ID);
        return 다일리;
    }
```

위의 코드는 마지막에 `ReflectionTestUtils` 를 이용해서 `id` 필드를 `DAILRY_ID` (상수) 로 지정하였다.

## 뭘 쓰는게 좋을까

잘 모르겠다. 다만 `ReflectionTestUtils` 같은 경우에는 수정할 필드를 문자열으로 지정한다.

이렇게 매직문자열로 저장해놓는 방식은 추후에 유지보수하기 불편할수도 있다. 

반면 `Mockito.spy()`는 메서드체이닝으로 계속 연결해서 IDE의 도움을 받기도 쉽다. 

그리고 리플렉션 자체가 정상적인 방법이라고 보기는 쪼끔 어려우니.. `Mockito.Spy()`를 사용하는게 좋은것 같다..

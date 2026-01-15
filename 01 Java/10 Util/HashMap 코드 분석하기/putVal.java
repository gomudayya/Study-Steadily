    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        // 지역변수 선언 및 할당
        // 현재 테이블이 초기화되어 있지 않으면, resize()함수로 초기화 하고, 현재 해시테이블의 사이즈를 n에 할당
        // tab = 해시 테이블
        //  n  = 해시 테이블의 사이즈
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;

        // n이 2의 제곱수일 때 아래 식이 성립
        // (n-1) & hash == hash % n, n은 항상 2의 제곱수이다. (필드 주석 : When allocated, length is always a power of two.)

        // 예시
        // n                 = 001000000 (2)
        // n-1               = 000111111 (2)
        // hash              = 111010101 (2)
        // (n-1) & hash      = 000010101 (2)

        // a % n = 0 ~ n-1 사이의 어떤수
        // n이랑 같거나 왼쪽에 있는 비트는 어차피 n으로 나누어 떨어지기 때문에 의미가 없다.
        // n보다 오른쪽에 있는 비트들이 중요하다. n 오른쪽에 있는 비트들을 모두 포함시키기 위해서는 111...11 (2) 과 같은 형태여야 한다
        
        
        // 모듈러 연산으로 해시값을 테이블 사이즈에 맞는 인덱스로 변환
        // i = 해시값와 매핑되는 배열 인덱스 (해시 인덱스)
        // p = tab[i] 즉, 해시 인덱스 위치에 이미 있는 노드를 변수 p에 할당.
        // p == null 이면 (해시 인덱스 위치에 노드가 없으면)
        // 바로 tab[i] 에 파라미터로 넘어온 값을 할당
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null); // 간단한 케이스
        else {
            // 이 때는 해시 인덱스 위치에 이미 값이 있는 상황이고, 현재 그 값은 p임. p = 해시 인덱스 위치에 있는 노드.

            // 1)Map 에 동일한 키가 이미 존재하는 상황 하고, 2)해시 인덱스 위치에 값이 있는상황하고 구별이 필요하다.
            // 1번의 경우는 전자는 덮어쓰기로 처리하거나(onlyIfAbsent == false) 그냥 그대로 냅두거나(onlyIfAbsent == true) 할 수 있다.
            // 2번의 경우는 해시충돌의 상황이다. 1번 상황은 2번 상황에 포함된다. 해시충돌된 노드(연결리스트, 레드블랙트리)를 탐색하면서 1번의 상황을 만날 수도 있다.

            Node<K,V> e; K k;

            // 1번 상황. 동일한 키가 이미 존재하고 있고, 그게 p임. e = p 를 대입.
            // 빠른 처리
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;

            //p가 TreeNode(레드블랙트리의 노드) 의 인스턴스 일 때 처리.
            // putTreeVal 값도 putVal 메서드와 같이 previous value(existing value) 를 반환한다.
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);

            //p가 연결리스트의 인스턴스일 때 처리
            else {
                for (int binCount = 0; ; ++binCount) { //무한 루프 - break 구조. 연결리스트의 순회

                    // e = p.next 이다.
                    // 연결리스트의 다음 노드가 null일 때는 새로운 노드를 만들어서 연결한다.
                    // 새로운 노드를 포함해 노드 갯수가 9개가 됐으면, 레드블랙트리로 전환한다. (treeifyBin) TREEIFY_THRESHOLD = 8
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }

                    // 현재 e = p.next 인것을 기억.
                    // 연결리스트를 순회하면서 동일한 키가 이미 존재하는 상황일 때이다.
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;

                    //p 값을 변경하면서, 연결리스트 순회
                    p = e;
                }
            }
            
            //e = Map에 동일한 키가 이미 있었을 때, 해당 키값을 갖고 있는 노드 값(existing). 위의 로직을 통해 e값을 설정함. 동일한 키가 없을 때는 null 이다.
            if (e != null) { // existing mapping for key
                V oldValue = e.value;

                // !onlyIfAbsent    : 해당 key가 있을 때, 덮어쓰기 허용.
                // oldValue == null : 해당 key가 있는데, value값이 null일때는 덮어쓴다.
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e); // 후처리 메서드. 하위 구현체(LinkedHashMap) 에서 오버라이딩해서 사용
                return oldValue; // 여기서 리턴, 수정이나 리사이즈 코드 실행안함. 해시 테이블의 슬롯(bin, 버킷) 에 추가된 게 아니기 때문에
            }
        }

        //아래 로직이 돌아가는 상황은, 해당 키가 맵에 존재하지 않았을 때 이다.
        //(해시인덱스 위치에 값이 없거나, 해시충돌된 노드를 따라 탐색했는데, existing 값이 null이 나왔을 때)
        ++modCount; // 수정횟수 증가(modifyCount)
        if (++size > threshold) // 임계치를 넘기면, 해시테이블 리사이징
            resize();
        afterNodeInsertion(evict); // 후처리 메서드. 하위 구현체(LinkedHashMap) 에서 오버라이딩해서 사용
        return null;
    }

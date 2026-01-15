    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length; // 과거 테이블 사이즈
        int oldThr = threshold;                            // 과거 임계치
        int newCap, newThr = 0;
        
        // 새 테이블 사이즈(newCap)와 새로운 임계치 값(newThr) 을 구하는 로직
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) { // Max Capacity(2^30)에 도달했을 때에는
                threshold = Integer.MAX_VALUE; // threshold 를 int 최대값으로 설정 (리사이즈 불가능. 10억개 버킷 + 체이닝)
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)   // 억지로 해시맵 초기화 할 때, 사이즈를 DEFAULT 보다 작게 잡아서 생성한다면, 이 로직 안탈듯
                newThr = oldThr << 1; // double threshold // threshold 값은 로드팩터 * 사이즈인데, 사이즈가 항상 *2 로 늘어나서 이렇게 처리하는 듯
            //caseA : 아무조건도 타지 않음.
        }
        //caseB
        else if (oldThr > 0) // initial capacity was placed in threshold .
                            // 처음 capacity 는  threshold 값으로
                            // 어떤 생성자를 통해 생성했는 지에 따라서 threshold 값이 0일수도 있고, 아닐수도 있음
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
                            // threshold 값도 0이면 default 값 으로 할당
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        
        // newThr 값을 구하지 못했을 때 처리 (caseA, caseB 두 곳에서 넘어옴)
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?  // ft < (float)MAXIMUM_CAPACITY 이거는 로드팩터가 1일때, 방어로직 같은데
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        
        
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap]; // 새로운 해시테이블 생성
        table = newTab;
        //리사이징으로 늘어난 사이즈를 고려해, 노드들의 위치를 재배치 하는 로직
        if (oldTab != null) {
            
            // 이전 해시테이블 순회
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null; // 이거는 왜 하는지 잘 모르겠네...? 어차피 노드를 newTab 에서 사용해서 GC는 안될텐데

                    //노드가 하나만 있을 때 처리. e.hash % newCap 위치에 노드를 넣는다.
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;

                    //레드블랙트리 일 때, 재배치 로직?
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order

                        //연결리스트 다룰 때는, head를 가르키는 변수를 미리 선언해놔야 나중에 옮기기가 가능함.
                        //head는 처음에 할당만해주고, 순회를 하거나 수정을 할 때는 tail을 타고가면서 처리하면 됨.
                        //여기 while 로직은 좀 헷갈려서, 그림으로 이해하는게 편함.
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;

                            // ex) oldCap = 16, newCap = 32, e.hash = 21인 상황에선 해시 인덱스 값이 바뀌어야 하기 때문에 위치를 옮겨야 한다.
                            // 즉 e.hash % 32 값이 16~31이 나오면, 위치를 옮겨야 한다.
                            // e.hash & oldCap == 0 이면, 원래 위치에 두어야한다. (loHead)
                            // e.hash & oldCap != 0 이면, 위치를 위로 옮겨야한다. (hiHead)
                            // 아래로직은 링크드리스트를 원래 해시테이블 위치에 둘 것(loHead)과, 상위 인덱스의 해시테이블로 올릴 것(hiHead) 두개로 분리 (그림으로 이해)

                            // &연산에 대한 설명
                            // oldCap   =   10000 (2)
                            // e.hash   = 1101101 (2)

                            // oldCap   =   10000 (2)
                            // e.hash   =   01101 (2)

                            // oldCap   =   10000 (2)
                            // e.hash   =   10110 (2)

                            // oldCap 과 같은위치에 비트가 있으면, 위치를 위로 올려야 하고, 그렇지 않으면 원래 위치에 있어도 된다.
                            // (노드의 위치 이동을 할지 안할지 구분하는 조건)
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);

                        // 재배치
                        if (loTail != null) {
                            loTail.next = null; // 이거를 해줘야 뒤에 달려있는 노드들을 짜를수 있음
                            newTab[j] = loHead; // 기존 해시테이블 인덱스에 두는 링크드리스트
                        }
                        if (hiTail != null) {
                            hiTail.next = null; // 이거를 해줘야 뒤에 달려있는 노드들을 짜를수 있음
                            newTab[j + oldCap] = hiHead; // 상위 해시테이블 인덱스로 이동시키는 링크드리스트
                        }
                    }
                }
            }
        }
        return newTab;
    }

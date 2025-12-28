
- Evacuation 이주, 대피
	- Evacuate 대피시키다
	- Mark & Copy 방식의 GC 알고리즘을 쓸 때, 다른 영역으로 Evacuation 한다고 표현함.
	
- Reclamation
	- Reclaim 되찾다. 회수하다
	- G1 GC의 Space Reclamation 단계

- Promotion
	- Promote 승격하다. 승진하다

- Tenured
	- 테뉴어드, Old Generation (구세대)을 의미

- Humongous
	- 거대한
	- G1 GC의 거대 객체 영역 = Humongous Region

- IHOP (Initiating Heap Occupancy Percent)
	- Concurrent Marking을 시작(initiate)할 힙 사용률 퍼센트

- Incremental
	- 증가하는 증분의
	- Incremental Update (삼색마킹 문제해결 방법, 새로 추가되는 참조를 정보를 저장)

- contiguous
	- 인접한 근접한 
	- Note that the regions are not required to be contiguous like the older garbage collectors.

- piggybacked
	- 업혀서, 동시에 같이하는, 편승하는, 하는김에 같이하는느낌
	- With G1, it is piggybacked on a normal young GC.

- command line switches
	- 커맨드라인 스위치, cmd 옵션들
	- -Xmx4g, -XX:MaxGCPauseMillis 와 같은 옵션들을 switches라고 부른다.




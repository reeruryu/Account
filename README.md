## wifi
내 위치 기반 공공 와이파이 정보를 제공하는 웹 서비스



## 활용 기술

- JSP (JDK1.8)
- Maven
- MariaDB
- Lombok
- okhttp3
- gson



## 구현

#### Open API 와이파이 정보 가져오기
1. Open API 와이파이 정보 가져오기 시, load-wifi.jsp를 통해 ApiExplorer 클래스의 loadWifi 함수 호출
2. Open API에서 약 14985개의 WIFI 정보 load


#### 내 위치 가져오기를 한 후에 근처 WIFI 정보 보기
1. 내 위치 가져오기(Javascript)를 통해, 현재 나의 LAT과 LNT를 얻기
2. 근처 WIFI 정보 보기 시, WifiDAO 클래스의 selectWifi 함수 호출 + MyWifiDAO 클래스의 insertMyWifi 함수 호출
3. 내 위치 정보에서 가까운 위치에 있는 와이파이 정보 20개 보여주기 + 위치 히스토리 저장
4. 

#### 위치 히스토리 목록
1. 위치 히스토리 목록 보기 시, MyWifiDAO 클래스의 selectMyWifi 함수 호출
2. DB에 저장된 위치 히스토리 목록 load
3. 위치 히스토리 삭제 시, delete-history.jsp를 통해 MyWifiDAO 클래스의 deleteMyWifi 함수 호출
4. DB에 지정한 위치 히스토리 삭제

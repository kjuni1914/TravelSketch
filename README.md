# TravelSketch  

TravelSketch는 지도 기반 다이어리 기록 및 공유 애플리케이션입니다.  
여행 중 촬영한 사진, 동영상, 영수증 등의 데이터를 지도 위의 캔버스에 저장할 수 있으며, 각 미디어 파일의 GPS 메타데이터를 분석하여 캔버스의 위치를 자동으로 업데이트합니다.  
이를 통해 사용자는 여행 기록을 지도 위에 저장하여, 보다 생생하게 추억을 되새길 수 있습니다.

## 주요 기능  

### 지도 기반 캔버스  
- Google Maps와 연동하여 여행 기록을 **직관적**으로 저장
- 캔버스에 추가된 미디어 파일의 GPS 메타데이터를 분석하여 **지도상의 캔버스 위치를 자동 조정**  

### 캔버스 편집 기능  
- 사진, 동영상, 텍스트 등의 **다양한 미디어 파일을 캔버스에 추가**
- 저장된 미디어 파일은 학습된 TFLite 모델의 분류로 **필터링 가능**  

### 파일 업로드 및 관리  
- Firebase Storage를 사용하여 **이미지 및 동영상 저장**
- 전체 보기, 미디어만 보기, 영수증만 보기 등 **모아보기 기능**

### 사용자 인증  
- Firebase Authentication을 이용한 **이메일/비밀번호 로그인 및 회원가입**  
- Google 계정 로그인 지원  
- 비밀번호 재설정 및 ID 찾기 기능 제공  

### 영수증 자동 분류  
- 학습된 TensorFlow Lite 기반(From. Teachable Machine) **영수증 이미지 자동 인식**  
- 영수증과 일반 이미지를 자동으로 판별하여 **분류 및 정리**
- GPT 연동을 통해 환율 계산 결과를 JSON 파일 형식으로 받아 사용자에게 표시하는 기능 _(API 키 문제로 삭제됨)_

## 기술 스택  

### Backend  
- Firebase Authentication을 통한 로그인 및 사용자 관리  
- Firebase Realtime Database를 활용한 데이터 저장 및 실시간 동기화  
- Firebase Storage를 사용한 미디어 파일 관리  
- TensorFlow Lite 기반의 영수증 자동 분류 모델 적용  

### Frontend  
- Kotlin 및 Jetpack Compose를 사용한 UI 구성  
- ViewModel 및 StateFlow를 활용한 UI 상태 관리
- Google Maps API 연동을 통한 위치 기반 캔버스 표시

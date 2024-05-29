#include <Wire.h>
#include <LiquidCrystal_I2C.h> // LCD 라이브러리
#include <TM1637Display.h> // TM1637 FND 라이브러리
#include <SoftwareSerial.h> // 블루투스 모듈 라이브러리
#include <DHT.h> // DHT22 센서 라이브러리
#include <DHT_U.h>

#define DHTPIN 2     // DHT22 센서의 DATA 핀을 아두이노의 디지털 핀 2에 연결
#define DHTTYPE DHT22   // DHT22 센서 사용
// CLK와 DIO 핀 설정
#define DIO 6
#define CLK 7
#define BT_RX 8
#define BT_TX 9
#define DUST_SENSOR_PIN A0 // GP2Y1014AU 센서의 아날로그 출력 핀

const int battery = A1; // 배터리 전압을 읽을 아날로그 핀
const float batteryT = 10; // 배터리 충전량 임계값

const int PTCheater = 4; // PTC 히터
const int HUM = 5; // 가습기 모듈
const int FAN1 = 10;
const int FAN2 = 11;
const int GREEN = 12; // 초록색 LED 핀
const int RED = 13; // 빨간색 LED 핀
DHT dht(DHTPIN, DHTTYPE);

// HC-06 모듈 TX : 8 RX : 9
SoftwareSerial hc06(BT_RX, BT_TX);

LiquidCrystal_I2C lcd(0x27,16,2); // I2C 주소, LCD 
// TM1637Display 객체 생성
TM1637Display display(CLK, DIO);

// 온도, 습도, 미세먼지 임계점
int coldTemp = 18;
int hotTemp = 26;
int lowHum = 40;
int dustSet = 10;

// 0:Off 1:On
int fanOn = 1;
int heaterOn = 1;
int humOn = 1;
int airOn = 1;

// 0:수동 1:자동
int autoFan = 1;
int autoHeater = 1;
int autoHum = 1;
int autoAir = 1;

long HUMofftime = 0; // 가습기 종료 시간
const long HUMDelay = 30000; // 30초 

void setup() {
  Serial.begin(9600); // 시리얼 통신 시작
  hc06.begin(9600); // HC-06 모듈 통신 시작

  dht.begin();        // DHT22 센서 초기화

  lcd.init();  // LCD초기 설정             
  lcd.backlight(); // LCD초기 설정  
  lcd.setCursor(0, 0);  // LCD 커서 첫 번째 줄
  lcd.print("Temp: ");  // 온도 표시
  lcd.setCursor(0, 1);  // LCD 커서 두 번째 줄
  lcd.print("Humidity: "); // 습도 표시

  display.setBrightness(0x0a); // FND 디스플레이 밝기 설정 (0x00 ~ 0x0f)
  pinMode(PTCheater, OUTPUT);
  pinMode(FAN1, OUTPUT);
  pinMode(FAN2, OUTPUT);
  pinMode(GREEN, OUTPUT);
  pinMode(RED, OUTPUT);
}

void loop() {
  delay(2000); // 2초 대기
  int dustValue = analogRead(DUST_SENSOR_PIN); // 아날로그 핀에서 미세먼지 값 읽기
  int humidity = dht.readHumidity(); // 습도
  float temperature = dht.readTemperature(); // 온도 (C)
  // 배터리 전압 읽기
  int batteryValue = analogRead(battery);
  int voltage = batteryValue * (5.0 / 1023.0); // 아날로그 값에서 전압으로 변환
  int batteryPercent = (voltage / 5.0) * 100; // 전압 값을 퍼센트로 변환

  // DHT22 센서로부터 데이터를 읽어오지 못한 경우
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("DHT22 센서에서 값을 읽지 못했습니다.");
    return;
  }
  Serial.print("미세먼지 농도: ");
  Serial.println(dustValue); // 미세먼지 값 시리얼 모니터에 출력

  // 블루투스로 Android에 데이터 전송
  String dataAndroid = String(temperature, 1) + "," + String(humidity) + "," + String(dustValue) + "," + String(batteryPercent);
  hc06.println(dataAndroid);

  // 블루투스 데이터 수신 및 처리
  if (hc06.available()) {
    String data = hc06.readStringUntil('\n');
    int firstCommaIndex = data.indexOf(',');
    int secondCommaIndex = data.indexOf(',', firstCommaIndex + 1);
    int thirdCommaIndex = data.indexOf(',', secondCommaIndex + 1);
    int fourthCommaIndex = data.indexOf(',', thirdCommaIndex + 1);
    int fifthCommaIndex = data.indexOf(',', fourthCommaIndex + 1);
    int sixthCommaIndex = data.indexOf(',', fifthCommaIndex + 1);
    int seventhCommaIndex = data.indexOf(',', sixthCommaIndex + 1);
    int eighthCommaIndex = data.indexOf(',', seventhCommaIndex + 1);
    int ninthCommaIndex = data.indexOf(',', eighthCommaIndex + 1);
    int tenthCommaIndex = data.indexOf(',', ninthCommaIndex + 1);
    int eleventhCommaIndex = data.indexOf(',', tenthCommaIndex + 1);

    if (firstCommaIndex != -1 && secondCommaIndex != -1 && thirdCommaIndex != -1 &&
        fourthCommaIndex != -1 && fifthCommaIndex != -1 && sixthCommaIndex != -1 &&
        seventhCommaIndex != -1 && eighthCommaIndex != -1 && ninthCommaIndex != -1 &&
        tenthCommaIndex != -1 && eleventhCommaIndex != -1) {

        String coldTempStr = data.substring(0, firstCommaIndex);
        String hotTempStr = data.substring(firstCommaIndex + 1, secondCommaIndex);
        String lowHumStr = data.substring(secondCommaIndex + 1, thirdCommaIndex);
        String dustSetStr = data.substring(thirdCommaIndex + 1, fourthCommaIndex);
        String fanOnStr = data.substring(fourthCommaIndex + 1, fifthCommaIndex);
        String humOnStr = data.substring(fifthCommaIndex + 1, sixthCommaIndex);
        String heaterOnStr = data.substring(sixthCommaIndex + 1, seventhCommaIndex);
        String airOnStr = data.substring(seventhCommaIndex + 1, eighthCommaIndex);
        String autoFanStr = data.substring(eighthCommaIndex + 1, ninthCommaIndex);
        String autoHumStr = data.substring(ninthCommaIndex + 1, tenthCommaIndex);
        String autoHeaterStr = data.substring(tenthCommaIndex + 1, eleventhCommaIndex);
        String autoAirStr = data.substring(eleventhCommaIndex + 1);

        coldTemp = coldTempStr.toInt();
        hotTemp = hotTempStr.toInt();
        lowHum = lowHumStr.toInt();
        dustSet = dustSetStr.toInt();
        fanOn = fanOnStr.toInt();
        humOn = humOnStr.toInt();
        heaterOn = heaterOnStr.toInt();
        airOn = airOnStr.toInt();
        autoFan = autoFanStr.toInt();
        autoHum = autoHumStr.toInt();
        autoHeater = autoHeaterStr.toInt();
        autoAir = autoAirStr.toInt();

        Serial.print("온풍기 작동 온도: ");
        Serial.println(coldTemp);
        Serial.print("선풍기 작동 온도: ");
        Serial.println(hotTemp);
        Serial.print("가습기 작동 습도: ");
        Serial.println(lowHum);
        Serial.print("공기청정기 작동 미세먼지 농도 : ");
        Serial.println(dustSet);
        Serial.print("선풍기 : ");
        Serial.println(fanOn);
        Serial.print("가습기 : ");
        Serial.println(humOn);
        Serial.print("온풍기 : ");
        Serial.println(heaterOn);
        Serial.print("공기청정기 : ");
        Serial.println(airOn);
        Serial.print("자동 선풍기 : ");
        Serial.println(autoFan);
        Serial.print("자동 가습기 : ");
        Serial.println(autoHum);
        Serial.print("자동 온풍기 : ");
        Serial.println(autoHeater);
        Serial.print("자동 공기청정기 : ");
        Serial.println(autoAir);
    }
  }

  /* if (hc06.available()) { // HC-06 -> 시리얼 모니터 통신 확인
    char HC06ok = hc06.read();
    Serial.print("HC-06: ");
    Serial.println(HC06ok);
  }

  if (Serial.available()) { // HC-06 <- 시리얼 모니터 통신 확인
    char Arduinook = Serial.read();
    hc06.print("Arduino: ");
    hc06.println(Arduinook);
  } */

  // 선풍기 및 온풍기 작동
  if ((temperature <= coldTemp && autoHeater == 1 && heaterOn == 1) || (autoHeater == 0 && heaterOn == 1)) {
    digitalWrite(PTCheater, HIGH);
    digitalWrite(FAN1, LOW);
  } else if ((temperature >= hotTemp && fanOn == 1) || (autoFan == 0 && fanOn == 1)) {
    digitalWrite(PTCheater, LOW);
    digitalWrite(FAN1, HIGH);
  } else {
    digitalWrite(PTCheater, LOW);
    digitalWrite(FAN1, LOW);
  }

  // 습도에 따라 가습기 작동
  if ((humidity < lowHum && humOn == 1) || (autoHum == 0 && humOn == 1)) {
    digitalWrite(HUM, HIGH);
    HUMofftime = 0;
  } else {
    if (HUMofftime == 0) {
      HUMofftime = millis() + HUMDelay; // 타이머 설정
    } else if (millis() > HUMofftime) {
      digitalWrite(HUM, LOW);
      HUMofftime = 0; // 타이머 초기화
    }
  }

  // 설정된 미세먼지에 따라 공기청정기 작동
  if ((dustValue >= dustSet && airOn == 1) || (autoAir == 0 && airOn == 1)) {
    digitalWrite(FAN2, HIGH);
  } else {
    digitalWrite(FAN2, LOW);
  }


  // 배터리 충전량 값에 따라 LED 색상 변경
  if (batteryPercent >= batteryT) {
    digitalWrite(GREEN, HIGH); // 초록색 LED 켜기
    digitalWrite(RED, LOW); // 빨간색 LED 끄기
  } else {
    digitalWrite(GREEN, LOW); // 초록색 LED 끄기
    digitalWrite(RED, HIGH); // 빨간색 LED 켜기
  }

  // 온습도 데이터 출력
  Serial.print("습도: ");
  Serial.print(humidity);
  Serial.print("%, ");
  Serial.print("온도: ");
  Serial.print(temperature, 1);
  Serial.println("°C");
  Serial.print("배터리 : ");
  Serial.print(batteryPercent);
  Serial.println("%");

  // 온습도 값을 LCD에 표시
  lcd.setCursor(6, 0);
  lcd.print(temperature); // 온도 값 출력
  lcd.setCursor(10, 0);
  lcd.print("C"); // 온도 단위 출력
  lcd.setCursor(10, 1);
  lcd.print(humidity); // 습도 값 출력
  lcd.print("%"); // 습도 단위 출력

  // 표시할 숫자 배열 선언
  uint8_t data[] = {0x0, 0x0, 0x0, 0x0};

  // 디스플레이에 10진수 숫자 표시 함
  display.showNumberDec(dustValue);
}
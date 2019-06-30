#include <SoftwareSerial.h>
#include <AFMotor.h>
AF_DCMotor motor_L(4);              // 모터드라이버 L293D  3: M3에 연결,  4: M4에 연결
AF_DCMotor motor_R(3); 

#define BT_RXD A5
#define BT_TXD A4
SoftwareSerial bluetooth(BT_RXD, BT_TXD);       // RX: A5, TX: A4

char rec_data;
bool rec_chk = false;

int i;
int j;

//초음파센서 출력핀(trig)과 입력핀(echo), 변수, 함수 선언//
int TrigPin = A0;
int EchoPin = A1;
long duration, distance;
void Distance_Measurement();

void setup(){
  Serial.begin(9600);              // PC와의 시리얼 통신속도
  bluetooth.begin(9600);            // 스마트폰 블루투스 통신속도
  Serial.println("Eduino Smart Car Start!");
  pinMode(EchoPin, INPUT);   // EchoPin 입력
  pinMode(TrigPin, OUTPUT);  // TrigPin 출력

  // turn on motor
  motor_L.setSpeed(250);              // 왼쪽 모터의 속도   
  motor_L.run(RELEASE);
  motor_R.setSpeed(250);              // 오른쪽 모터의 속도   
  motor_R.run(RELEASE);
}
void loop(){
  
  if(bluetooth.available()){         // 블루투스 명령 수신
     rec_data = bluetooth.read();
     Serial.write(rec_data);
     rec_chk = true;
  }  
  Distance_Measurement();
  if(rec_data == 'g'){  // 전진, go
    Serial.println("g");
     motor_L.run(FORWARD);  motor_R.run(FORWARD);        
  } 
  else if(rec_data == 'b'){ // 후진, back
    Serial.println("b");
     motor_L.run(BACKWARD);  motor_R.run(BACKWARD);    
  }
  else if(rec_data == 'l'){ // 좌회전, Go Left
    Serial.println("l");
   motor_L.run(RELEASE);  motor_R.run(FORWARD);     
  }
  else if(rec_data == 'r'){ // 우회전, Go Right
    Serial.println("r");
    motor_L.run(FORWARD);  motor_R.run(RELEASE);                
  }
  else if(rec_data == 'w'){ // 제자리 회전, Right Rotation
    Serial.println("w");
     motor_L.run(BACKWARD);   motor_R.run(FORWARD);      
  }
  else if(rec_data == 'q'){ // 제자리 회전, Left Rotation
      motor_L.run(FORWARD);   motor_R.run(BACKWARD);    
  }
  else if(rec_data == 's'){  } // Stop 

  if(rec_data == 's' ){       // 정지
    if(rec_chk == true){
       for (i=250; i>=0; i=i-20) {
          motor_L.setSpeed(i);  motor_R.setSpeed(i);  
          delay(10);
       }  
       motor_L.run(RELEASE);       motor_R.run(RELEASE);
    }
  }
  else{
    if(rec_chk == true){
      if(rec_data == 'l'){  // Left
        for (i=20; i<200; i=i+10) {
          motor_L.setSpeed(i);  motor_R.setSpeed(i);
          delay(30);
        }
       }
       else if(rec_data == 'r'){       // Right
        for (i=20; i<200; i=i+10) {
          motor_L.setSpeed(i);  motor_R.setSpeed(i);
          delay(30);
        }
       }
       else if(rec_data == 'w' || rec_data == 'q'){ // Rotation Left, Right
        for (i=0; i<250; i=i+20) {
          motor_L.setSpeed(i);  motor_R.setSpeed(i);  
          delay(20);
        }
       }
       else if(rec_data == 'g'){ //Go
        for (i=0; i<250; i=i+20) { 
           motor_L.setSpeed(i);  motor_R.setSpeed(i);  
          delay(10);   
        }
       }
       else{
        for (i=0; i<250; i=i+20) { //  Back
           motor_L.setSpeed(i);  motor_R.setSpeed(i);  
          delay(10);                         
        }
       }
     }
    else{     
          motor_L.setSpeed(250);  motor_R.setSpeed(250);  
    }
  }
  rec_chk = false;
}

////////거리감지///////////
void Distance_Measurement(){
  digitalWrite(TrigPin, LOW);
  delay(2);
  digitalWrite(TrigPin, HIGH);  // trigPin에서 초음파 발생(echoPin도 HIGH)
  delayMicroseconds(10);
  digitalWrite(TrigPin, LOW);
  duration = pulseIn(EchoPin, HIGH);    // echoPin 이 HIGH를 유지한 시간을 저장 한다.
  Serial.println(distance);
  //Serial.println(duration);
  distance = ((float)(340 * duration) / 1000) / 2;
  
  if(distance < 150)
  {
    bluetooth.write(444);
    motor_L.run(RELEASE);
    motor_R.run(RELEASE);
    for (i=200; i>=0; i=i-20) {
          motor_L.setSpeed(i);  motor_R.setSpeed(i);  
          delay(2);
          }
    
    delay(5);
    // stop car

  }
}

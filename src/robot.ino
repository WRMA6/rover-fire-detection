#include < SoftwareSerial.h >
#include < SparkFun_TB6612.h >

// Pins for all inputs, keep in mind the PWM defines must be on PWM pins
#define AIN1 2
#define BIN1 7
#define AIN2 4
#define BIN2 8
#define PWMA 5
#define PWMB 6
#define STBY 9

// these constants are used to allow you to make your motor configuration 
// line up with function names like forward.  Value can be 1 or -1
const int offsetA = -1;
const int offsetB = 1;
Motor motor1(AIN1, AIN2, PWMA, offsetA, STBY);
Motor motor2(BIN1, BIN2, PWMB, offsetB, STBY);

int state = 0;

void setup() {
    // put your setup code here, to run once:
    Serial.begin(9600);
}

void loop() {
    if (Serial.available() > 0) {
        state = Serial.read();
        if (state == 'S') {
            motor1.brake();
            motor2.brake();
        } else if (state == 'F') {
            motor1.drive(255);
            motor2.drive(255);
        } else if (state == 'B') {
            motor1.drive(-255);
            motor2.drive(-255);
        } else if (state == 'L') {
            motor1.drive(255);
            motor2.drive(-255);
        } else if (state == 'R') {
            motor2.drive(255);
            motor1.drive(-255);
        }
    }
}
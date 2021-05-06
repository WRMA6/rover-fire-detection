import processing.serial.*;

Serial myPort;

boolean connected = true;
boolean still = false;

float dx = 1.75;
float da = 1.63;

float x = 0;
float y = 0;
float angle = 90;

float xo = 0;
float yo = 0;
float angleo = 90;

int mode = 0;

String input;

class lineData {
    float x1;
    float y1;
    float x2;
    float y2;
    float angle;

    lineData(float _x1, float _y1, float _x2, float _y2) {
        x1 = _x1;
        y1 = _y1;
        x2 = _x2;
        y2 = _y2;
        setAngle();
    }

    void setAngle() {
        if (x1 == x2) {
            if (y2 <= y1) angle = 90;
            else angle = 270;
        } else angle = degrees(atan((y1 - y2) / (x2 - x1)));
        if (x2 < x1) angle += 180;
    }
};

ArrayList < lineData > map = new ArrayList < lineData > ();
boolean mapping = false;

boolean play = false;
boolean pause = false;
int index = 0;
boolean start = true;

float factor1 = 1.15;
float factor2 = 1.5;
float factorx = 10;
float factora = 15;
int startTime = 0;
int currentTime = 0;
float closestAngle = 0;
boolean angleSet = false;
float cmpAngle = 0;
float angleChange = 0;

boolean looping = false;
boolean pausable = false;

void setup() {
    size(1080, 720);
    if (connected) {
        myPort = new Serial(this, "COM6", 9600); // Starts the serial communication
        myPort.bufferUntil('\n'); // Defines up to which character the data from the serial port will be read. The character '\n' or 'New Line'
    }
}

void draw() {
    background(255);
    stroke(100);
    strokeWeight(5);
    lineData line;
    for (int i = 0; i < map.size(); i++) {
        line = map.get(i);
        line(line.x1, line.y1, line.x2, line.y2);
    }
    fill(0);
    stroke(0);
    strokeWeight(0);
    if (still) {
        x = 0;
        y = 0;
        angle = 90;
    }
    ellipse(width / 2 + x, height / 2 - y, 20, 20);
    strokeWeight(10);
    line(width / 2 + x, height / 2 - y, width / 2 + x + 15 * cos(radians(angle)), height / 2 - y - 15 * sin(radians(angle)));

    if (mode > 0) {
        currentTime = millis();
        if (mode == 1) {
            x = xo + (currentTime - startTime) * dx * cos(radians(angle)) / factorx;
            y = yo + (currentTime - startTime) * dx * sin(radians(angle)) / factorx;
        } else if (mode == 2) {
            x = xo - (currentTime - startTime) * dx * cos(radians(angle)) / factorx;
            y = yo - (currentTime - startTime) * dx * sin(radians(angle)) / factorx;
        } else if (mode == 3) {
            angle = angleo + (currentTime - startTime) * da / factora;
            if (angle >= 360) angle -= 360;
        } else {
            angle = angleo - (currentTime - startTime) * da / factora;
            if (angle < 0) angle += 360;
        }
    }

    if (play && map.size() > 0) {
        currentTime = millis();
        if ((pause && currentTime - startTime > 5000) || (!pause && currentTime - startTime > 2000)) {
            startTime = currentTime;
            if (pausable) pause = !pause;
            else pause = false;
            if (pause) myPort.write('S');
        }
        if (index < map.size()) {
            line = map.get(index);
            if (!angleSet) {
                float angleArr[] = {
                    abs(angle - line.angle + 360),
                    abs(angle - line.angle),
                    abs(angle - line.angle - 360)
                };
                closestAngle = line.angle - 360;
                for (int i = 1; i < 3; ++i) {
                    if (angleArr[i] < abs(angle - closestAngle)) {
                        if (i == 1) closestAngle = line.angle;
                        else closestAngle = line.angle + 360;
                    }
                }
                angleSet = true;
            }
            if (start) {
                x = line.x1 - width / 2;
                y = height / 2 - line.y1;
                start = false;
            }
            if (abs(angle - closestAngle) > factor1 * da / 50) {
                if (abs(angle - closestAngle) > factor1 * (1.01) * da / 2 && !pause) {
                    if (angle > closestAngle) {
                        angle -= factor1 * da * 0.88;
                        angleChange -= factor1 * da * 0.88;
                        if (abs(angleChange) >= 95) {
                            angleChange = 0;
                            if (connected) myPort.write('S');
                            delay(100);
                        }
                        if (connected) myPort.write('R');
                    } else {
                        angle += factor1 * da;
                        angleChange += factor1 * da;
                        if (abs(angleChange) >= 95) {
                            angleChange = 0;
                            if (connected) myPort.write('S');
                            delay(100);
                        }
                        if (connected) myPort.write('L');
                    }
                } else if (abs(angle - closestAngle) < factor1 * (1.01) * da / 2) {
                    if (angle > closestAngle) {
                        angle -= factor1 * da / 50;
                        if (connected) {
                            myPort.write('r');
                            myPort.write('S');
                        }
                    } else {
                        angle += factor1 * da / 50;
                        if (connected) {
                            myPort.write('l');
                            myPort.write('S');
                        }
                    }
                }
            } else if (pow((line.x2 - width / 2 - x), 2) + pow((height / 2 - line.y2 - y), 2) > factor2 * 5 * dx) {
                cmpAngle = angle % 360;
                if (cmpAngle < 0) cmpAngle += 360;
                if (pow((line.x2 - width / 2 - x), 2) + pow((height / 2 - line.y2 - y), 2) > factor2 * 50 * dx && !pause) {
                    if ((y - height / 2 + line.y2) * (cmpAngle - 180) > 0 || (line.x2 - x - width / 2) * (abs(cmpAngle - 180) - 90) > 0) {
                        x += factor2 * dx * cos(radians(cmpAngle));
                        y += factor2 * dx * sin(radians(cmpAngle));
                        if (connected) myPort.write('F');
                    } else {
                        x -= factor2 * dx * cos(radians(cmpAngle));
                        y -= factor2 * dx * sin(radians(cmpAngle));
                        if (connected) myPort.write('B');
                    }
                } else if (pow((line.x2 - width / 2 - x), 2) + pow((height / 2 - line.y2 - y), 2) < factor2 * 50 * dx) {
                    if ((y - height / 2 + line.y2) * (cmpAngle - 180) > 0 || (line.x2 - x - width / 2) * (abs(cmpAngle - 180) - 90) > 0) {
                        x += factor2 * dx * cos(radians(cmpAngle)) / 2;
                        y += factor2 * dx * sin(radians(cmpAngle)) / 2;
                        if (connected) {
                            myPort.write('f');
                            myPort.write('S');
                        }
                    } else {
                        x -= factor2 * dx * cos(radians(cmpAngle)) / 2;
                        y -= factor2 * dx * sin(radians(cmpAngle)) / 2;
                        if (connected) {
                            myPort.write('b');
                            myPort.write('S');
                        }
                    }
                }
            } else {
                angle = angle % 360;
                if (angle < 0) angle += 360;
                index++;
                start = true;
                angleSet = false;
                angleChange = 0;
                if (index >= map.size()) {
                    if (!looping) play = false;
                    else index = 0;
                }
            }
        }
    }
}

void mouseMoved() {
    if (mapping) {
        map.get(map.size() - 1).x2 = mouseX;
        map.get(map.size() - 1).y2 = mouseY;
        map.get(map.size() - 1).setAngle();
        draw();
    }
}

void mousePressed() {
    if (mapping) {
        float px1 = map.get(map.size() - 1).x2;
        float py1 = map.get(map.size() - 1).y2;
        map.get(map.size() - 1).setAngle();
        map.add(new lineData(px1, py1, mouseX, mouseY));
        draw();
    }
}

void keyPressed() {
    if (key == ENTER) {
        if (!mapping) {
            float px1 = width / 2 + x;
            float py1 = height / 2 - y;
            if (map.size() > 0) {
                px1 = map.get(map.size() - 1).x2;
                py1 = map.get(map.size() - 1).y2;
            }
            map.add(new lineData(px1, py1, mouseX, mouseY));
            draw();
        } else if (looping) {
            lineData linef = map.get(map.size() - 1);
            lineData lines = map.get(0);
            map.add(new lineData(linef.x2, linef.y2, lines.x1, lines.y1));
        }
        mapping = !mapping;
    } else if (key == DELETE) {
        mapping = false;
        int l = map.size();
        for (int i = 0; i < l; ++i) {
            map.remove(0);
        }
        index = 0;
    } else if (key == 'p') {
        xo = x;
        yo = y;
        angleo = angle;
        startTime = millis();
        index = 0;

        play = !play;
        angleChange = 0;
    } else if (key == 't') {
        x = mouseX - width / 2;
        y = height / 2 - mouseY;
        angle = 90;
        mode = 0;
        index = 0;
        draw();
    } else if (key == 'r') {
        x = 0;
        y = 0;
        angle = 90;
        mode = 0;
        index = 0;
        draw();
    } else if (key == CODED && !play) {
        if (keyCode == UP) {
            xo = x;
            yo = y;
            startTime = millis();
            mode = 1;
            if (connected) myPort.write('F');
        } else if (keyCode == DOWN) {
            xo = x;
            yo = y;
            startTime = millis();
            mode = 2;
            if (connected) myPort.write('B');
        } else if (keyCode == LEFT) {
            angleo = angle;
            startTime = millis();
            mode = 3;
            if (connected) myPort.write('L');
        } else if (keyCode == RIGHT) {
            angleo = angle;
            startTime = millis();
            mode = 4;
            if (connected) myPort.write('R');
        }
    }
}

void keyReleased() {
    if (key == CODED) {
        mode = 0;
        if (connected) myPort.write('S');
    }
}

void stop() {
    if (connected) myPort.write('S');
}
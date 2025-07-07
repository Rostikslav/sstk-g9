#include <ESP8266WebServer.h>
#include <ESP8266WiFi.h>
#include <WiFiManager.h>
#include <WiFiUdp.h>
#include <EEPROM.h>
#include <Ticker.h>
#include <Wire.h>
#include <Adafruit_BMP085.h>

#include "token.h"
#include "udp.h"

#define SWITCH_PIN D5  // main control pin
#define RESET_PIN D0   // to GND on boot for reset

// indicators
#define RED D7
#define GREEN D8
#define BLUE D6

// UDP
#define BROADCAST_INTERVAL 1000    // 1sec
#define BROADCAST_DURATION 120000  // 2min

// temp sensor
#define TEMP_CUTOFF 50.0
#define TEMP_FAULTY_READING_THRESHOLD 100.0
#define TEMP_READ_INTERVAL 2000  // 2sec
#define COOLDOWN_DURATION 60000  // 1min

ESP8266WebServer server(80);
Ticker blinker;

unsigned long broadcastStart = 0;
unsigned long lastBroadcast = 0;
bool enableBroadcast = false;

unsigned long timerEnd = 0;
int timerAction = 0;

Adafruit_BMP085 bmp;
unsigned long temperatureLastRead = 0;
unsigned long cooldownEnd = 0;
bool cooldownMode = false;
// ------ LED ------

void toggle(uint8_t pin) {
    digitalWrite(pin, !digitalRead(pin));
}

void updateLedState() {
    if (digitalRead(SWITCH_PIN)) {
        digitalWrite(RED, LOW);
        digitalWrite(GREEN, HIGH);
        digitalWrite(BLUE, LOW);
    } else {
        digitalWrite(RED, HIGH);
        digitalWrite(GREEN, LOW);
        digitalWrite(BLUE, LOW);
    }
}

// ------ HANDLERS ------

// validates token
bool auth() {
    if (!server.hasArg("token") || server.arg("token") != String(savedToken)) {
        server.send(403, "text/plain", "Forbidden");
        return false;
    }
    return true;
}

// check the sensor availability
bool checkSensor() {
    if (!bmp.begin()) {
        server.send(500, "text/plain", "Temperature sensor is not detected");
        return false;
    }
    return true;
}

// check whether the server is running
void handlePing() {
    server.send(200, "text/plain", "pong");
}

// initial pairing with token generation
void handleSetup() {
    if (tokenExists()) {
        server.send(403, "text/plain", "Already configured");
        return;
    }

    String token = generateToken(TOKEN_LENGTH);
    saveToken(token);
    enableBroadcast = false;
    updateLedState();
    server.send(200, "text/plain", token);
}

// getter fot the switch state(requires auth)
void handleState() {
    if (!auth())
        return;

    int remainingSeconds = timerEnd ? (timerEnd - millis()) / 1000 : 0;

    String json = "{";
    json += "\"sensorError\":" + String(!bmp.begin() ? "true" : "false") + ",";
    json += "\"state\":" + String(digitalRead(SWITCH_PIN) ? "true" : "false") + ",";
    json += "\"timerEnd\":" + String(remainingSeconds) + ",";
    json += "\"cooldownMode\":" + String(cooldownMode ? "true" : "false");
    json += "}";

    server.send(200, "application/json", json);
}

// setter for the switch state(requires auth)
void handleToggleState() {
    if (!auth() || !checkSensor())
        return;

    if (!server.hasArg("state")) {
        server.send(400, "text/plain", "Missing 'state' parameter");
        return;
    }

    String state = server.arg("state");
    if (state == "1") {
        if (cooldownMode) {
            server.send(409, "text/plain", "Device is in cooldown mode");
            return;
        }
        digitalWrite(SWITCH_PIN, HIGH);
        server.send(200, "text/plain", "ON");
    } else if (state == "0") {
        digitalWrite(SWITCH_PIN, LOW);
        server.send(200, "text/plain", "OFF");
    } else {
        server.send(400, "text/plain", "Invalid state. Use 1 or 0.");
    }
    updateLedState();
}

// set/reset timer
void handleTimer() {
    if (!auth() || !checkSensor())
        return;

    if (cooldownMode) {
        server.send(409, "text/plain", "Device is in cooldown mode");
        return;
    }

    if (!server.hasArg("seconds")) {
        server.send(400, "text/plain", "Missing 'seconds' parameter");
        return;
    }

    String rawSeconds = server.arg("seconds");
    int seconds = rawSeconds.toInt();
    bool isValid = rawSeconds.length() > 0 && seconds != 0;

    if (!isValid && rawSeconds != "0") {
        server.send(400, "text/plain", "Invalid parameter 'seconds'. The Value should be an integer.");
        return;
    }

    if (seconds == 0) {
        timerEnd = 0;
        server.send(200, "text/plain", "Timer deactivated.");
        return;
    }

    if (!server.hasArg("action")) {
        server.send(400, "text/plain", "Missing 'action' parameter");
        return;
    }

    String rawAction = server.arg("action");
    if (rawAction != "1" && rawAction != "0") {
        server.send(400, "text/plain", "Invalid parameter 'action'. Use 1 or 0.");
        return;
    }

    timerEnd = millis() + (unsigned long)seconds * 1000;
    timerAction = rawAction.toInt();
    server.send(200, "text/plain", "Timer set.");
}

// getter fot the temperature readings(requires auth)
void handleTemperature() {
    if (!auth() || !checkSensor())
        return;

    float temp = bmp.readTemperature();
    if (isnan(temp) || temp > TEMP_FAULTY_READING_THRESHOLD) {
        server.send(500, "text/plain", "Faulty sensor readings");
    } else {
        server.send(200, "text/plain", String(temp, 1));
    }
}

// forget connected device and reboot
void handleDisconnect() {
    if (!auth())
        return;

    clearToken();
    server.send(200, "text/plain", "Successfully disconnected");
    delay(1000);
    ESP.restart();
}

void setup() {
    pinMode(SWITCH_PIN, OUTPUT);
    pinMode(RESET_PIN, INPUT);

    pinMode(RED, OUTPUT);
    pinMode(GREEN, OUTPUT);
    pinMode(BLUE, OUTPUT);

    digitalWrite(SWITCH_PIN, LOW);
    digitalWrite(RED, LOW);
    digitalWrite(GREEN, LOW);
    digitalWrite(BLUE, LOW);

    Serial.begin(115200);
    Serial.print('\n');

    while (!bmp.begin()) {
        toggle(RED);
        delay(300);
    }
    digitalWrite(RED, LOW);

    blinker.attach(0.3, []() {
        toggle(BLUE);
    });

    randomSeed(micros());
    EEPROM.begin(TOKEN_LENGTH);

    WiFiManager wm;
    if (digitalRead(RESET_PIN) == 0) {
        Serial.println("Resetting WiFi and token...");
        wm.resetSettings();
        clearToken();
        ESP.restart();
    } else {
        Serial.println("No reset");
    }

    if (!wm.autoConnect("EcoSwitch-Setup"))
        ESP.restart();

    loadToken();
    if (!tokenExists()) {  // only start broadcasting if no token saved
        enableBroadcast = true;
        broadcastStart = millis();
        digitalWrite(BLUE, HIGH);
    } else {
        updateLedState();
    }

    blinker.detach();
    udp.begin(UDP_PORT);

    server.on("/ping", handlePing);
    server.on("/setup", handleSetup);
    server.on("/status", handleState);
    server.on("/temperature", handleTemperature);
    server.on("/toggle", handleToggleState);
    server.on("/timer", handleTimer);
    server.on("/disconnect", handleDisconnect);
    server.begin();
}

void loop() {
    server.handleClient();
    if (!bmp.begin()) {
        digitalWrite(SWITCH_PIN, LOW);
        updateLedState();
        while (!bmp.begin()) {
            server.handleClient();
            toggle(RED);
            delay(300);
        }
        updateLedState();
    }

    if (enableBroadcast && millis() - broadcastStart < BROADCAST_DURATION && millis() - lastBroadcast > BROADCAST_INTERVAL) {
        sendIPBroadcast();
        lastBroadcast = millis();
    }

    if (timerEnd && timerEnd < millis()) {
        timerEnd = 0;
        digitalWrite(SWITCH_PIN, timerAction ? HIGH : LOW);
        updateLedState();
    }

    if (millis() - temperatureLastRead > TEMP_READ_INTERVAL) {
        temperatureLastRead = millis();
        if (!bmp.begin())
            return;
        float t = bmp.readTemperature();
        Serial.println(t);

        if (!isnan(t) && t >= TEMP_CUTOFF) {
            digitalWrite(SWITCH_PIN, LOW);
            updateLedState();
            cooldownMode = true;
            timerEnd = 0;
            cooldownEnd = millis() + COOLDOWN_DURATION;
        }
    }

    if (cooldownEnd && millis() > cooldownEnd) {
        cooldownEnd = 0;
        cooldownMode = false;
    }
}
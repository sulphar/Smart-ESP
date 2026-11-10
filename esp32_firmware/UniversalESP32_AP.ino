#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>

// Change these GPIOs to match your relay board and ESP32 variant.
// Verify boot-strapping pins and board pin availability before wiring.
const uint8_t RELAY_PINS[8] = {4, 5, 6, 7, 8, 9, 10, 11};
const bool RELAY_ACTIVE_LOW = true;
const char* AP_SSID = "ESP32_HOME";
const char* AP_PASSWORD = "ChangeMe123"; // 8+ characters

WebServer server(80);
bool channelState[8] = {false,false,false,false,false,false,false,false};

void applyRelay(uint8_t i, bool on) {
  channelState[i] = on;
  digitalWrite(RELAY_PINS[i], (on ^ RELAY_ACTIVE_LOW) ? HIGH : LOW);
}

void sendStatus() {
  JsonDocument doc;
  JsonArray arr = doc["switches"].to<JsonArray>();
  for (int i=0; i<8; i++) arr.add(channelState[i]);
  String out; serializeJson(doc, out);
  server.send(200, "application/json", out);
}

void setup() {
  Serial.begin(115200);
  for (int i=0; i<8; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    applyRelay(i, false);
  }
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  Serial.print("AP IP: "); Serial.println(WiFi.softAPIP());

  server.on("/api/status", HTTP_GET, sendStatus);
  for (int i=1; i<=8; i++) {
    String path = "/api/switch/" + String(i);
    server.on(path.c_str(), HTTP_POST, [i]() {
      JsonDocument doc;
      if (deserializeJson(doc, server.arg("plain"))) {
        server.send(400, "application/json", "{\"error\":\"invalid json\"}");
        return;
      }
      bool state = doc["state"] | false;
      applyRelay(i-1, state);
      server.send(200, "application/json", "{\"accepted\":true}");
    });
  }
  server.begin();
}

void loop() { server.handleClient(); }

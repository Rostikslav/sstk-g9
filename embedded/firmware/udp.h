#ifndef UDP_HELPER_H
#define UDP_HELPER_H

#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

extern WiFiUDP udp;
extern const unsigned int UDP_PORT;

IPAddress getBroadcastAddress(IPAddress ip, IPAddress subnet);
void sendIPBroadcast();

#endif

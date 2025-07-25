#include "udp.h"

WiFiUDP udp;
const unsigned int UDP_PORT = 4210;

// determine the udp broadcast ip address
IPAddress getBroadcastAddress(IPAddress ip, IPAddress subnet) {
  IPAddress broadcast;
  for (int i = 0; i < 4; i++) {
    broadcast[i] = ip[i] | ~subnet[i];
  }
  return broadcast;
}

// send a udp broadcast package with local ip address as data
void sendIPBroadcast() {
  IPAddress broadcastIp = getBroadcastAddress(WiFi.localIP(), WiFi.subnetMask());
  String message = "ecoswitch:" + WiFi.localIP().toString();
  udp.beginPacket(broadcastIp, UDP_PORT);
  udp.write(message.c_str());
  udp.endPacket();
}

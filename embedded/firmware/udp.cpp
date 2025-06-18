#include "udp.h"

WiFiUDP udp;
const unsigned int UDP_PORT = 4210;

IPAddress getBroadcastAddress(IPAddress ip, IPAddress subnet) {
  IPAddress broadcast;
  for (int i = 0; i < 4; i++) {
    broadcast[i] = ip[i] | ~subnet[i];
  }
  return broadcast;
}

void sendIPBroadcast() {
  IPAddress broadcastIp = getBroadcastAddress(WiFi.localIP(), WiFi.subnetMask());
  String message = "ecoswitch:" + WiFi.localIP().toString();
  udp.beginPacket(broadcastIp, UDP_PORT);
  udp.write(message.c_str());
  udp.endPacket();
}

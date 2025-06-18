#include <EEPROM.h>
#include "token.h"

char savedToken[TOKEN_LENGTH + 1];

String generateToken(int length) {
  const char charset[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  String token = "";
  for (int i = 0; i < length; i++) {
    token += charset[random(strlen(charset))];
  }
  return token;
}

void loadToken() {
  EEPROM.begin(TOKEN_LENGTH);
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    savedToken[i] = EEPROM.read(TOKEN_ADDR + i);
  }
  savedToken[TOKEN_LENGTH] = '\0';
  
}

void saveToken(const String& token) {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    EEPROM.write(TOKEN_ADDR + i, token[i]);
  }
  EEPROM.commit();
  token.toCharArray(savedToken, TOKEN_LENGTH + 1);
}

bool tokenExists() {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    if (EEPROM.read(TOKEN_ADDR + i) != 0xFF) return true;
  }
  return false;
}

void clearToken() {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    EEPROM.write(TOKEN_ADDR + i, 0xFF);
  }
  EEPROM.commit();
  savedToken[0] = '\0';
}
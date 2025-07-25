#include <EEPROM.h>
#include "token.h"

char savedToken[TOKEN_LENGTH + 1];

// random token generation of specified length
String generateToken(int length) {
  const char charset[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  String token = "";
  for (int i = 0; i < length; i++) {
    token += charset[random(strlen(charset))];
  }
  return token;
}


// load saved token from the memory
void loadToken() {
  EEPROM.begin(TOKEN_LENGTH);
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    savedToken[i] = EEPROM.read(TOKEN_ADDR + i);
  }
  savedToken[TOKEN_LENGTH] = '\0';
  
}

// write specified token into memory
void saveToken(const String& token) {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    EEPROM.write(TOKEN_ADDR + i, token[i]);
  }
  EEPROM.commit();
  token.toCharArray(savedToken, TOKEN_LENGTH + 1);
}

// check token existence
bool tokenExists() {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    if (EEPROM.read(TOKEN_ADDR + i) != 0xFF) return true;
  }
  return false;
}

// clear saved token
void clearToken() {
  for (int i = 0; i < TOKEN_LENGTH; i++) {
    EEPROM.write(TOKEN_ADDR + i, 0xFF);
  }
  EEPROM.commit();
  savedToken[0] = '\0';
}
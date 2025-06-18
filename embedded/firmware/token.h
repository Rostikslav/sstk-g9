#ifndef TOKEN_H
#define TOKEN_H

#include <Arduino.h>

#define TOKEN_ADDR 0
#define TOKEN_LENGTH 16

extern char savedToken[TOKEN_LENGTH + 1];

void loadToken();
void saveToken(const String& token);
void clearToken();
bool tokenExists();
String generateToken(int length);

#endif

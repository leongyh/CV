#include <SoftwareSerial.h>
#include <RTClib.h>
#include <Adafruit_GPS.h>
#include <Wire.h>
#include <String.h>
#include <SD.h>

#include <avr/sleep.h>

#define FLOWSENSORPIN 4
#define WAKEPIN 19
#define SDCSPIN 53
#define SDDIPIN 51
#define SDDOPIN 50
#define SDCLKPIN 52
#define GPRSPOWERPIN 9
#define GPSPOWERPIN 12

#define GPSECHO  true

//SYSTEM DATA VARIABLES
const String SYSTEM_ID = "cwbuild";
const String SECRET_KEY = "qTEVwf3Nmp3lcMpCPu6t";
const String METER_SIZE = "3/4\"";
const String METER_CONV = "7.5";
const String FLOWBIT_NUMBER = "+15104021337"; //Flowbit Twilio Number

//PERIMETER CONSTANTS AND SD FILE PATHS
const unsigned long FLOW_SESSION_INTERVAL = 5000;  //in millis
const unsigned long WAKE_SESSION_INTERVAL = 10000; //in millis
const unsigned int SMS_SEND_INTERVAL = 120; //in seconds
const unsigned int SMS_CHAR_LIMIT = 160;
const unsigned long SMS_SUCCESS_WAIT = 3000; //300000; //5 mins default
const unsigned int SMS_RETRY_TIMEOUT = 5;

const char* TIME_FILEPATH = "/sys/time.txt";
const char* DATA_SENT_SUCCESS_FILEPATH = "/sys/dtsent.txt";
const char* REGISTERED_FILEPATH = "/sys/reg.txt";

//GPS VARIABLES
uint32_t timer = millis();
boolean usingInterruptGPS = false;

//FLOWMETER VARIABLES
volatile uint32_t pulses = 0; // count how many pulses!
volatile uint8_t lastflowpinstate; // track the state of the pulse pin
volatile uint32_t lastflowratetimer = 0; // you can try to keep time of how long it is between pulses
volatile float flowrate;// and use that to calculate a flow rate

//MODULE OBJECTS
SoftwareSerial GPRS_Serial(7, 8); //GPRS for Arduino Uno
RTC_DS1307 RTC;
SoftwareSerial mySerial(3, 2);
Adafruit_GPS GPS(&mySerial);

//------------MAIN------------
void setup() {
  //Begin Serial
  Serial.begin(9600);
  
  //SD Card
  pinMode(SDCSPIN, OUTPUT);
  if (!SD.begin(SDCSPIN, SDDIPIN, SDDOPIN, SDCLKPIN)) {
    Serial.println("*initialization failed!");
  }
  Serial.println("initialization done.");
  delay(500);
    
  //FlowMeter and RTC
  Wire.begin();
  RTC.begin();
  pinMode(FLOWSENSORPIN, INPUT);
  digitalWrite(FLOWSENSORPIN, HIGH);
  lastflowpinstate = digitalRead(FLOWSENSORPIN);
  useInterruptFlow(true);
  
  //Arduino Wake/Sleep
  pinMode(WAKEPIN, INPUT);
  digitalWrite(WAKEPIN, LOW);

  //Serial Initialization of GPRS
  pinMode(GPRSPOWERPIN, OUTPUT);  //Power pin for on/off - HIGH/LOW
  GPRS_Serial.begin(19200); //GPRS Shield baud rate
  
  //testing purposes
  sdReset();

  //Parsing & GPS
  gpsPowerDown();
  if(sdReadSys((char*)REGISTERED_FILEPATH) != 1) {
    gpsPowerUp();
    
    GPS.begin(9600);
    GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
    GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);   // 1 Hz update rate
    GPS.sendCommand(PGCMD_ANTENNA);
    useInterruptGPS(true); //set to false if you want to get rid of unparsed
    delay(1000);
  }
  
  Serial.println("ENterdddding init");
  if(sdReadSys((char*)REGISTERED_FILEPATH) != 1) {
      Serial.println("ENtering init");
      systemInit();
  }

  //Serial.println(SD.exists("/sys"));
  //System Initialize
  Serial.println("system start");
}

void loop() {
  if(sdReadSys((char*)REGISTERED_FILEPATH) != 1) {
    while(GPS.satellites < 3 || GPS.fix < 1) {
      // in case you are not using the interrupt above, you'll
      // need to 'hand query' the GPS, not suggested :(
      if (!usingInterruptGPS) {
        // read data from the GPS in the 'main loop'
        char c = GPS.read();
        // if you want to debug, this is a good time to do it!
        if (GPSECHO)
          if (c) Serial.print(c);
      }
      
      // if a sentence is received, we can check the checksum, parse it...
      if (GPS.newNMEAreceived()) {
        // a tricky thing here is if we print the NMEA sentence, or data
        // we end up not listening and catching other sentences! 
        // so be very wary if using OUTPUT_ALLDATA and trytng to print out data
         Serial.println(GPS.lastNMEA());   // this also sets the newNMEAreceived() flag to false
      
        if (!GPS.parse(GPS.lastNMEA()))   // this also sets the newNMEAreceived() flag to false
          return;  // we can fail to parse a sentence in which case we should just wait for another
      }
      
      // if millis() or timer wraps around, we'll just reset it
      if (timer > millis())  timer = millis();
      
      // approximately every 2 seconds or so, print out the current stats
      if (millis() - timer > 2000) { 
        timer = millis(); // reset the timer
        
        Serial.print("\nTime: ");
        Serial.print(GPS.hour, DEC); Serial.print(':');
        Serial.print(GPS.minute, DEC); Serial.print(':');
        Serial.print(GPS.seconds, DEC); Serial.print('.');
        Serial.println(GPS.milliseconds);
        Serial.print("Date: ");
        Serial.print(GPS.day, DEC); Serial.print('/');
        Serial.print(GPS.month, DEC); Serial.print("/20");
        Serial.println(GPS.year, DEC);
        Serial.print("Fix: "); Serial.print((int)GPS.fix);
        Serial.print(" quality: "); Serial.println((int)GPS.fixquality); 

        if (GPS.fix) {
          Serial.print("Location: ");
          Serial.print(GPS.latitude, 4); Serial.print(GPS.lat);
          Serial.print(", "); 
          Serial.print(GPS.longitude, 4); Serial.println(GPS.lon);
          
          Serial.print("Speed (knots): "); Serial.println(GPS.speed);
          Serial.print("Angle: "); Serial.println(GPS.angle);
          Serial.print("Altitude: "); Serial.println(GPS.altitude);
          Serial.print("Satellites: "); Serial.println((int)GPS.satellites);
        }
      }
    } //end while (GPS.satellites)
    
    Serial.println("--------------------------------------");
    Serial.print("Fix: "); Serial.print(GPS.fix);
    Serial.print(" quality: "); Serial.println((int)GPS.fixquality);
    Serial.print("Satellites : "); Serial.println(GPS.satellites);
    Serial.print("Altitude: "); Serial.println(GPS.altitude);
    Serial.print("latitutde: "); Serial.println(GPS.latitude);
    Serial.print("longitude: "); Serial.println(GPS.longitude);
    
    while(sdReadSys((char*)REGISTERED_FILEPATH) != 1) {
      gprsPowerUp();
      sendRegisterMessage(GPS.latitude, GPS.lat, GPS.longitude, GPS.lon);

      if (recieveSuccessMessage()) {
        Serial.println("registered");
        sdWriteSys((char*)REGISTERED_FILEPATH, 1); //registered
      }
    }
    
    gpsPowerDown();
    delay(1000);
    gprsPowerDown();
    delay(5000);

    Serial.println("--------------------------------------");
    Serial.print("It is now day: ");
    Serial.println(sdReadSys((char*)TIME_FILEPATH));
    Serial.println(pulses);
    useInterruptGPS(false);
  }
  
  //Start Flowmeter Sensing
  sdGetTime();
  unsigned long wake_session = millis();
  
  while (millis() - wake_session < WAKE_SESSION_INTERVAL) {
    if (pulses != 0) {
      unsigned long flow_session = millis();
      unsigned long prev_pulses = (long)pulses;
      unsigned long start_time = timeNow();
      
      while (millis() - flow_session < FLOW_SESSION_INTERVAL) {
        prev_pulses = pulses;
        
        if (prev_pulses != pulses) {
          flow_session = millis();
          wake_session = millis();
        }
      }
      
      unsigned long end_time = timeNow();
      
      Serial.print("Session ended. Pulses: ");
      Serial.println(pulses);
      sdWriteFlow(String(start_time) + "," + String(end_time) + "," + String(pulses));
      
      pulses = 0;  
      
      //now this does every day after deadline. power hungry. do modulus?
      if (isDataSend()) {
        gprsPowerUp(); //turn on before send data
        Serial.println("Turn on GPRS");
        
        sendDataMessage(0);
        Serial.println("\nMessage sent");
        delay(500);
        
        if (sdReadSys((char*)DATA_SENT_SUCCESS_FILEPATH) == 0) {
          sdSetTime();
          SD.remove("/data/flow.txt");
        }
        
        wake_session = millis();
        
        //gprsSleep(false); //back to sleep
        gprsPowerDown(); //turn off
        Serial.println("Turn off GPRS");
      }
    }
    //Serial.println(millis() - wake_session);
  }
  
  Serial.println("entering sleep func");
  delay(600);
  enterSleep();
}

//----------------ARDUINO METHODS-------------
void systemInit() {
  //SD
  sdInit();
  delay(500);
  sdSetTime();
  delay(500);
}

void wakeUp() {
  //blank callback
}

void enterSleep(void) { 
  set_sleep_mode(SLEEP_MODE_PWR_DOWN);
  sleep_enable();
  attachInterrupt(4, wakeUp, CHANGE);

  Serial.println("sleep enabled");
  sleep_mode();
  /* The program will continue from here. */
  
  /* First thing to do is disable sleep. */
  sleep_disable(); 
  
  detachInterrupt(4);
  Serial.println("exit sleep");
}

//----------------GPRS METHODS----------------
void sendRegisterMessage(float latitude, char lat, float longitude, char lon) {
  flushGPRSSerial();
  
  GPRS_Serial.print("AT+CMGF=1\r");    //Because we want to send the SMS in text mode
  delay(500);
  GPRS_Serial.println("AT+CMGS=\"" + FLOWBIT_NUMBER + "\"");//send sms message, be careful need to add a country code before the cellphone number
  delay(500);
  GPRS_Serial.print("r:");//the content of the message
  GPRS_Serial.print(char(123));
  GPRS_Serial.print("'system_id':'");
  GPRS_Serial.print(SYSTEM_ID);
  GPRS_Serial.print("','secret_key':'");
  GPRS_Serial.print(SECRET_KEY);
  GPRS_Serial.print("','latitude':'");
  GPRS_Serial.print(String(latitude, 4)); 
  GPRS_Serial.print(lat);
  GPRS_Serial.print( "','longitude':'");
  GPRS_Serial.print(String(longitude,4));
  GPRS_Serial.print(lon); 
  GPRS_Serial.print("','meter_size':'");
  GPRS_Serial.print(METER_SIZE);
  GPRS_Serial.print("','meter_conv':'");
  GPRS_Serial.print(METER_CONV);
  GPRS_Serial.print("'");
  GPRS_Serial.print(char(125));
  GPRS_Serial.println(char(26));//the ASCII code of the ctrl+z is 26
  delay(500);
  GPRS_Serial.println();
  
  if (GPRS_Serial.available()) {
    Serial.write(GPRS_Serial.read());
  }
}

void sendDataMessage(unsigned int char_seek) { 
  String data = sdReadFlow(char_seek);
  String message;
  unsigned int _seek;
  
  if(data.endsWith("*")) {
    unsigned int delim_pos = data.indexOf("*");
    unsigned int attempts = 0;
    
    message = data.substring(0, delim_pos);
    _seek = (data.substring(delim_pos + 1, data.length())).toInt();
    
    sendSMS("d" + message);
    sendDataMessage(_seek);
  }
  else {
    sendSMS("d" + data);
  }
}

void sendSMS(String message) {
  unsigned int attempts = 0;
        
  while(attempts < SMS_RETRY_TIMEOUT && !recieveSuccessMessage()) {
    flushGPRSSerial();

    GPRS_Serial.print("AT+CMGF=1\r");    //Because we want to send the SMS in text mode
    delay(500);
    GPRS_Serial.println("AT+CMGS=\"" + FLOWBIT_NUMBER + "\"");//send sms message, be careful need to add a country code before the cellphone number
    delay(500);
    GPRS_Serial.print(message);//the content of the message
    Serial.print(message);
    delay(500);
    GPRS_Serial.println((char)26);//the ASCII code of the ctrl+z is 26
    delay(500);
    GPRS_Serial.println();
    
    if (GPRS_Serial.available()) {
      Serial.write(GPRS_Serial.read());
    }
    
    attempts += 1;
  }

  if(attempts >= SMS_RETRY_TIMEOUT) {
    sdWriteSys((char*)DATA_SENT_SUCCESS_FILEPATH, 0);
  }
}

boolean isDataSend() {  
  Serial.println("time to send?: " + String((timeNow() - sdGetTime()) > SMS_SEND_INTERVAL));
  delay(500);
  Serial.println("Time now " + String(timeNow()));
  delay(500);
  Serial.println("Time old: " + String(sdGetTime()));
  delay(500);
  Serial.println("Time diff: " + String(timeNow() - sdGetTime()));
  delay(500);
  return timeSince(sdGetTime()) > SMS_SEND_INTERVAL;
}

boolean recieveSuccessMessage() {
  delay(SMS_SUCCESS_WAIT); //wait for sms to return from server

  //if success message, return true. else false
  
  return true;
}

void gprsPowerUp() {
  GPRS_Serial.println("AT+CPOWD=1");    //Because we want to send the SMS in text mode
  delay(500);
  
  digitalWrite(GPRSPOWERPIN,LOW);
  delay(1000);
  digitalWrite(GPRSPOWERPIN,HIGH);
  delay(2000);
  digitalWrite(GPRSPOWERPIN,LOW);
  delay(10000);
}

void gprsPowerDown() {
  GPRS_Serial.println("AT+CPOWD=1");    //Because we want to send the SMS in text mode
  delay(500);
}

void flushGPRSSerial() {
  while(GPRS_Serial.available()!=0) {
    GPRS_Serial.read();
  }
}

//----------------GPS METHODS----------------
//Not using Arduino 0023
void useInterruptGPS(boolean); // Func prototype keeps Arduino 0023 happy

SIGNAL(TIMER0_COMPA_vect) {
  char c = GPS.read();
  
  #ifdef UDR0
    if (GPSECHO)
      if (c) UDR0 = c;
      
  #endif
}

void useInterruptGPS(boolean v) {
  if (v) {
    OCR0A = 0xAF;
    TIMSK0 |= _BV(OCIE0A); 
    usingInterruptGPS = true;
  } 
  else {
    TIMSK0 &= ~_BV(OCIE0A);
    usingInterruptGPS = false;
  }
}

void gpsPowerUp() {
  digitalWrite(GPSPOWERPIN, HIGH); 
  delay(5000);
}

void gpsPowerDown() {
  digitalWrite(GPSPOWERPIN, LOW);
  delay(5000);  
}

//----------------FLOWMETER METHODS----------------
SIGNAL(TIMER1_COMPA_vect) { //Signal for flow (changed timer vector 0 to 1)
  uint8_t x = digitalRead(FLOWSENSORPIN);
  if (x == lastflowpinstate) {
    lastflowratetimer++;
    return; // nothing changed!
  } 
  if (x == HIGH) {
    //low to high transition!
    pulses++;
  }
  lastflowpinstate = x;
  flowrate = 1000.0;
  flowrate /= lastflowratetimer;  // in hertz
  lastflowratetimer = 0;
}

//Signal for flow (changed timer vector 0 to 1)
void useInterruptFlow(boolean v) {
  if (v) {
    // Timer0 is already used for millis() - we'll just interrupt somewhere
    // in the middle and call the "Compare A" function above
    OCR0A = 0xAF;
    TIMSK1 |= _BV(OCIE0A);
  } else {
    // do not call the interrupt function COMPA anymore
    TIMSK1 &= ~_BV(OCIE0A);
  }
}

//-------------------SD CARD METHODS--------------
void sdInit() {
  SD.mkdir("/sys");
  SD.mkdir("/data");
  delay(1000);
  sdWriteSys((char*)REGISTERED_FILEPATH, 1); //for testing purposes only!! (skip gps lookup)
  //sdWriteSys(REGISTERED_FILEPATH, 0);
  sdWriteSys((char*)DATA_SENT_SUCCESS_FILEPATH, 0);
}

void sdReset() {
  Serial.println("Entering SD reset");
  delay(500);
  if(SD.exists("/sys")) {
    SD.rmdir("/sys");
    Serial.println("Removed sys");
    delay(500);
  } 
  else {
    Serial.println("Nothing Removed-sys");
    delay(500);
  }
  if(SD.exists("/data")) {
    SD.rmdir("/data");
    Serial.println("Removed data");
    delay(500);
  }
  else {
    Serial.println("Nothing Removed-data");
    delay(500);
  }
  Serial.println("Exiting SD reset");
  delay(500);
}

unsigned int sdReadSys(char* filepath) {
  File file = SD.open(filepath);
  String value;

  if(file) {
    while(file.available()) {
      value += String((char)file.read());
    }

    Serial.println("Read " + value + " from " + String(filepath));
  }
  else {
    Serial.println("Can't read " + String(filepath));
  }
  
  return value.toInt();
}

void sdWriteSys(char* filepath, unsigned int value) {
  if (SD.exists(filepath)) {
    SD.remove(filepath);
  }

  File file = SD.open(filepath, FILE_WRITE);

  if(file) {
    Serial.print("Wrote " + String(value));
    file.print(String(value));

    file.close();

    Serial.println(" to " + String(filepath));
  }
  else {
    Serial.println("Can't write " + String(filepath));
  }
}

void sdSetTime() {
  if (SD.exists((char*)TIME_FILEPATH)) {
    SD.remove((char*)TIME_FILEPATH);
  }

  File file = SD.open((char*)TIME_FILEPATH, FILE_WRITE);

  if(file) {
    Serial.print("Wrote " + String(timeNow()));
    file.print(String(timeNow()));

    file.close();

    Serial.println(" to " + String(TIME_FILEPATH));
  }
  else {
    Serial.println("Can't write " + String(TIME_FILEPATH));
  }
}

unsigned long sdGetTime() {
  File file = SD.open((char*)TIME_FILEPATH);
  String temp_value;

  if(file) {
    while(file.available()) {
      temp_value += String((char)file.read());
    }

    Serial.println("Read " + temp_value + " from " + String((char*)TIME_FILEPATH));
  }
  else {
    Serial.println("Can't read " + String((char*)TIME_FILEPATH));
  }

  char value[temp_value.length()+1];
  temp_value.toCharArray(value, temp_value.length()+1);

  Serial.print("value: ");
  Serial.println(value);
  Serial.println("atol: " + String(atol(value)));
  return atol(value);
}

String sdReadFlow(unsigned int char_seek) {
  // String temp = String("/data/" + String(sdGetTime()) + "flow.txt"); //fix overflow filename bug
  // char* filepath;
  
  // temp.toCharArray(filepath, temp.length());
  
  File file = SD.open("flow.txt");
  unsigned int count = 0;
  String data = "";
  boolean isContinue = false;

  Serial.println("Reading flow.txt");
  
  if(file) {
    file.seek(char_seek);
    
    // read from the file until there's nothing else in it:
    while(file.available()) {
      String temp_data = "";
      char temp_char = (char)26;
      unsigned int temp_count = 0;
      
      while(file.available() && &temp_char != "|") {
        temp_count += 1;
        temp_char = file.read();
        temp_data += temp_char;
      }
      
      if(count + temp_count < SMS_CHAR_LIMIT - 1) {
        count += temp_count;
        data += temp_data;
      }
      else {
        isContinue = true;
        break;
      }
    }
    
    // close the file:
    file.close();
    Serial.println("read data.txt");
  }
  else {
    // if the file didn't open, print an error:
    Serial.println("error reading flow.txt");
  }
  
  if (isContinue) {
    Serial.println("returning" + data + "^" + String(count) + "*");

    return data + "^" + String(count) + "*";
  }
  
  Serial.println("returning" + data);  
  return data;
}

void sdWriteFlow(String data) {
  // String temp = String("/data/" + String(sdGetTime()) + "flow.txt"); //fix overflow filename bug
  // char* filepath;
  
  // temp.toCharArray(filepath, temp.length());
  
  File file = SD.open("flow.txt", FILE_WRITE);
  Serial.println("Writing flow.txt");

  if(file) {
    file.print("|");
    file.print(data);
    
    // close the file:
    file.close();
    Serial.println("done.");
  }
  else {
    // if the file didn't open, print an error:
    Serial.println("error writing flow.txt");
  }
}

//-------------------RTC METHODS------------------
unsigned long timeNow() {
  return RTC.now().unixtime();
}

unsigned long timeSince(unsigned long time) {
  unsigned long diff = timeNow() - time;
  
  return diff;
}

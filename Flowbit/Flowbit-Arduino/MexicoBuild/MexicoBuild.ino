//The following I/O Numbers match these functions
//Pin 13 = Unassigned
//Pin 12 = Unassigned
//Pin 11 = Unassigned
//Pin 10 = Relay 2 On Notification (20 mA)
//Pin 9 = Relay 1 On Notification (20 mA)
//Pin 8 = Data is being collected (20 mA)
//Pin 7 = unassigned
//Pin 6 = unassigned
//Pin 5 = Digital Water Temp Sensor 
//Pin 4 = Flowrate Sensor (15 mA)
//Pin 3 = Relay 2 CTRL (15 mA?)
//Pin 2 = Relay 1 CTRL (15 mA?) 
//Pins 0 + 1 = serial communication

//Analog Inputs
//Analog Pin 0 = Room Temperature
//Analog Pin 1 = unassigned 
//Analog Pin 2 = unassigned
//Analog Pin 3 = unassigned
//Analog Pin 4 = used for offline clock SDA
//Analog Pin 5 = used for offline clock SCL


//CLOCK CLOCK CLOCK
#include "Wire.h"
#define DS1307_I2C_ADDRESS 0x68  // This is the I2C address
// Arduino version compatibility Pre-Compiler Directives
#if defined(ARDUINO) && ARDUINO >= 100   // Arduino v1.0 and newer
  #define I2C_WRITE Wire.write 
  #define I2C_READ Wire.read
#else                                   // Arduino Prior to v1.0 
  #define I2C_WRITE Wire.send 
  #define I2C_READ Wire.receive
#endif

// Global Variables
int command = 0;       // This is the command char, in ascii form, sent from the serial port     
int i;
long previousMillis = 0;        // will store last time Temp was updated
byte second, minute, hour, dayOfWeek, dayOfMonth, month, year;
byte test;
byte zero;
char  *Day[] = {"","Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
char  *Mon[] = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
String theDate = "Timestamp Unknown"; 
// Convert normal decimal numbers to binary coded decimal
byte decToBcd(byte val)
{
  return ( (val/10*16) + (val%10) );
}
 
// Convert binary coded decimal to normal decimal numbers
byte bcdToDec(byte val)
{
  return ( (val/16*10) + (val%16) );
}

void setDateDs1307()                
{
 
   second = (byte) ((Serial.read() - 48) * 10 + (Serial.read() - 48)); // Use of (byte) type casting and ascii math to achieve result.  
   minute = (byte) ((Serial.read() - 48) *10 +  (Serial.read() - 48));
   hour  = (byte) ((Serial.read() - 48) *10 +  (Serial.read() - 48));
   dayOfWeek = (byte) (Serial.read() - 48);
   dayOfMonth = (byte) ((Serial.read() - 48) *10 +  (Serial.read() - 48));
   month = (byte) ((Serial.read() - 48) *10 +  (Serial.read() - 48));
   year= (byte) ((Serial.read() - 48) *10 +  (Serial.read() - 48));
   Wire.beginTransmission(DS1307_I2C_ADDRESS);
   I2C_WRITE(zero);
   I2C_WRITE(decToBcd(second) & 0x7f);    // 0 to bit 7 starts the clock
   I2C_WRITE(decToBcd(minute));
   I2C_WRITE(decToBcd(hour));      // If you want 12 hour am/pm you need to set
                                   // bit 6 (also need to change readDateDs1307)
   I2C_WRITE(decToBcd(dayOfWeek));
   I2C_WRITE(decToBcd(dayOfMonth));
   I2C_WRITE(decToBcd(month));
   I2C_WRITE(decToBcd(year));
   Wire.endTransmission();
}
 
// Gets the date and time from the ds1307 and prints result
void getDateDs1307()
{
  // Reset the register pointer
  Wire.beginTransmission(DS1307_I2C_ADDRESS);
  I2C_WRITE(zero);
  Wire.endTransmission();
 
  Wire.requestFrom(DS1307_I2C_ADDRESS, 7);
 
  // A few of these need masks because certain bits are control bits
  second     = bcdToDec(I2C_READ() & 0x7f);
  minute     = bcdToDec(I2C_READ());
  hour       = bcdToDec(I2C_READ() & 0x3f);  // Need to change this if 12 hour am/pm
  dayOfWeek  = bcdToDec(I2C_READ());
  dayOfMonth = bcdToDec(I2C_READ());
  month      = bcdToDec(I2C_READ());
  year       = bcdToDec(I2C_READ());
 
 //messy code but I think it works 
  if (hour < 10)
    theDate = "0";
  theDate = String(hour, DEC); 
  theDate = theDate + ":"; 
  if (minute < 10)
    theDate = theDate + "0"; 
  theDate = theDate + String(minute, DEC); 
  theDate = theDate + ":"; 
  if (second < 10)
    theDate = theDate + "0"; 
  theDate = theDate + String(second, DEC);
  theDate = theDate + " ";
  theDate = theDate + Day[dayOfWeek];
  theDate = theDate + " ";
  theDate = theDate + String(dayOfMonth, DEC);
  theDate = theDate + " ";
  theDate = theDate + Mon[month];
  theDate = theDate + " 20";
  if (year < 10)
    theDate = theDate + "0";
  theDate = theDate + String(year, DEC);
 
}

//CLOCK CLOCK CLOCK

#include <OneWire.h>

// which pin to use for reading the sensor? can use any pin!
#define FLOWSENSORPIN 4

// count how many pulses!
volatile uint16_t pulses = 0;
// track the state of the pulse pin
volatile uint8_t lastflowpinstate;
// you can try to keep time of how long it is between pulses
volatile uint32_t lastflowratetimer = 0;
// and use that to calculate a flow rate
volatile float flowrate;
// Interrupt is called once a millisecond, looks for any pulses from the sensor!

//analog pins
int sensorPin = 0; 

//digital I/O
int relayPinOne = 2; //Relay 1
int relayPinTwo = 3; //Relay 2
//Pin 4 defined above, so no need to define it here
int dataLED = 8; 
int relayOneLED = 9; 
int relayTwoLED = 10; 

OneWire  ds(5);  // Place water temperature sensor on Pin 10

/*
For serial instructions---
 Order of CSV values as of 2/17:
 ["Relay 1 ON/OFF","Relay 2 ON/OFF"] 
 */
String CSVorder[4];
boolean instructionReady = false;

//on/off state for controllable items, false by default
boolean relayOneOn = false; 
boolean relayTwoOn = false; 

//for the flowrate sensor
SIGNAL(TIMER0_COMPA_vect) {
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

void useInterrupt(boolean v) 
{
  if (v) {
    // Timer0 is already used for millis() - we'll just interrupt somewhere
    // in the middle and call the "Compare A" function above
    OCR0A = 0xAF;
    TIMSK0 |= _BV(OCIE0A);
  } 
  else {
    // do not call the interrupt function COMPA anymore
    TIMSK0 &= ~_BV(OCIE0A);
  }
}

void setup() 
{
  delay(1000); 
  //SET UP RELAYS AND LEDS
  pinMode(relayOneLED, OUTPUT); 
  pinMode(relayTwoLED, OUTPUT); 
  pinMode(relayPinOne, OUTPUT); 
  pinMode(relayPinTwo, OUTPUT); 
   
  //turn on Relays automatically at the beginning
  digitalWrite(relayPinOne, HIGH);  //we are using the SeeedStudio relays; LOW = ON, HIGH = OFF 
  digitalWrite(relayOneLED, !digitalRead(relayPinOne)); 
  relayOneOn = false; 
  digitalWrite(relayPinTwo, HIGH); 
  digitalWrite(relayTwoLED, !digitalRead(relayPinTwo)); 
  relayTwoOn = false; 
  
  delay(10000); 
  Serial.begin(38400);
  delay(100); 
  
  //CLOCK
  Wire.begin(); 
  zero=0x00; 
  //CLOCK

//define LEDs (by default I believe they are off)
  pinMode(dataLED, OUTPUT);
  
  //Define flowrate pin 
  pinMode(FLOWSENSORPIN, INPUT);
  digitalWrite(FLOWSENSORPIN, HIGH);
  lastflowpinstate = digitalRead(FLOWSENSORPIN);
  useInterrupt(true);
  
  delay(100); 
  serialInstructionRead();
  doInstruction();
}

void loop()                     // run over and over again
{ 
  delay(3000);
  serialInstructionRead();
  doInstruction();

  digitalWrite(dataLED, HIGH);   // turn the LED on (HIGH is the voltage level)

//DIGITAL WATER TEMPERATURE CODE

//DIGITAL WATER TEMPERATURE CODE
  byte i;
  byte present = 0;
  byte type_s;
  byte data[12];
  byte addr[8];
  float celsius, fahrenheit; 
  
    if ( !ds.search(addr)) {
   // Serial.println("No more addresses.");
   // Serial.println();
    ds.reset_search();
    delay(250);
    return;
  }
  
  ds.reset();
  delay(250); //pause and reflect on your life
  ds.select(addr);
  ds.write(0x44,1);         // start conversion, with parasite power on at the end
  
  present = ds.reset();
  ds.select(addr);    
  ds.write(0xBE);         // Read Scratchpad
  
  for ( i = 0; i < 9; i++) {           // we need 9 bytes
    data[i] = ds.read();
  }
  
  unsigned int raw = (data[1] << 8) | data[0];
  if (type_s) {
    raw = raw << 3; // 9 bit resolution default
    if (data[7] == 0x10) {
      // count remain gives full 12 bit resolution
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  } else {
    byte cfg = (data[4] & 0x60);
    if (cfg == 0x00) raw = raw << 3;  // 9 bit resolution, 93.75 ms
    else if (cfg == 0x20) raw = raw << 2; // 10 bit res, 187.5 ms
    else if (cfg == 0x40) raw = raw << 1; // 11 bit res, 375 ms
    // default is 12 bit resolution, 750 ms conversion time
  }
  
  celsius = (float)raw / 16.0;
  fahrenheit = celsius * 1.8 + 32.0;
  
  
  
    //AIR TEMPERATURE CODE 
  int reading = analogRead(sensorPin);  

  // converting that reading to voltage, for 3.3v arduino use 3.3
  float voltage = reading * 5.0;
  voltage /= 1024.0; 

  // now print out the temperature
  float temperatureC = (voltage - 0.5) * 100 + 1.5;  //converting from 10 mv per degree wit 500 mV offset
  //added a 1.5 calibration value 

  // now convert to Fahrenheight
  float temperatureF = (temperatureC * 9.0 / 5.0) + 32.0;
  
  
  
  
//FLOW RATE CODE 
  float liters = pulses;
  float flowratecalc = flowrate/14; //liters per minute, with 14 calibration factor
  
  //not valid if it is below 1 LPM. Probably off. 
  
  //liters /= 14;
  //liters /= 60.0;

  
  // if a brass sensor use the following calculation
  // float liters = pulses;
   liters /= 8.1;
   liters -= 6;
   liters /= 60.0;

   
   //MIGHT WANT TO INSERT CLOCK CODE HERE
   getDateDs1307(); 
   //END CLOCK CODE 

  //Writes JSON sensor data to serial: Start---------------------
  Serial.println("{");
  Serial.println("\t\"title\":\"Flowbit Demo: System No. 3\",");
  Serial.print("\t\"offline_clock\":\""); 
  Serial.print(theDate);
  Serial.println("\","); 
  Serial.println("\t\"version\":\"1.0.0\",");
  Serial.println("\t\"tags\":[");
  Serial.println("\t\"Prototype\"");
  Serial.print("\t\t"); 
  Serial.println("],");
  Serial.println("\t\"location\":{");
  Serial.println("\t\"disposition\":\"fixed\",");
  Serial.println("\t\"ele\":\"1337\",");
  Serial.println("\t\"name\":\"DEMO\",");
  Serial.println("\t\"lat\":37.420269,");
  Serial.println("\t\"exposure\":\"indoor\",");
  Serial.println("\t\"lon\":-122.083949,");
  Serial.println("\t\"domain\":\"physical\"");
  Serial.println("\t},");
  Serial.println("");
  
  Serial.println("\t\"datastreams\":[");
  Serial.println(""); //skip a line for formatting 
  Serial.println("\t{\"id\":\"PumpFlowRate\","); 
  Serial.print("\t\"current_value\":");
 // Serial.print("\t"); 
  Serial.print(flowratecalc); 
  Serial.println(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"basicSI\",");
  Serial.println("\t\"label\":\"Liters Per Minute\",");
  Serial.println("\t\"symbol\":\"LPM\"");
  Serial.println("\t\t} ");
  Serial.println("\t},"); //note the comma 
  
  Serial.println("\t{\"id\":\"TankWaterTempSI\", \"current_value\":");
  Serial.print("\t"); 
  Serial.print(celsius); 
  Serial.print(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"basicSI\",");
  Serial.println("\t\"label\":\"Celsius\",");
  Serial.println("\t\"symbol\":\"C\"");
  Serial.println("\t} ");
  Serial.println("\t},"); //note the comma 
  
  Serial.println("\t{\"id\":\"TankWaterTempUS\", \"current_value\":");
  Serial.print("\t"); 
  Serial.print(fahrenheit); 
  Serial.print(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"American\",");
  Serial.println("\t\"label\":\"Fahrenheit\",");
  Serial.println("\t\"symbol\":\"F\"");
  Serial.println("\t} ");
  Serial.println("\t},");
  
  Serial.println("\t{\"id\":\"OutsideTankTempSI\", \"current_value\":");
  Serial.print("\t"); 
  Serial.print(temperatureC); 
  Serial.print(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"basicSI\",");
  Serial.println("\t\"label\":\"Celsius\",");
  Serial.println("\t\"symbol\":\"C\"");
  Serial.println("\t} ");
  Serial.println("\t},"); //note the comma 
  
  Serial.println("\t{\"id\":\"OutsideTankTempUS\", \"current_value\":");
  Serial.print("\t"); 
  Serial.print(temperatureF); 
  Serial.print(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"American\",");
  Serial.println("\t\"label\":\"Fahrenheit\",");
  Serial.println("\t\"symbol\":\"F\"");
  Serial.println("\t} ");
  Serial.println("\t},");

  Serial.println("\t{\"id\":\"RelayOneON\","); 
  Serial.print("\t\"current_value\": ");
  //Serial.print("\t"); 
  Serial.print(!digitalRead(relayPinOne)); //= 1 if on, = 0 if off 
  Serial.println(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"boolean\"");
  Serial.println("\t\t} ");
  Serial.println("\t},");
  
  Serial.println("\t{\"id\":\"RelayTwoON\","); 
  Serial.print("\t\"current_value\": ");
 // Serial.print("\t"); 
  Serial.print(!digitalRead(relayPinTwo)); //= 1 if on, = 0 if off 
  Serial.println(","); 
  Serial.println("\t\"unit\": {");
  Serial.println("\t\"type\":\"boolean\"");
  Serial.println("\t\t} ");
  Serial.println("\t}");
  
  Serial.print("\t"); 
  Serial.println("]");
  Serial.println(""); //skip a line for formatting
  Serial.println("}");
  //Writes JSON sensor data to serial: End---------------------
  
  //Serial.print(liters); Serial.println(" Liters");
  //Serial.print("Dispensing at "); Serial.print(flowratecalc); Serial.println(" liters per minute."); 

  digitalWrite(dataLED, LOW);    // turn the LED off by making the voltage LOW
  //digitalWrite(green, HIGH);  //waiting a second
  delay(1000);
  serialInstructionRead();
  doInstruction();
}

/*-----
 Reads the serial input RX pin from the Linux Box and parses it to an
 instructions array.
 -----*/
void serialInstructionRead()
{
  int iter = 0;
  char delimiter = ',';
  String tempStr = "";

  while (Serial.available()){
    char inChar = (char)Serial.read(); 
    // Serial.print("The character is "); Serial.println(inChar); 

//ADJUST CLOCK CODE
   // if (inChar == 84 || inChar == 116) {
   //  setDateDs1307();
   // }
//ADJUST CLOCK CODE

    if (inChar == delimiter) {
      CSVorder[iter] = tempStr;
      iter=iter+1;
      tempStr = "";
    } 
    else if (inChar == '\n') {
      CSVorder[iter] = tempStr;
      iter=iter+1; 
      instructionReady = true;
    } 
    else {
      tempStr += inChar;
    }
  }
}


/*------
 Wrapper function that will do all actions based on the commands
 parameters sent to it via serial. Add new actions and 
 functionality here.
 ------*/
void doInstruction()
{
  if (instructionReady) {
    //Add all actions below
   /* Order of CSV values as of 4/8/13:
 ["RELAY ONE ON/OFF", "RELAY TWO ON/OFF"] 
 */
   fControlRelayOne(CSVorder[0]); 
   fControlRelayTwo(CSVorder[1]); 
   
   // controlPump(CSVorder[0]); 
   // controlLamp(CSVorder[1]);

    instructionReady = false;
  } 
}

/*----Add all functionality and actions below----*/

void fControlRelayOne(String command)
{
  if (command.equals("ON")) {
    digitalWrite(relayPinOne, LOW);
    relayOneOn = true; 
    digitalWrite(relayOneLED, !digitalRead(relayPinOne)); 
    }

  else if (command.equals("OFF")) {
    digitalWrite(relayPinOne, HIGH);
    relayOneOn = false; 
    digitalWrite(relayOneLED, !digitalRead(relayPinOne)); 
    }
}

void fControlRelayTwo(String command)
{
  if (command.equals("ON")) {
    digitalWrite(relayPinTwo, LOW);
    relayTwoOn = true; 
    digitalWrite(relayTwoLED, !digitalRead(relayPinTwo)); 
    }
    
  else if (command.equals("OFF")) {
    digitalWrite(relayPinTwo, HIGH);
    relayTwoOn = false; 
    digitalWrite(relayTwoLED, !digitalRead(relayPinTwo)); 
    }
}

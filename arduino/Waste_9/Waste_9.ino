/**
 * Copyright © 2019 Gabriele Maffoni
 * This code permits an Arduino Bluno Beetle - Based on an arduino Uno - to read data from a bottle. It will aslo receive messages from a raspberry pi and send them online through it.
 * 
 * CHANGELOG:
 * - Added comments
 */

#include <Wire.h> //To make I2C sensors work
#include <Adafruit_NeoPixel.h> //For the LED
#include <ArduinoJson.h> // For sending the data on the Serial monitor.
//Setting up I2C Time of Flight sensor ("ToF" from now on).
#include <SparkFun_VL6180X.h> //To get the ToF Sensor library

#define VL6180X_ADDRESS 0x29 //Defining ToF sensor address for I2C
VL6180xIdentification identification;
VL6180x tof_sensor(VL6180X_ADDRESS);

//Declaring all variables
const int button_pin = 2;
//const int voltage_distance_sensor = 5; //For the digital output
const int ext_temp_sensor = 2;
const int int_temp_sensor = 3;
const int led_pin = 4;
const int battery_pin = 1;
const int led_pin_low = 5;
Adafruit_NeoPixel led = Adafruit_NeoPixel(1, led_pin, NEO_GRB + NEO_KHZ800); //initalising the LED
int button_state = 0; //To know whether the button is pressed or not
int general_counter = 0; //The counter that checks when it is time to check temperature again
//TEMPERATURE
int internal_temperature = 0;//To save the internal temp
int external_temperature = 0; //To save the external temp

//JSON-related variables
const size_t capacity = JSON_OBJECT_SIZE(7);
DynamicJsonDocument doc(capacity);

//QUANTITY
long quantity_in_distance = 0;
float final_quantity = 0;
double ratio_quantity = 0;

//BUTTON
bool button_pressed = false; //boolean to check whether the button is pressed
int long_press_timer = 3000; //3 secs how much is the seconds for which you can consider it long pressed
long button_timer = 0; //How long the button is pressed for
bool long_press_active = false; //if the long press is active

bool data_request = false;
bool first_setup = true;
bool reset_mode = false;
bool light_issue = false;
float final_timer = 0;

//TOTAL COUNTER OF STREAMS
int streams_counter = 1;
long tmp_time = 0;

long light_check_seconds = 15000;
long light_timer = 0;
long waiting_check_time = 3000;
long waiting_time = 0;
int button_counter = 0;
bool door_open = false;
String line_to_check = "";
int days_until_expiration = 0;

bool emergency_mode = false;
int light_counter = 0;
long voltage_timer = 0;
int voltage_check_time = 30000;
void setup() {
  // put your setup code here, to run once:
  
  Serial.begin(115200); // Starting the serial at 115200;
  while (!Serial){
    break;
  }
  Wire.begin(); //Starting I2C library
  led.begin(); //Turning on the LED
  first_led_check(); // Checks whether the LED is okay or not
  get_voltage(); // Prints the voltage on the Serial
  
  //All input
  pinMode (button_pin, INPUT);
  pinMode (led_pin, OUTPUT);
  pinMode (led_pin_low,OUTPUT);
  digitalWrite(led_pin_low, LOW); // We transform the led_pin_low to a GND pin
  delay(100); //For safety reasons
  Serial.println("Done starting, intialising ToF sensor!"); // Starting to initialise the sensors
  initialize_tof_sensor();
  tmp_time = millis(); // Getting next checks
  light_timer = tmp_time+light_check_seconds;
  waiting_time = millis()+waiting_check_time;
  voltage_timer = millis() + voltage_check_time; //We'll check the voltage every 30 seconds.
  Serial.println("SETUP DONE");

}

void loop() {
  // The line will be resetted
  line_to_check = "";
    // if it is not first setup mode
    if (first_setup == false){
      // Write Waiting for commands
      Serial.println("Waiting for commands");
    }
    // IF instead is first setup mode
    if (first_setup == true){
      // We turn off the long press mode
      long_press_active = false;
      // We turn off the first setup mode to avoid loops
      first_setup = false;
      // We restart stream counter
      streams_counter = 0;
      // We restart the light counter
      light_counter = 0;
      // We print on the serial that it is the first setup. Meaning that the Raspberry has to initialise something
      Serial.println("FIRST_SETUP!");
      // We visually say to the user that it's first setup mode by multicolouring our LED
      while(Serial.available() == 0){
        blink_led_multicolor(500);
      }
   }
    // While there is no message from the Raspberry
    while(Serial.available() == 0){
      // We check whether the button has been pressed
      button_state = digitalRead(button_pin);
      check_button();
      // We check if it is time to check the light again
      if (millis() > light_timer){
        check_light();
        light_timer = millis() + waiting_check_time;
      }
      // We check if it is time to check the voltage
      if (millis() > voltage_timer){
        get_voltage();
        voltage_timer = millis() + voltage_check_time;
      }
    
    }
    // If there is new message forom the raspberry
    if (Serial.available() > 0){
      // Save the line to check
      line_to_check = Serial.readString();
    }

   // What does it say?
   if (line_to_check.substring(0) == "data"){
      // Sends all data
      get_data();
    } else if (line_to_check.substring(0) == "luminosity"){
      // prints luminosity
      get_luminosity();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "quantity"){
      // Prints the distance - quantity mm
      get_distance();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "temperature"){
      // prints the temperature
      get_temperature();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "expiration"){
      // Gets the expiration days everyday
      get_expiration_days();
    }

   
}


/**
 * LED METHODS
 * All the colour method are done because the Adafruit LED requires several lines of code to be coloured.
 * Black() means that it is off.
 *  Everytime we change code is important also use the .show() method to show the colour
 */
 
void first_led_check(){
  // Simply changes colours to the LED in 2 seconds
  Serial.println("Checking LED!");
  white();
  delay(500);
  blue();
  delay(500);
  red();
  delay(500);
  green();
  delay(500);
  black();
  Serial.println("Led checked!");
}

void white(){

  led.setPixelColor(0, led.Color(255,255,255));
  led.show();
}


void yellow(){
  led.setPixelColor(0, led.Color(255,211,0));
  led.show();
}

void black(){
  led.setPixelColor(0, led.Color(0,0,0));
  led.show();
}

void blue(){
  led.setPixelColor(0,led.Color(0,0,255));
  led.show();
}

void red(){
  led.setPixelColor(0, led.Color(255,0,0));
  led.show();
}

void green(){
  led.setPixelColor(0, led.Color(0,255,0));
  led.show();
}

void blink_led(int set_delay){
  //A method that makes the LED blink for a set mount of time
   white();
   delay(set_delay);
   black();
   delay(set_delay);
}

void blink_led_multicolor(int set_delay){
  white();
  delay(set_delay);
  blue();
  delay(set_delay);
  red();
  delay(set_delay);
  yellow();
  delay(set_delay);
  green();
  delay(set_delay);
  black();
  delay(set_delay);
}

/**
 * TIME OF FLIGHT SENSOR!
 * This intialises the time of flight sensor, checking whether it exists or not. In case it doesn't it says that there is a problem.
 */

void initialize_tof_sensor(){
  /*Copied and interepreted by the VL6180X hookup guide*/
  tof_sensor.getIdentification(&identification); // Retrieve manufacture info from device memory
  printIdentification(&identification); // Helper function to print all the Module information

  int initialisation_code = tof_sensor.VL6180xInit();
  if(initialisation_code != 0){
    Serial.println("There has been problems initializing the Time of Flight sensor."); //Initialize device and check for errors
  } else {
    Serial.println("Initialised!");
  }


  tof_sensor.VL6180xDefautSettings(); //Load default settings to get started.

}


void printIdentification(struct VL6180xIdentification *temp){
 
  Serial.print("Model ID = ");
  Serial.println(temp->idModel);

  Serial.print("Model Rev = ");
  Serial.print(temp->idModelRevMajor);
  Serial.print(".");
  Serial.println(temp->idModelRevMinor);

  Serial.print("Module Rev = ");
  Serial.print(temp->idModuleRevMajor);
  Serial.print(".");
  Serial.println(temp->idModuleRevMinor);

  Serial.print("Manufacture Date = ");
  Serial.print((temp->idDate >> 3) & 0x001F);
  Serial.print("/");
  Serial.print((temp->idDate >> 8) & 0x000F);
  Serial.print("/1");
  Serial.print((temp->idDate >> 12) & 0x000F);
  Serial.print(" Phase: ");
  Serial.println(temp->idDate & 0x0007);

  Serial.print("Manufacture Time (s)= ");
  Serial.println(temp->idTime * 2);
  Serial.println();
  Serial.println();
}

/**
 * Gets the internal temperature and converts it.
 * Returns the internal temperature
 */

float get_internal_temperature(){
  //Serial.println("Get internal temperature");
  float final_temperature = 0;
  internal_temperature = analogRead(int_temp_sensor);
  //Serial.println("Internal temperature got!");
   //Gets 5.0 V voltage
   float voltage = internal_temperature * 5.0;
   voltage /= 1024.0;
   //Converts it to C
   final_temperature = (voltage - 0.5) * 100;

  return final_temperature;
}


/**
 * Gets the external temperature and converts it.
 * Returns the external temperature
 */

float get_external_temperature(){
    float final_temperature = 0;
    external_temperature = analogRead(int_temp_sensor);
   //Gets 5.0 V voltage
   float voltage = internal_temperature * 5.0;
   voltage /= 1024.0;
   //Converts it to C
   final_temperature = (voltage - 0.5) * 100;
  return final_temperature;
}

/**
 * This method returns the value of the luminosity as float
 * @returns Float: value of the luminosity
 */

float luminosity(){
  float luminosity = tof_sensor.getAmbientLight(GAIN_1);
  return luminosity;
}

/**
 * Gathers and converts the voltage from an analog pin.
 * Code borrowed by a real useful tutorial on instructables by chineline: https://www.instructables.com/id/Arduino-Battery-Voltage-Indicator/ 
 * Check it out!
 */

void get_voltage(){
  int sensorValue = analogRead(battery_pin); //read the A0 pin value
  float voltage = sensorValue * (5.00 / 1023.00) * 2; //convert the value to a true voltage.
  Serial.println("Voltage: " + String(voltage));
}

// Set on the JSON file the "stream_number"
void set_stream_number(){
  doc["stream_number"] = streams_counter;
}

// Set the returned value of luminosity on the json file
void get_luminosity(){
  doc["luminosity"] = luminosity();
}

/**
 * Gets both temepratures and adds them to the JSON
 */
void get_temperature(){
   doc["int_temperature"] = get_internal_temperature();
   doc["ext_temperature"]= get_external_temperature();
}

/**
 * Gets the distance and prints them on the Serial monitor
 */


void get_distance(){
  float quantity = tof_sensor.getDistance();
  doc["quantity_left"]= quantity;

}


// Gathers all the data
void get_data(){
    set_stream_number();
    get_temperature();
    get_distance();
    get_luminosity();
    send_data();

}

// Prints the data on the serial as a prettified JSON
void send_data(){
  //SENDING DATA
  serializeJsonPretty(doc, Serial); // Prettifing JSON
  streams_counter++;
  Serial.println(""); // Adds a new line, as the JSON library doesn't
}


/**
 * Checks if the button is being pressed. If so it keeps the status of pressed until it hits the reset time.
 */

void check_button(){
   if (button_state == HIGH){
    //The moment the button is pressed it starts counting.
     if (button_pressed == false && final_timer == 0){
      button_pressed = true;
      button_timer = millis(); // gets the now moment
      final_timer = button_timer + long_press_timer; //it'll wait for 3s to considering it long pressed
    }
     //When button is long pressed
    if ((millis() > final_timer)&&(long_press_active == false)){
      //Button is long pressed
      long_press_active = true;
      first_setup = true;
      //Sends reset message
      Serial.println("RESET_MODE");
      /**
       * Every time the button is pressed, it lights up the led as white and then black again for a quarter of second
       */
      } else if ((millis() < final_timer) && (long_press_active == false)){
        blink_led(250);
        Serial.println("Pressing button");
      }

  } 
 
}

/**
 * Checks the light in the environment. If it is ON keeps counting.
 * Meanwhile, if the light is on for the first time it will light up with the right colour from the expiration day.
 */

void check_light(){
  // If luminosity is more than 0
  if (luminosity() > 0){
    // Says that the light is on
    Serial.println("LIGHT_ON!");
    door_open = true;
    // Lights up the LED for 3 seconds at the first time until the door closes again.
    if (light_counter == 0){
      long fade_timer = millis() + 3000;
      long stop_timer = millis();
      while(stop_timer < fade_timer){
        if (days_until_expiration <=2){
          red();
        } else if (days_until_expiration < 5){
          yellow();
        } else if (days_until_expiration >= 5){
          green();
        }
        stop_timer = millis();
      }
      black();
      light_counter += 1;
    }
    // If the door is closed again it prints "LIGHT_OFF"
  } else if (luminosity() <= 0 && door_open == true){
    Serial.println("LIGHT_OFF");
    door_open = false;
    light_counter = 0; // Sets the counter to 0
  }
}

/**
 * When it receives expiration date, waits for the serial to print
 */
void get_expiration_days(){
  Serial.println("DAYS_TO_EXPIRATION");
  /**
   * The moment somethi arrives to the serial it converts it to an integer.
   */
  while(Serial.available() == 0){
    //Do nothing
  }
  if (Serial.available()> 0){
    // Sets the general variable as the right amount.
    days_until_expiration = Serial.readString().toInt();
  }

}

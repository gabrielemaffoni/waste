#include <Wire.h> //To make I2C sensors work
#include <Adafruit_NeoPixel.h> //For the LED
#include <ArduinoJson.h>
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
int long_press_timer = 3000; //10 secs
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
void setup() {
  // put your setup code here, to run once:
  
  Serial.begin(115200); // Starting the serial at 115200;
  while (!Serial){
    break;
  }
  Wire.begin(); //Starting I2C library
  led.begin(); //Turning on the LED
  first_led_check();

  //All input
  pinMode (button_pin, INPUT);
  pinMode (led_pin, OUTPUT);
  delay(100); //For safety reasons
  Serial.println("Done starting, intialising ToF sensor!");
  initialize_tof_sensor();
  tmp_time = millis();
  light_timer = tmp_time+light_check_seconds;
  waiting_time = millis()+waiting_check_time;
  Serial.println("SETUP DONE");

}

void loop() {
  line_to_check = "";
  // put your main code here, to run repeatedly:


    if (first_setup == false){
      Serial.println("Waiting for commands");
    }
    if (first_setup == true){
      long_press_active = false;
      first_setup = false;
      streams_counter = 0;
      light_counter = 0;
      Serial.println("FIRST_SETUP!");
   }

    while(Serial.available() == 0){
      button_state = digitalRead(button_pin);
      check_button();
    
      
      if (millis() > light_timer){
        check_light();
        light_timer = millis() + waiting_check_time;
      }
    }
    
    if (Serial.available() > 0){
      line_to_check = Serial.readString();
    }

  
   if (line_to_check.substring(0) == "data"){
      get_data();
    } else if (line_to_check.substring(0) == "luminosity"){
      get_luminosity();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "quantity"){
      get_distance();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "temperature"){
      get_temperature();
      send_data();
      streams_counter--;
    } else if (line_to_check.substring(0) == "expiration"){
      get_expiration_days();
    }

   
}



void first_led_check(){
  Serial.println("Checking LED!");
  white();
  delay(100);
  blue();
  delay(100);
  red();
  delay(100);
  green();
  delay(100);
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

void initialize_tof_sensor(){
  /*Copied and interepreted by the VL6180X hookup guide*/
  //digitalWrite(voltage_distance_sensor, HIGH);
  tof_sensor.getIdentification(&identification); // Retrieve manufacture info from device memory
  printIdentification(&identification); // Helper function to print all the Module information

  int initialisation_code = tof_sensor.VL6180xInit();

  if(initialisation_code != 0){
    Serial.println("There has been problems initializing the Time of Flight sensor."); //Initialize device and check for errors
  } else {
    Serial.println("Initialised!");
  }


  tof_sensor.VL6180xDefautSettings(); //Load default settings to get started.

  //digitalWrite(voltage_distance_sensor, LOW);

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

float get_external_temperature(){
  //Serial.println("Get external temperature");
  float final_temperature = 0;
  external_temperature = analogRead(int_temp_sensor);
  //Serial.println("External temperature got!");
   //Gets 5.0 V voltage
   float voltage = internal_temperature * 5.0;
   voltage /= 1024.0;
   //Converts it to C
   final_temperature = (voltage - 0.5) * 100;
  return final_temperature;
}

void set_stream_number(){
  doc["stream_number"] = streams_counter;
}

void get_luminosity(){
  doc["luminosity"] = luminosity();
}

void send_data(){
  //SENDING DATA
  serializeJsonPretty(doc, Serial);
  streams_counter++;
  Serial.println("");
  //delay(1000);
}

float luminosity(){
  float luminosity = tof_sensor.getAmbientLight(GAIN_1);
  return luminosity;
}

void get_data(){
    set_stream_number();
    get_temperature();
    get_distance();
    get_luminosity();
    send_data();

}



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

void get_temperature(){
   //Serial.println("INT_TEMP: "+String(get_internal_temperature()));
   doc["int_temperature"] = get_internal_temperature();
   doc["ext_temperature"]= get_external_temperature();
   //Serial.println("EXT_TEMP: "+String(get_external_temperature()));
}

void get_distance(){
  float quantity = tof_sensor.getDistance();
  doc["quantity_left"]= quantity;
  //Serial.println("DISTANCE: "+String(quantity));
}

void check_button(){
   if (button_state == HIGH){
    //The moment the button is pressed it starts counting.
     if (button_pressed == false && final_timer == 0){
      button_pressed = true;
      button_timer = millis();
      final_timer = button_timer + long_press_timer; //it'll wait for 15s to considering it long pressed
    }
     //When button is long pressed
    if ((millis() > final_timer)&&(long_press_active == false)){
      //Button is long pressed
      long_press_active = true;
      first_setup = true;
      //Sends reset message
      Serial.println("RESET_MODE");
      } else if ((millis() < final_timer) && (long_press_active == false)){
        blink_led(250);
        Serial.println("Pressing button");
      }

  } 
 
}

void check_light(){
  if (luminosity() > 0){
    Serial.println("LIGHT_ON!");
    door_open = true;
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
  } else if (luminosity() <= 0 && door_open == true){
    Serial.println("LIGHT_OFF");
    door_open = false;
    light_counter = 0;
  }
}

void get_expiration_days(){
  int days_string = 0;
  Serial.println("DAYS_TO_EXPIRATION");
  while(Serial.available() == 0){
    //Do nothing
  }
  if (Serial.available()> 0){
    days_string = Serial.readString().toInt();
  }
  days_until_expiration = days_string;
  Serial.println("Days until expiration " + String(days_string));
}

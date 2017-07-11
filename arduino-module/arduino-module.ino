/*
 * Code by : github.com/heronimus
 * 			 heronimustra@gmail.com
 * 			 @2017
 * Education & experimental fair use.
 * website : http://kucik.in
 *
 */

//---------LIBRARY----------
  #include <dht.h>
  #include <SoftwareSerial.h>


//---------PIN DECLARE-------
  //sensorDHT11 (Temp&Humid)
  dht sensorDHT;
  #define sensorDHT11_PIN 5

  //Bluetooth TX RX
  SoftwareSerial serialBT(2,3);

  //Photocell (Analog)
  int sensorPhotoCell = 0;  
  

  //PIRMotion Sensor
  int sensorPIR = 4;    
  
  //Flame IR Sensor
  
// lowest and highest sensor readings:
const int sensorMin = 512;     // sensor minimum
const int sensorMax = 1024;  // sensor maximum
int range;

  //LED
  int lampu1 = 13;
  int lampu2 = 12;
  int lampuAC = 11;
  
//---------VAR DECLARE-------

  //Photocell Calibrate Light
  int PCminLight=0;          //Used to calibrate the readings
  int PCmaxLight=0;          //Used to calibrate the readings
  int PClightLevel;
  int PCadjustedLightLevel;

  //DHT
  int DHThumid=0;
  int DHTtemp=0;

  //PIR Motion Sensor
  int calibrationTime = 15; 
  long unsigned int lowIn;  
  long unsigned int pause = 3000;  
  boolean lockLow = true;
  boolean takeLowTime; 
  int MotionSave=0;
  

  //Interval Loop Delay
  int baseInterval=10;
  int intervalCounter=0;
  int intervalCounter2=0;
  char dataRead = 0;  
  
  //Lamp Value
  int lampu1Save = 0;
  int lampu2Save = 0;
  int lampuACSave = 0;


//----------------------------
//----------------------------

void setup() {
  //Serial Begin : serialBT = Bluetooth, Serial = monitoring.
  serialBT.begin(9600);
  Serial.begin(9600);

  //PIR Motion Setup
   pinMode(sensorPIR, INPUT);
   pinMode(lampu1, OUTPUT);
   digitalWrite(sensorPIR, LOW);

  //LED Setup
  pinMode(lampu1, OUTPUT);
  pinMode(lampu2, OUTPUT);
  pinMode(lampuAC, OUTPUT);

  
  //PhotoCellSensor Setup
  PClightLevel=analogRead(sensorPhotoCell);
  PCminLight=PClightLevel-20;
  PCmaxLight=PClightLevel;
  
  //Calibrate Sensor
  CalibrateSensor();
  
}

void loop() {


  if(serialBT.available() > 0)  // Send dataRead only when you receive dataRead:
  {
    //Read the incoming dataRead and store it into variable dataRead
    SaklarOnOff();
  }                
  //Sensor Read Interval
  if(intervalCounter==80){
    //DHT11sensor
    DHTRead();
    //PhotoCellSensor
    PhotoCellRead();  

    int sensorReading = analogRead(A1);
    range = map(sensorReading, sensorMin, sensorMax, 0, 100); 
    
    intervalCounter=0;
  }
  
  //Sensor Print Serial Interval
  if(intervalCounter2==200){
    printSerial();
    intervalCounter2=0;
  }

  //Sensor PIR Motion
  PIRMotionRead();


  //Interval Delay
  intervalCounter=intervalCounter+1;
  intervalCounter2=intervalCounter2+1;
  delay(baseInterval);
  
}


//-------------PROCEDURE-------------

void CalibrateSensor(){
  //give the sensor some time to calibrate
    Serial.print("Calibrating sensor ");
    for(int i = 0; i < calibrationTime; i++){
      Serial.print(".");
      delay(1000);
    }
    Serial.println(" done");
    Serial.println("SENSOR ACTIVE");
    delay(50);
}

void DHTRead(){
  int chk = sensorDHT.read11(sensorDHT11_PIN);
  DHThumid = sensorDHT.humidity;
  DHTtemp = sensorDHT.temperature;

  if (isnan(DHTtemp) || isnan(DHThumid)){
    Serial.println("Failed to read from sensorDHT");
  }
}

void PhotoCellRead(){
  PClightLevel=analogRead(sensorPhotoCell);
  if(PCminLight>PClightLevel){
     PCminLight=PClightLevel;
  }
  if(PCmaxLight<PClightLevel){
     PCmaxLight=PClightLevel;
  }
  //Adjust the light level to produce a result between 0 and 100.
  PCadjustedLightLevel = map(PClightLevel, PCmaxLight, PCminLight, 0, 100); 
}

void PIRMotionRead(){
  if(digitalRead(sensorPIR) == HIGH){
       digitalWrite(lampu2, HIGH);   //the led visualizes the sensors output pin state
       if(lockLow){  
         //makes sure we wait for a transition to LOW before any further output is made:
         lockLow = false;            
         Serial.println("----");
         Serial.print("motion detected at ");
         Serial.print(millis()/1000);
         Serial.println(" sec"); 
     
     MotionSave=1;
         delay(50);
         }         
         takeLowTime = true;
   }

   if(digitalRead(sensorPIR) == LOW){       
     
     if(takeLowTime){
      lowIn = millis();          //save the time of the transition from high to LOW
      takeLowTime = false;       //make sure this is only done at the start of a LOW phase
      }
     //if the sensor is low for more than the given pause, 
     //we assume that no more motion is going to happen
     if(!lockLow && millis() - lowIn > pause){  
         //makes sure this block of code is only executed again after 
         //a new motion sequence has been detected
         digitalWrite(lampu2, LOW);  //the led visualizes the sensors output pin state
         lockLow = true;                        
         Serial.print("motion ended at ");      //output
         Serial.print((millis() - pause)/1000);
         Serial.println(" sec");
     
     MotionSave=0;
         delay(50);
      }
   }
}

void SaklarOnOff(){
  dataRead = serialBT.read();    
  Serial.print("--> Data Received : ");  
  Serial.println(dataRead);  
    if(dataRead == '1'){
    digitalWrite(lampu1, HIGH);
    lampu1Save=1;
  }            
    else if(dataRead == '0'){
    digitalWrite(lampu1, LOW);   
    lampu1Save=0;
  }       
  if(dataRead == '3'){
    digitalWrite(lampu2, HIGH);
    lampu2Save=1;
  }            
    else if(dataRead == '2'){
    digitalWrite(lampu2, LOW);   
    lampu2Save=0;
  }       
  if(dataRead == '5'){
    digitalWrite(lampuAC, HIGH);
    lampu1Save=1;
  }            
    else if(dataRead == '4'){
    digitalWrite(lampuAC, LOW);   
    lampuACSave=0;
  }       
}

void printSerial(){
  //Bluetooth Serial Print 
      serialBT.print("#");
    //StartLine #
      serialBT.print("Humid=");
      serialBT.print(DHThumid);
      serialBT.print(",");
      serialBT.print("Temp=");
      serialBT.print(DHTtemp);
      serialBT.print(",");
      serialBT.print("Light=");
      serialBT.print(PCadjustedLightLevel);
      serialBT.print(",");
      serialBT.print("Flame=");
      serialBT.print(range);
      serialBT.print(",");
      serialBT.print("Motion="); 
      serialBT.print(MotionSave);
      serialBT.print(",");
      serialBT.print("lamp1=");
      serialBT.print(lampu1Save);
      serialBT.print(",");
      serialBT.print("lamp2=");
      serialBT.print(lampu2Save);
      serialBT.print(",");
      serialBT.print("lampAC=");
      serialBT.print(lampuACSave);
      serialBT.print(",");
      
      //EndOfLine ~
      serialBT.print("~");

      
    //COM Serial Print 
      Serial.println("-----------------");
      Serial.print("Humid  = ");
      Serial.print(DHThumid);
      Serial.println(" %");
      Serial.print("Temp   = ");
      Serial.print(DHTtemp);
      Serial.println(" C");
      Serial.print("Light  = ");
      Serial.println(PCadjustedLightLevel);
      Serial.print("Flame  = ");
      Serial.println(range);
      Serial.print("Motion = ");
      Serial.println(MotionSave);
      Serial.print("Lamp   = ");
      Serial.print(lampu1Save);
      Serial.print(lampu2Save);
      Serial.println(lampuACSave);
      Serial.println("-----------------");
}



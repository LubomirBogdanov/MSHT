import processing.core.*; 
import processing.xml.*; 

import java.util.Iterator; 
import java.net.*; 
import java.io.*; 
import javax.swing.JOptionPane; 
import java.util.Enumeration; 
import processing.serial.*; 
import java.awt.*; 
import java.awt.event.*; 
import java.beans.*; 
import java.util.*; 
import java.net.*; 
import javax.swing.*; 
import javax.swing.border.*; 
import javax.swing.*; 
import java.awt.event.*; 
import java.beans.*; 
import java.io.*; 
import java.awt.Insets; 
import java.awt.BorderLayout; 
import java.util.*; 
import java.util.concurrent.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class CC3000SensorAppGUI extends PApplet {

/*****************************************************************************
 *
 *  CC3000SensorAppGUI - CC3000 FRAM Sensor Application GUI
 *  Copyright (C) 2011 Texas Instruments Incorporated - http://www.ti.com/
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the   
 *    distribution.
 *
 *    Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *****************************************************************************/

 
 


  //Include the Processing Serial Library in this program
















final int debugLevel = 0;

final int serverPortNumber = 1204;
PImage logo;
PImage backgroundImg;
myThread mt;
boolean hashmapInUse = false; //semaphore for multithreading

protected Socket clientSocket;
ServerSocket serverSocket = null; 
InetAddress addr;
String hostName;   
float speed = 0.4f;// speed of planet orbit around sun

Planet[] solarSystem;

Handle[] handlesTemp;
Handle[] handlesAccel;

String APssid = null;

HashMap nodes = new HashMap(); // holds node data/planets
int planetCount = 0;

boolean handleLock = false; //prevent users from adjusting multiple handles at once
InetAddress inetAddress2 = null;

// Factory Default IP Address of TP-LINK APs from
byte[] defaultTPLINKIP = {
  (byte)192, (byte)168, (byte)1, (byte)1
};

// Factory Default IP Address of buffalo APs from
byte[] defaultBuffaloIP = {
  (byte)192, (byte)168, (byte)11, (byte)1
};
int searchAttempts = 0;
final int searchAttemptsMaxCount = 1;
long timeoutDialogTime = 0;
final int timeoutDialogTimeoutSeconds = 20;

final int selectionAuto = 2;
final int selectionManual = 3;

Logger serialLog = new Logger("serial.log");
Logger infLog = new Logger("inf.log");

int shortScanCount = 0;
/***************************************************************************************************************  
 *  Setup runs only once at the beginning of the GUI execution.  
 *  Sets font, size of the GUI window, background color, etc
 *  This also initializes multithreading and starts the 2nd thread (accepts new socket connections)
 **************************************************************************************************************/
public void setup() {

  size(1000, 700, P3D); //set size of the GUI window. Must be first line in setup or else setup will run twice

  // Disable IPv6 support so we use IPv4 Addresses only.
  System.setProperty("java.net.preferIPv4Stack", "true");

  printJREData();
  
  if(checkIfJREVersionCorrect() == false)
  {
      println("Incorrect Java JRE version detected");
	  JOptionPane.showMessageDialog(frame, "Java v" + System.getProperty("java.version") + "detected. GUI will properly run only with JRE v1.6");	  
	  System.exit(1);
  }  
  
  if (checkConfig() == false)
  {
    int autoConfigChoice = askAutoConfig();
    if (autoConfigChoice == selectionAuto )
    {
      performAutoConfig();
    }
    else if (autoConfigChoice == selectionManual )
    {
      obtainNetworkInterface();
    }
  }
  if(debugLevel > 0)
	println("Starting setup");
  PFont font;
  font = loadFont("KhmerOSClassic-48.vlw"); 
  frameRate(25);
  textFont(font);

  logo = loadImage("ti_logo.png");  

  mt = new myThread(this); // initialize new thread
  mt.start(); // start thread

  // Setup handles for threshold adjustment

    handlesTemp = new Handle[1]; // initialize new type for adjusting temp threshold
  handlesAccel = new Handle[1]; // initialize new type for adjusting accelerometer threshold
  int hsize = 10; // set size of the handle
  handlesTemp[0] = new Handle(10, 60, 70, 10, handlesTemp); //create temp scroller
  handlesAccel[0] = new Handle(325, 60, 50, 10, handlesAccel); // create acc scroller

  backgroundImg = loadImage("andromeda.jpg");
  if(debugLevel > 0)
	println("Setup complete");
}

/***************************************************************************************************************  
 *  draw is our main function. This function runs after Setup is complete and loops indefinitely  
 *  In draw, we update the GUI screen. 
 *  First, we check if any of the open sockets have any data to be read.
 **************************************************************************************************************/
public void draw() {
  String planetIP; // holds the IP address of the planet that is being updated
  Planet p1;       // holds the planet that is being updated

  // Set background color to black
  background(0, 0, 0);

  translate(-550, -250, -400);
  if(debugLevel > 1)
	println(frameRate+" fps");
  image(backgroundImg, 0, 0);
  translate(550, 250, 400);


  image(logo, 680, 12);

  checkBuffer(); //Check if new data is present in the open sockets

  fill(255, 255, 255); // change font color to white
  textSize(15);        // set font size
  text("Set temperature threshold for alarm (\ufffdF).", 10, 47); // label threshold scroller for temp
  textSize(15);        // set font size
  text("Set accelerometer threshold for alarm (g's).", 325, 47); // label threshold scroller for acc  
  handlesTemp[0].update();
  handlesTemp[0].display("temp");

  handlesAccel[0].update();
  handlesAccel[0].display("accel");

  translate(width/2-50, height/2-50); // translate to the center of the GUI to place the "sun" in the middle
  rotateX(radians(55)); // rotate the solar system's plane so that we see everything at an angle
  lights(); // add lights to give objects shadows

    //Draw orbit
  strokeWeight(3);
  stroke(255, 50);
  fill(0, 0, 0, 0);
  ellipse(0, 0, 600, 585);  

  //draw the sun/access point
  noStroke();  // remove strokes
  fill(255, 165, 0); // red fill 
  sphere(62); //draw the sun
  fill(255, 255, 255); //change fill color to white for the font
  textSize(30);  // change font size
  if (planetCount == 1)
  {
    text(planetCount+" Node", 60, 63); // display the number of connected nodes.
  }
  else
  {
    text(planetCount+" Nodes", 60, 63); // display the number of connected nodes.
  }
  text("Connected ", 60, 86);
  textSize(18);  // change font size  
  text(hostName, 60, 109);
  text("" + addr.getHostAddress(), 60, 132);//ipAddr[3]+"." + ipAddr[2]+"."  + ipAddr[1]+"." + ipAddr[0] );


  //Update planets on GUI, but first check to see if it's timed out...
  planetCount = nodes.size(); // how many planets do we have?
  Iterator it = nodes.keySet().iterator(); // create iterator to cycle through hashmap contents.

  while (it.hasNext ()) {
    planetIP = (String)it.next(); //get first key (Our hashmap, called "nodes" contains all planets. Each planet can be pulled from the hashmap using a key. The key is the node's IP address)
    p1 = (Planet)nodes.get(planetIP); // pull planet from hashmap and hold it in temp variable p1

    //check if planet has timed out. If so, remove it from solar system
    if ((millis()-p1.lifeTime) > 1000) { // if 1 second passes since last received packet, remove node from hashmap and close socket.
      try // try to close planet socket. it's timed out!
      {
        //  Force socket linger to 0 so that socket is closed with RST
        // allowing a disconnected node to return successfully. If not,
        // the SYN is followed by ACK, instead of SYN-ACK.
        p1.planetSocket.setSoLinger(true, 0);
        p1.planetSocket.close(); // node timed out. Close socket!
		if(debugLevel > 0)
			println("Timeout.");
      }
      catch (IOException e) 
      { 
        System.err.println("Problem with Communication Server: "+e);
      } 
	  if(debugLevel > 0)
		println("Socket closed = "+p1.planetSocket.isClosed()); // if successful, it should be "true"
      nodes.remove(p1.ip); // remove planet in hashmap
      it = nodes.keySet().iterator(); //reset iterator since hashmap has been changed
      break;
    }
    else { // if this node did not timeout, let's update the GUI with the planet's latest information...
      p1.orbit(300, 290, speed, p1.planetOffset, planetIP, p1.accelX, p1.accelY, p1.accelZ, handlesTemp[0].thresh, handlesAccel[0].thresh, p1.Vcc, p1.lifeTime);
      while (hashmapInUse == true); //continuously loop until the hashmap is no longer in use
      hashmapInUse = true; // Once available, block the use of the hashmap
      nodes.put(planetIP, p1); // update planet in hashmap
      hashmapInUse =false; // done with hashmap, free up resource
      
    }
  }
}




/*******************************************************************************************************************
 * This function is called anytime a new packet arrives within CheckBuffer()
 * CheckBuffer reads the socket, then parses the data. It then sends the new incoming data to this function.
 * It is this function's role to see if it's a new planet or an existing planet's new data.
 * if the planet exists, it will update the existing information within the hashmap. 
 * If it's a new IP address, a new planet will be added to the hashmap.
 *******************************************************************************************************************/
public void addPlanet(String address, int X, int Y, int Z, int temperature, float Vcc, Socket planetSocket) {
  String planetIP = address; // Holds the IP address/key for navigating the hashmap.
  int planetIndex=0; // index so that we can apply the planet offsets so that the nodes equidistant around the solar system
  Planet p1; // holds the current planet that is being updated.

  if (nodes.containsKey(planetIP) == false) { // IP address does not exist. This is a new planet! Add to solar system!
    if(debugLevel > 0)
		println("New node added!");
    planetIndex=0; // reset index
    planetCount = nodes.size()+1; // Found a new planet! Add to solar system and ensure they are equidistant from eachother!

    Iterator it = nodes.keySet().iterator(); // Since a new planet is added, we need to update all of the existing planet's offsets so that they stay equideistant from eachother around the orbit.
    while (it.hasNext ()) {
      planetIP = (String)it.next(); //get first key
      p1 = (Planet)nodes.get(planetIP); // get planet
      p1.planetOffset = 360*((float)planetIndex/((float)planetCount)); // apply new offset
      p1.angle = p1.planetOffset; // update angle
      nodes.put(planetIP, p1); // Since planet has been updated with new offset data, we need to re-add it to the hashmap
      planetIndex++;
    }     
    // now that the existing planet's offsets have been updated and re-added to the hashmap, we can add in our new planet to the hashmap.
    p1 = new Planet(360*((float)planetIndex/((float)planetCount)), 15, 0xffDD33CC, 0, 0, 0, 0, 0, 90, millis(), address, planetSocket); // create new planet!
    //  Planet(float planetOffset, float planetRadius, color surface, int accelX, int accelY, int accelZ, int temp, float Vcc, float ping, int lifeTime, String ip, Socket planetSocket){
    nodes.put(address, p1); // add new planet to hashmap
  }
  else {//This planet exists! we
	if(debugLevel > 0)
		println("Node info updated!");
    p1 = (Planet)nodes.get(planetIP); //load up existing planets parameters
    p1.accelX = X; // update with new X acc data
    p1.accelY = Y; // update with new Y acc data
    p1.accelZ = Z; // update with new Z acc data
    p1.temp = temperature; // update with new temp data
    p1.Vcc = Vcc; // update with new Vcc data
    p1.lifeTime = millis(); // got new data, refresh timeout! 
    nodes.put(address, p1); // update planet in hashmap
  }
}

/*******************************************************************************************************************
 * This function is called by the second thread. 
 * This function tries to create a new socket and will block at accept
 * It won't go beyond the accept() function until a socket is created & binded
 *******************************************************************************************************************/
public void openSocket() { //each planet has it's own socket!

  try { 
    serverSocket = new ServerSocket(serverPortNumber, 10, inetAddress2); // create a new socket at port
	if(debugLevel > 0)
		System.out.println ("Connection Socket Created"); // success!
    try { 
		if(debugLevel > 0)
			System.out.println ("Waiting for Connection");
      clientSocket = serverSocket.accept(); //accepted new planet! Need to initialize into hashmap
      while (hashmapInUse == true); // continuously block until hashmap is available
      hashmapInUse = true; // lock down the hashmap for use
      addPlanet(""+clientSocket.getInetAddress(), 0, 0, 0, 0, 0, clientSocket); //addPlanet(String address, int X, int Y, int Z, int temperature, float signalStrength)
      hashmapInUse = false; // free up resource!
	  if(debugLevel > 0)
		println("New socket accepted");
    } 
    catch (IOException e) 
    { 
      System.err.println("Accept failed."); 
      System.exit(1);
    }
  } 
  catch (IOException e) 
  { 
    System.err.println("Could not listen on port."); 
    System.exit(1);
  } 
  finally
  {
    try {
      serverSocket.close();
	  if(debugLevel > 0)
		println("socket closed");
    }
    catch (IOException e)
    { 
      System.err.println("Could not close port."); 
      System.exit(1);
    }
  }
}

/*******************************************************************************************************************
 * This function is called by main thread. It checks all open sockets to see if there is any new data.
 * If new data is found, it passes it on to the addPlanet function to see if it's a new planet or existing planet data
 * For some reason the first batch of data needs to be discarded...
 *******************************************************************************************************************/
public void checkBuffer() { // Check the buffers of each open socket
  Planet p1; //holds planet that is being updated
  String planetIP; // holds key to hashmap/current planet's IP address
  if(debugLevel > 1)
      println("in CheckBuffer");
  while (hashmapInUse == true); //continuously loop until the hashmap is no longer in use
  hashmapInUse = true; // Once available, block the use of the hashmap

  Iterator it = nodes.keySet().iterator(); // iterate through each planet in hashmap to check if there's any data
  while (it.hasNext ()) {
    try { //need to check the buffer for each open socket!
      planetIP = (String)it.next(); //get first key
      p1 = (Planet)nodes.get(planetIP); // get planet
      BufferedInputStream   in = new BufferedInputStream  (p1.planetSocket.getInputStream()); //read in data for this socket
      byte[] packetBuffer = {
        0, 1, 2, 3, 4, 5, 6, 7
      };

      if (in.available() > 0)
      { //wait for data to get in buffer
        in.read(packetBuffer, 0, 8); // if buffer has data, read it!    
        if (packetBuffer[0]==(byte)13 && packetBuffer[1]==(byte)190 && packetBuffer[7]==(byte)239 ) { // check if packet is valid. If so, add/update planet
          if(debugLevel > 0)
		  {
		  println("Data source :   "+ planetIP);
		  	  
          for (int i=0;i<8;i++)
            print(PApplet.parseInt (packetBuffer[i]) + "  ");
		  }
          addPlanet(""+p1.planetSocket.getInetAddress(), packetBuffer[2], packetBuffer[3], packetBuffer[4], packetBuffer[5], packetBuffer[6], p1.planetSocket); //Update each planet's data
          // addPlanet(String address,                      int X,  int Y,  int Z, int temp, float Vcc, Socket planetSocket){
        }
        else
		{
			if(debugLevel > 0)
				println("Bad packet!!!! Preamble, header/footer do not match!"); // print out bad packet if the preamble, header/footer don't match.
		}
      }
      else
	  {
		if(debugLevel > 0)
			println(" --- EMPTYBUFFER --- ");
	  }
    }
    catch (IOException e) 
    { 
      System.err.println("Cannot Read socket. Problem with Communication Server: "+e);
    }
  }    

  hashmapInUse = false; // free up the resource!
}

/*******************************************************************************************************************
 * This defines the 2nd thread.
 * this thread is responsible for accepting any new sockets
 * sockets remain open until a node timeouts...
 *******************************************************************************************************************/
public class myThread implements Runnable {
  Thread thread;
  private volatile boolean running = true;
  public myThread(PApplet parent) {
  }
  public void start() {
    thread = new Thread(this);
    thread.start();
  }
  public void dispose() { 
    stop();
  }
  public void stop() {
    running = false;
    thread = null;
  }

  public void run() {
    while (running == true) {
	if(debugLevel > 0)
		println("Hello from a thread! Try to open socket");
      delay(1000);

      discoverCC3000();
    }
  }
}


/*******************************************************************************************************************
 * \brief Shows dialog and performs automatic CC3000 configuration
 *
 * This function will display a dialog with the progress of
 * the CC3000 configuration process.
 *
 * \param none
 * \return true if succeeded in configuring the CC3000, false otherwise
 *******************************************************************************************************************/
public boolean performAutoConfig()
{
  Thread th = new Thread();

  int attemptCount = 1;
  String ap = askTPLinkOrBuffalo();
  println ("AP: " + ap); 

  JLabel centerTitleText = new JLabel();
  centerTitleText.setText("CC3000 Automatic Configuration Process");
  JFrame frame = new JFrame("CC3000 Configuration");
	BoxLayout boxLayout = new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS); // top to bottom
    frame.setLayout(boxLayout);
  frame.setLocationRelativeTo(null);
  
  // Get the size of the screen
  Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

  // Determine the new location of the window
  int w = frame.getSize().width;
  int h = frame.getSize().height;
  int x = (dim.width-w)/2;
  int y = (dim.height-h)/2;

  
  Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension windowSize = frame.getSize();

	int windowX = Math.max(0, (screenSize.width  - windowSize.width ) / 2);
	int windowY = Math.max(0, (screenSize.height - windowSize.height) / 2);

	frame.setSize(400, 400);

  // Move the window
  	frame.setLocationRelativeTo(null);
  frame.setLocation(windowX, windowY);

  JPanel topPanel = new JPanel();
  JPanel botPanel = new JPanel();
  
  topPanel.add(centerTitleText);


  JLabel apsearchLabel = new JLabel();
  apsearchLabel.setText("Searching for " + ap + " Access Point");

  topPanel.add(apsearchLabel);


  JLabel ssidLabel = new JLabel();
  ssidLabel.setText("");
  topPanel.add(ssidLabel);    

  JLabel waitingCC3000AssociationLabel = new JLabel();
  waitingCC3000AssociationLabel.setText("");
  topPanel.add(waitingCC3000AssociationLabel);  

  frame.add(topPanel,BorderLayout.NORTH );
  
  
  JList dataList = new JList();
    
	DefaultListModel model;
	model = new DefaultListModel();
	dataList = new JList(model);
	dataList.setSize(200,200);
	JScrollPane pane = new JScrollPane(dataList);  
	pane.setSize(200,200);
	botPanel.add(pane,BorderLayout.NORTH);
frame.add(botPanel,BorderLayout.SOUTH );
  
  frame.setVisible(true);
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  try
  {
    th.sleep(1000);
  }
  catch (InterruptedException ie)
  {
  }        

  // We do auto Config
  boolean ssidFound = false;
  if (ap.equals("buffalo"))
  {
    ssidFound = findBuffaloAP();
  }
  else
   {
      ssidFound = findTPLINKAP();
   }
  if(debugLevel > 0)
	println("SSID found? " + ssidFound);
  if (ssidFound == true)
  {
    apsearchLabel.setText("Searching for " + ap + " Access Point... Found");

    try
    {
      th.sleep(1000);
    }
    catch (InterruptedException ie)
    {
    }

    ssidLabel.setText("Configuring CC3000 with Access Point SSID: " + APssid); 
    int confSuccesses = 0;
    while(attemptCount > 0)
    {
      attemptCount = 0;
	  model.clear();
      SerialPortController sc = new SerialPortController();
      int i = 0;
      
      String portName = null;
      if (sc.getPortCount() > 0)
      {
        for (i = 0; i < sc.getPortCount(); i++)
        {
          portName  = sc.getPortName(i);
          model.add(i,portName);
          SerialPort sp = new SerialPort(portName);
          if (sp.open(this) == 0)
          {
            model.set(i,portName + " => " + "Open");
            if (sp.sendCommand("assoc " + APssid) != 0)
            {
              // Check whether we received Ok.
              if (sp.waitForString("OK", 2) == 0)
              {
                // CC3000 on the port was reconfigured
                model.set(i,portName + " => " + "Configuration Successful");
                confSuccesses++;
				sp.close();
              }
            }
            else
            {
              // Port timed out. Either port is incorrect or MSP430 isn't responding
              model.set(i,portName + " => " + "Response Timeout");
            }
          }
          else
          {
            model.set(i,portName + " => " + "In Use");
            serialLog.Write(portName + " Error: Com Port in use");
			if(debugLevel > 0)
				println("Error: Com Port in use");            
          }
        }
      }

	  try
	  {
		th.sleep(1000);
	  }
	  catch (InterruptedException ie)
	  {
	  }  
      
      if(confSuccesses > 0)
      {
        waitingCC3000AssociationLabel.setText("Waiting for CC3000 to Associate");
         try
        {
          th.sleep(3000);
        }
        catch (InterruptedException ie)
        {
        }                           
        frame.setVisible(false);
        return true;
      }    
      else
      {
        // Error Configuring any of the COM Ports
        // Either Serial Port or another error

        int n = JOptionPane.showConfirmDialog(
        frame, 
        "COM Port Errors or Timeouts occured!\n\nPlease disconnect, then reconnect \nthe USB cable, wait for it to load \nand ensure no other application is using it.\n\nDo you wish to retry?", 
        "COM Port Error", 
        JOptionPane.YES_NO_OPTION);
        if (n == 0)
        {
          attemptCount = 1;  
        }
        else
        {
          frame.setVisible(false);
          return false;
        }
      }
    }
    frame.setVisible(false);
    if(confSuccesses > 0)
    {
        return true;
    }
	return false;    
  }
  else
  {
    frame.setVisible(false);
    apsearchLabel.setText("Searching for " + ap + " Access Point... Error");
    JOptionPane.showMessageDialog(frame, "Unable to find " + ap + " Access Point!\nPlease ensure that it is connected to the computer, and select the appropriate interface in the next screen.");
    obtainNetworkInterface();
    return false;
  }
}

/*******************************************************************************************************************
 * \brief Shows dialog asking user whether to automatically configure CC3000
 *
 * This function will display a dialog with the progress of
 * the CC3000 configuration process.
 *
 * \param none
 * \return true if succeeded in configuring the CC3000, false otherwise
 *******************************************************************************************************************/
public int askAutoConfig()
{ 
  Object[] options = { 
    "Continue", "Skip"
  };
  int selection = JOptionPane.showOptionDialog(null, "Would you like to configure the CC3000 to associate to your AP?\n" +
    "Select Continue to do so automatically. This requires that the FRAM board be connected to the PC using USB\n" +
    "and that your PC is connected to the TP-LINK or buffalo Access Point.\n"+
    "This will only work with the TP-LINK of buffalo Access Point provided in the CC3000 Kit\n\n" +
    "If you previously associated the CC3000 or are not using a TP-LINK or buffalo AP,\n select Skip, where you can select network interface to use regardless of AP", "Automatic Configuration", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);  
  if (selection == 0)
  {    
    // Pressed Continue
    return selectionAuto;
  }
  else
  {
    // Press skip
    return selectionManual;
  }
}

/*******************************************************************************************************************
 * \brief Shows dialog asking user whether to connect to TP-LINK of buffalo AP
 *
 * This function will display a dialog with option
 * to connect to TP-LINK or buffalo AP (Japan)
 *
 * \param none
 * \return a string with TP-LINK or buffalo
 *******************************************************************************************************************/
public String askTPLinkOrBuffalo()
{ 
  Object[] options = { 
    "TP-LINK", "buffalo"
  };
  int selection = JOptionPane.showOptionDialog(null, "Select TP-LINK or buffalo AP.\n", "Select Access Point", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);  
  if (selection == 0)
  {    
    // Pressed Continue
    return "TP-LINK";
  }
  else
  {
    // Press skip
    return "buffalo";
  }
}

/*******************************************************************************************************************
 * \brief Checks whether we previously configured the CC3000 on this computer
 *
 * Reads whether a dummy txt file was created.
 *
 * \param none
 * \return false if not previously configured, true if file exists
 *******************************************************************************************************************/
public boolean checkConfig()
{
  File file=new File("config.txt");
  boolean exists = file.exists();
  if (!exists) {
    // It returns false if File or directory does not exist
	if(debugLevel > 0)
		println("the file or directory you are searching does not exist : " + exists);
    return false;
  }
  else {
    // It returns true if File or directory exists
	if(debugLevel > 0)
		println("the file or directory you are searching does exist : " + exists);
    return true;
  }
}

/*******************************************************************************************************************
 * \brief Shows a dialog box asking the user to select a Network interface used to
 * communicate to the AP.
 *
 * Function lists all network interfaces on the PC
 *
 * \param none
 * \return false if not previously configured, true if file exists
 *******************************************************************************************************************/
public void obtainNetworkInterface()
{
  InetAddress selAddrInf;

  try {

    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

    if (nets.hasMoreElements() == false)
    {
      // No interfaces found.
      JOptionPane.showMessageDialog(frame, "No network interfaces found!\n\nPlease enable and connect an interface to the AP and try again.");
      System.exit(1);
    }    

    ArrayList al = new ArrayList();
    int i = 0;
    for (NetworkInterface netint : Collections.list(nets))
    {
      // Filter interfaces who have IP Addresses because Win7  many irrelevant interfaces
      Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
      if (inetAddresses.hasMoreElements() == true)
      {        
        al.add(netint.getName() + " : " + netint.getDisplayName());
      }
    }

    String input = (String) JOptionPane.showInputDialog(null, "Select Interface connected to AP", 
    "Network Interface Selection", JOptionPane.QUESTION_MESSAGE, null, // Use
    // default
    // icon
    al.toArray(), // Array of choices
    al.toArray()[0]); // Initial choice     

    int interfaceSelFlag = 0;
    while (interfaceSelFlag == 0)
    {
      if (input != null)
      {
        // Remove the trailing display name and just use te name to obtain the interface itself
        NetworkInterface selIntf = NetworkInterface.getByName(input.split(" ")[0]);

        Enumeration<InetAddress> inetAddresses = selIntf.getInetAddresses();
        if (inetAddresses.hasMoreElements() == true)
        {

          selAddrInf = (InetAddress)  inetAddresses.nextElement();
          // Set the hostname and the IP address, which was obtained from the
          // interface previously selected by the user
          hostName = selAddrInf.getLocalHost().getHostName();
          addr = selAddrInf;

		  if(debugLevel > 0)
		  {
			println("hostName " + hostName);
			println("addr: " + addr);
		  }
          interfaceSelFlag = 1;
        }
        else
        {
          JOptionPane.showMessageDialog(frame, "Selected interface is not connected and has no IP address. Please select another interface.");
          interfaceSelFlag = 0;
        }
      }
      else
      {
        // Cancel was selected. Exit.
        System.exit(1);
      }
    }
  }
  catch(IOException e)
  {
    println("IOException ");
  }
}

/*******************************************************************************************************************
 * \brief searches the IP address range for a CC3000 device
 * 
 * Attempts to connect to its port as specified in serverPortNumber, where it should be listening.  
 * After this, it sends the DATA command
 * and the CC3000 begins sending its data. 
 *
 * \param none
 * \return none
 *******************************************************************************************************************/
public void discoverCC3000() 
{
  // Get our address so we can find the address range we should be searching
  if(debugLevel > 0)
	println("addr in discover CC3000 " + addr.getHostAddress()); 
  byte[] cc3000Addr = addr.getAddress();

  int ipStartRange = 0;
  int ipStopRange = 0;  
  int i = 0;
  int error = 0;
  int connectSuccess = 0;
  Socket sock = null;
  InetAddress  cc3000Addr2 = null;


  int ipoctet4 = (cc3000Addr[3]&0xFF);
  if(debugLevel > 0)
	println("Octet4: " + ipoctet4);
  // Determine IP Range to scanning 
  if (ipoctet4 < 100)
   {
     ipStartRange = 0;
     ipStopRange = 150;
   }
   else if (ipoctet4 > 99 && ipoctet4 < 200 && shortScanCount <= 10)
   {
     ipStartRange = 100;
     
     // Stop value gets incremented for each scan so we go and try to check
     // more IPs progressively
     
     ipStopRange = 110 + (shortScanCount*10);
	 if(debugLevel > 0)
		println("Short Scan" + shortScanCount);
     shortScanCount++;
     
   }
   else if(ipoctet4 > 99 && ipoctet4 < 200 && shortScanCount > 10)
   {
     // We've performed 3 short scans, so now we will do a long scan
     if(debugLevel > 0)
		println("Performing Long Scan");
     ipStartRange = 0;
     ipStopRange = 255;
     
     // Set to 0 so next time a short scan is performed
     shortScanCount = 0;
     ipStopRange = 110;
   }

  // Scan IP Range

  for (i = ipStartRange; i < ipStopRange; i++)
  {
    // Reset Error Flag
    error = 0;
	connectSuccess = 0;
    cc3000Addr[3] = (byte)i;
	
	AddressPing ap = new AddressPing(cc3000Addr[0],cc3000Addr[1],cc3000Addr[2],cc3000Addr[3]);
	if(ap.ping() == true)
	{	
		if(debugLevel > 0)
			System.out.println("Success");
		error = 0;		
	}
	else
	{
		error = -1;
	}
  

  if (error != -1)
  {
	
	try
	{
	sock = new Socket();
      cc3000Addr2 = InetAddress.getByAddress("", cc3000Addr);
	  if(debugLevel > 0)
		println("Attempting to connect to: " + cc3000Addr2.getHostAddress());
      SocketAddress sockaddr = new InetSocketAddress(cc3000Addr2, serverPortNumber);

	  
      // Attempt to connect with 100ms timeout
      sock.connect(sockaddr, 200);
	 }
	 catch (UnknownHostException unknownHost) 
    {
		if(debugLevel > 0)
			println("Unknown Host Exception " + cc3000Addr2.getHostAddress() + "\n" + unknownHost.getMessage());
      try
      {
        sock.close();
      }
      catch(IOException ie)
      {
      }
	  connectSuccess = -1;
    }
    catch (SocketTimeoutException e) 
    { 
		if(debugLevel > 0)
			println("SocketTimeoutException Exception: " + cc3000Addr2.getHostAddress() + "\n" + e.getMessage());

      try
      {
        sock.close();
      }
      catch(IOException ie)
      {
      }
	  connectSuccess = -1;
    }
    catch (IOException e) 
    { 
		if(debugLevel > 0)
			println("IO Exception: " + cc3000Addr2.getHostAddress() + "\n" +  e.getMessage());

      try
      {
        sock.close();
      }
      catch(IOException ie)
      {
      }
	  connectSuccess = -1;
	}
	if(connectSuccess != -1)
	{
		if(debugLevel > 0)
			println("Sending Data");
    try 
    {
      // Send Data indicator to tell CC3000 to start sending data
      BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
      wr.write("DATA");
      wr.flush();
    }
    catch(IOException ioe)
    {
		println("Exception writing DATA command");
    }

    // Handle CC300 Planet and use
    while (hashmapInUse == true); // continuously block until hashmap is available
    hashmapInUse = true; // lock down the hashmap for use
    addPlanet(""+cc3000Addr2, 0, 0, 0, 0, 0, sock); //addPlanet(String address, int X, int Y, int Z, int temperature, float signalStrength)
    hashmapInUse = false; // free up resource!
	}
  }
  }

  
	searchAttempts++;
	if (searchAttempts > searchAttemptsMaxCount && planetCount == 0)
	{
	  // Start the timeout dialog countdown if we've attempted searches and no planets found
	  if (dialogTimerStartTime() == 0)
	  {
		startDialogTimer();
	  }
	  else
	  {
		// No planets found after several searches and the timeout has expired
		if (timeoutDialogCountExpired() == true)
		{
		  JOptionPane.showMessageDialog(frame, "A CC3000 device has not been found on the network.\nPlease ensure that:\n" +
			"1) The Computer is connected to the AP to which the CC3000 is associated\n" +
			"2) The CC3000+FRAM is connected and that the CC3000+FRAM is running the server");
		  searchAttempts = 0;    
		  resetDialogTimer();
		}
	  }
	}
  
}

/*******************************************************************************************************************
 * \brief Looks for the TP-LINK Access Point
 *
 * Searches initially by trying the default IP of 192.168.1.1, but then attempts to find it
 * in each of the network interfaces
 *
 * Once the AP is found, hostName and addr are set, which are used by the rest of the application
 *
 * \param none
 * \return true if successful in finding the AP, false otherwise
 *******************************************************************************************************************/
public boolean findTPLINKAP()
{  
  InetAddress interfaceAddr = null;  
  byte[] interfaceIP = {
    0, 0, 0, 0
  };
  try
  {
    InetAddress defaultTPLINKAdddress = InetAddress.getByAddress(defaultTPLINKIP);
	if(debugLevel > 0)
		println("Default TP-LINK IP Address: " + defaultTPLINKAdddress.getHostAddress());

    if ( checkAddressIfTPLink(defaultTPLINKAdddress) == true)
    {  
		if(debugLevel > 0)
			println("checkAddressIfTPLink confirms it is TP-Link WR740");
    }

    String ssid = findTPLINKSSID(defaultTPLINKAdddress);
    if (ssid != null)
    {
		if(debugLevel > 0)
			println("Found TP-LINK in IP: " + defaultTPLINKAdddress.getHostAddress());
      APssid = ssid;  

      try
      {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        // Iterate through network interfaces and their IPs to find the interface and its
        // IP address that is used to connect to the AP
        for (NetworkInterface netint : Collections.list(nets))
        {
          Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
          while (inetAddresses.hasMoreElements () == true)
          {
			if(debugLevel > 0)
				println("Doing next element");
            interfaceAddr = (InetAddress)  inetAddresses.nextElement();

            // Check that first 3 octets are the same, indicating the interface
            // is the one used to communicate with that gateway.
            interfaceIP = interfaceAddr.getAddress();
            if (interfaceIP[0] == defaultTPLINKIP[0] && interfaceIP[1] == defaultTPLINKIP[1] && interfaceIP[2] == defaultTPLINKIP[2])
            {
              // Set the hostname and the IP address, which was obtained from the
              // interface previously selected by the user  
				if(debugLevel > 0)
					println("Found interface for IP: " + interfaceAddr.getHostAddress());

              hostName = interfaceAddr.getLocalHost().getHostName();
			  if(debugLevel > 0)
				println("hostName: " + hostName);

              addr = interfaceAddr;
			  if(debugLevel > 0)
				println("addr: " + addr);

              // println("addr is now: " + addr.getHostAddress());
            }
          }
        }
      }
      catch (SocketException se)
      {
      }
    }
    else // Couldn't find TP-LINK using default IP Address
    {     
      // Scan network interfaces to see if their gateways are TP-LINKs (in case IP was changed).
	  if(debugLevel > 0)
		println("Couln't find TP-LINK AP using default IP Address. Scanning Interfaces");
      try
      {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        // Iterate through network interfaces and their IPs to find the interface and its
        // IP address that is used to connect to the AP
        for (NetworkInterface netint : Collections.list(nets))
        {
          Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
          while (inetAddresses.hasMoreElements () == true)
          {      
			if(debugLevel > 0)
				println("Doing next element");
            interfaceAddr = (InetAddress)  inetAddresses.nextElement();

            // Check that first 3 octets are the same, indicating the interface
            // is the one used to communicate with that gateway.
            interfaceIP = interfaceAddr.getAddress();
            int i = 0;
            // Scan first two IP Addresses to see if they're TP-LINK Routers
            for (i = 0; i <= 1; i++)
            {
              interfaceIP[3] = (byte)i;
              InetAddress testInterface = InetAddress.getByAddress(interfaceIP);
			  if(debugLevel > 0)
				println("Checking: " + testInterface.getHostAddress());
              if (checkAddressIfTPLink(testInterface) == true)
              {
                // Found TP-LINK AP
                hostName = testInterface.getLocalHost().getHostName();
				if(debugLevel > 0)
					println("hostName: " + hostName);
                addr = testInterface;
				if(debugLevel > 0)
					println("addr: " + addr);	

                // Obtain and store SSID
                APssid= findTPLINKSSID(testInterface);
              }
            }
          }
        }
      }
      catch(SocketException se)
      {
      }

      return false;
    }
  }
  catch (UnknownHostException uhe)
  {
  }

  return true;
}
/*******************************************************************************************************************
 * \brief Looks for the Buffalo Access Point
 *
 * Searches initially by trying the default IP of 192.168.11.1, but then attempts to find it
 * in each of the network interfaces
 *
 * Once the AP is found, hostName and addr are set, which are used by the rest of the application
 *
 * \param none
 * \return true if successful in finding the AP, false otherwise
 *******************************************************************************************************************/
public boolean findBuffaloAP()
{  
  InetAddress interfaceAddr = null;  
  boolean my_ssidFound = false;
  byte[] interfaceIP = {
    0, 0, 0, 0
  };
  try
  {
    InetAddress defaultBuffaloAdddress = InetAddress.getByAddress(defaultBuffaloIP);
if(debugLevel > 0)
println("Default Buffalo IP Address: " + defaultBuffaloAdddress.getHostAddress());

// the buffalo configuration requires a frame browser, so may not be possible to check if this is a buffalo AP 
// comment out this code till we find other solution
//      if ( checkAddressIfBuffalo(defaultBuffaloAdddress) == true)
//        {  
//        if(debugLevel > 0)
//            println("checkAddressIfBuffalo confirms it is Buffalo");
//        }
//    }
// hardcode ssid= "buffalo"    
String ssid = "buffalo";

    if (ssid != null)
    {
      if(debugLevel > 0)
      println("Found Buffalo in IP: " + defaultBuffaloAdddress.getHostAddress());
      APssid = ssid;  

      try
      {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        // Iterate through network interfaces and their IPs to find the interface and its
        // IP address that is used to connect to the AP
        for (NetworkInterface netint : Collections.list(nets))
        {
          Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
          while (inetAddresses.hasMoreElements () == true)
          {
            interfaceAddr = (InetAddress)  inetAddresses.nextElement();
if(debugLevel > 0)
println("Doing next element: " + interfaceAddr);

            // Check that first 3 octets are the same, indicating the interface
            // is the one used to communicate with that gateway.
            interfaceIP = interfaceAddr.getAddress();
            if(debugLevel > 0)
              println("Interface IP: " +  interfaceIP [0] + "." + interfaceIP [1] + "." + interfaceIP [2] + "." + interfaceIP [3] );
            if (interfaceIP[0] == defaultBuffaloIP[0] && interfaceIP[1] == defaultBuffaloIP[1] && interfaceIP[2] == defaultBuffaloIP[2])
            {
              // Set the hostname and the IP address, which was obtained from the
              // interface previously selected by the user  
              if(debugLevel > 0)
              println("Found interface for IP: " + interfaceAddr.getHostAddress());

              hostName = interfaceAddr.getLocalHost().getHostName();
              if(debugLevel > 0)
                println("hostName: " + hostName);

              addr = interfaceAddr;
              my_ssidFound = true; 
              if(debugLevel > 0)
                println("addr: " + addr);

              // println("addr is now: " + addr.getHostAddress());
            }
          }
        }
      }
      catch (SocketException se)
      {
      }
    }
    else // Couldn't find buffalo AP using default IP Address
    {     
      // Scan network interfaces to see if their gateways are buffalos (in case IP was changed).
 if(debugLevel > 0)
println("Couln't find Buffalo AP using default IP Address. Scanning Interfaces");
      try
      {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        // Iterate through network interfaces and their IPs to find the interface and its
        // IP address that is used to connect to the AP
        for (NetworkInterface netint : Collections.list(nets))
        {
          Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
          while (inetAddresses.hasMoreElements () == true)
          {      
if(debugLevel > 0)
println("Doing next element");
            interfaceAddr = (InetAddress)  inetAddresses.nextElement();

            // Check that first 3 octets are the same, indicating the interface
            // is the one used to communicate with that gateway.
            interfaceIP = interfaceAddr.getAddress();
            int i = 0;
            // Scan first two IP Addresses to see if they're buffalo Routers
            for (i = 0; i <= 1; i++)
            {
              interfaceIP[3] = (byte)i;
              InetAddress testInterface = InetAddress.getByAddress(interfaceIP);
 if(debugLevel > 0)
println("Checking: " + testInterface.getHostAddress());
            //  commenting this code for now
          //  if (checkAddressIfTPLink(testInterface) == true)
           //   {
                // Found buffalo AP
           //     hostName = testInterface.getLocalHost().getHostName();
//if(debugLevel > 0)
//println("hostName: " + hostName);
//                addr = testInterface;
//if(debugLevel > 0)
//println("addr: " + addr);
 

                // Obtain and store SSID
//                APssid= findTPLINKSSID(testInterface);
//              }
            }
          }
        }
      }
      catch(SocketException se)
      {
      }

      return my_ssidFound ;
    }
  }
  catch (UnknownHostException uhe)
  {
  }

  return my_ssidFound;
}

/*******************************************************************************************************************
 * \brief Finds the TP-LINK AP's SSID 
 *
 * Connects to the HTTP server running on the AP and parses the data
 * from the HTML pages.
 *
 * The factory default SSID is TP-LINK_XXXXXX where the XXXXXX are the 
 * last 6 Alphanumeric MAC Address characters
 *
 * \param none
 * \return the SSID of the TP-LINK AP
 *******************************************************************************************************************/
public String findTPLINKSSID(InetAddress gatewayAddress)
{
  String apSSID = null;

  class MyAuthenticator extends Authenticator {
    private String username, password;

    public MyAuthenticator(String user, String pass) {
      username = user;
      password = pass;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
	  if(debugLevel > 0)
	  {
		  System.out.println("Requesting Host  : " + getRequestingHost());
		  System.out.println("Requesting Port  : " + getRequestingPort());
		  System.out.println("Requesting Prompt : " + getRequestingPrompt());
		  System.out.println("Requesting Protocol: "
			+ getRequestingProtocol());
		  System.out.println("Requesting Scheme : " + getRequestingScheme());
		  System.out.println("Requesting Site  : " + getRequestingSite());
	  }
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }

  int index = 0;
  String urlString = "http://" + gatewayAddress.getHostAddress() + "/userRpm/StatusRpm.htm";
  String username = "admin";
  String password = "admin";
  Authenticator.setDefault(new MyAuthenticator(username, password));

  {
    try
    {
      URL url = new URL(urlString);
      URLConnection connection = url.openConnection();
      connection.setConnectTimeout(2000);
      //connection.setReadTimeOut(2000);
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      //String line;
	  if(debugLevel > 0)
		println("=============================");
      // InputStream content = (InputStream) url.getContent();
      //BufferedReader in = new BufferedReader(new InputStreamReader(content));
      String line;      

      while ( (line = in.readLine ()) != null) {

		// Look for JSON array that has Wireless LAN Parameters
		index = line.indexOf("var wlanPara = new Array(");
        if (index != -1)
        {
            line = in.readLine ();
            line = in.readLine (); //   Third line contains the SSID
              
			apSSID = line.substring(index+1, line.length()-2);    
            			
			
          break;
        }
      }
	  if(debugLevel > 0)
		println("SSID Found: " + apSSID);
    }
    catch(IOException ioe)
    {
    }
  }

  return apSSID;
}

/*******************************************************************************************************************
 * \brief Checks whether IP Address is a TP-LINK TL-WR740N AP
 *
 * Connects to the HTTP server running on the AP and parses the data
 * from the HTML pages to verify it is a TL-WR740N.
 *
 *
 * \param none
 * \return true if AP is TL-WR740N, false otherwise
 *******************************************************************************************************************/
public boolean checkAddressIfTPLink(InetAddress gatewayAddress)
{
  String deviceName = null;
  class MyAuthenticator extends Authenticator {
    private String username, password;

    public MyAuthenticator(String user, String pass) {
      username = user;
      password = pass;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
	if(debugLevel > 0)
	{
      System.out.println("Requesting Host  : " + getRequestingHost());
      System.out.println("Requesting Port  : " + getRequestingPort());
      System.out.println("Requesting Prompt : " + getRequestingPrompt());
      System.out.println("Requesting Protocol: "   + getRequestingProtocol());
      System.out.println("Requesting Scheme : " + getRequestingScheme());
      System.out.println("Requesting Site  : " + getRequestingSite());
	}
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }

  int index = 0;
  String urlString = "http://" + gatewayAddress.getHostAddress() + "/frames/top.htm";
  String username = "admin";
  String password = "admin";
  Authenticator.setDefault(new MyAuthenticator(username, password));

  try
  {
    URL url = new URL(urlString);

    try
    {           

      URLConnection yc = url.openConnection();
      yc.setConnectTimeout(200); 
	  if(debugLevel > 0)
		println("Getting Content");

      BufferedReader in = new BufferedReader(
      new InputStreamReader(
      yc.getInputStream()));
      String line;      

      while ( (line = in.readLine ()) != null) {
        index = line.indexOf("TL-WR740N");
        if (index != -1)
        {
          return true;
        }
      }
    }
    catch(IOException ioe)
    {
    }
  }
  catch (MalformedURLException mue)
  {
  }
  return false;
}



/*******************************************************************************************************************
 * this function is called anytime the mouse clicker is released. At this time, we should release the handles!
 *******************************************************************************************************************/
public void mouseReleased() 
{
  handlesTemp[0].release();
  handlesAccel[0].release();
}

/***************************************************************************************************************  
 *  Planet class. 
 **************************************************************************************************************/
class Planet {
  float planetOffset; // this defines the distance between multiple planets/nodes. As more nodes are added, they are always equidistant from eachother.
  float planetRadius; // radius of the planet.
  int surface; // fill color of the planet
  int accelX; // holds latest accel x data
  int accelY; // holds latest accel y data
  int accelZ; // holds latest accel z data
  float[] accelXhist = {
    0, 0, 0, 0, 0
  };
  float[] accelYhist = {
    0, 0, 0, 0, 0
  };
  float[] accelZhist = {
    0.6f, 0.6f, 0.6f, 0.6f, 0.6f
  };
  int ACCEL_BUFFER_SIZE = 3;
  int temp; // holds latest temp data
  float Vcc; // holds latest Vcc
  float angle; // holds angle of the planet (position around sun)
  int lifeTime; // holds the last time this node transmitted data. Allows us to see if a node should "Timeout"
  String ip; // holds the IP address of the planet
  float ping= planetRadius*6; // ping holds the size of the accelerometer threshold alarm (ring is emitted from planet if acc threshold is exceeded)
  Socket planetSocket; // holds the socket (IP + port) information of the node.
  boolean accelAlarm;
  int alarmTime;
  // call this to update planet information or create new planet.
  Planet(float planetOffset, float planetRadius, int surface, int accelX, int accelY, int accelZ, int temp, float Vcc, float ping, int lifeTime, String ip, Socket planetSocket) {
    angle = planetOffset;
    this.planetRadius = planetRadius;
    this.surface = surface;
    this.accelX=accelX;
    this.accelY=accelY;
    this.accelZ=accelZ;
    this.temp=temp;
    this.Vcc=Vcc;
    this.ping = ping;
    this.lifeTime=lifeTime;
    this.ip=ip;
    this.planetSocket = planetSocket;
    this.accelAlarm = false;
    this.alarmTime = 0;
    int ind;
    //    for (ind = 0; ind < 5; ind++)
    //    {
    //      this.accelXhist[ind] = 64;
    //      this.accelYhist[ind] = 64;
    //      this.accelZhist[ind] = 64 + 21;      
    //    }
  }


  /***************************************************************************************************************  
   *  orbit updates the planet on the GUI. Changes the planet's postion around the sun, tilt, color, etc.
   *  This function also checks if the thresholds have been exceeded and will show the shows on the GUI
   **************************************************************************************************************/
  public void orbit(float ellipticalRadiusX, float ellipticalRadiusY, float orbitSpeed, float newAngle, String IP, int accelXi, int accelYi, int accelZi, float maxTemp, float maxAccel, float Vcc, int lifeTime) {
    float px = cos(radians(angle))*ellipticalRadiusX; // set the planet's new X location
    float py = sin(radians(angle))*ellipticalRadiusY; // set the planet's new Y location
    int test = (5000- (millis()-lifeTime) ); // help determine the transparency of the planet fill color depending on the last time it received data.
    int ind;

    //convert incoming accelerometer data to float
    float  accelX = PApplet.parseFloat (accelXi);
    float  accelY = PApplet.parseFloat (accelYi);
    float  accelZ = PApplet.parseFloat (accelZi);



    float clear = 255-(5000-test)*255/5000; // set transparency of planet fill based on time since last update. This enables it to fade away when it times out.
    if (clear>220) // delay transparency...
      clear =255;
    //Update planet's position in solar system and apply tilt
    translate(px, py); // Update planet location around orbit

    //Check to see if acceleration threshold has been exceeded. If so, set off alarm
    // shift accelerometer data [0, 128] --> [-64, 63]
    accelX -=64;
    accelY -=64;
    accelZ -=64;

    // scale accelerometer data [-64, 63] --> [-3.00, 3.00]
    accelX = accelX *3/64; 
    accelY = accelY *3/64;
    accelZ = accelZ *3/64;

    //averaging filter
    for (ind=0; ind<ACCEL_BUFFER_SIZE-1; ind++) {
      accelXhist[ind] = accelXhist[ind+1];
      accelYhist[ind] = accelYhist[ind+1];
      accelZhist[ind] = accelZhist[ind+1];
    }

    accelXhist[ACCEL_BUFFER_SIZE-1] = accelX;
    accelYhist[ACCEL_BUFFER_SIZE-1] = accelY;
    accelZhist[ACCEL_BUFFER_SIZE-1] = accelZ;    

    accelX = 0;
    accelY = 0;
    accelZ = 0;
    for (ind=0; ind<ACCEL_BUFFER_SIZE; ind++) {
      accelX += accelXhist[ind];
      accelY += accelYhist[ind];
      accelZ += accelZhist[ind];
    }
    accelX = accelX / ACCEL_BUFFER_SIZE;
    accelY = accelY / ACCEL_BUFFER_SIZE;
    accelZ = accelZ / ACCEL_BUFFER_SIZE;
    
    // get resultant vector of acceleration
    float resultAcc = sqrt ( accelX*accelX + accelY*accelY + accelZ*accelZ ); 
    float resultAccNorm = 0; //hold normalized resultant
    
    rotateX(asin(accelX/resultAcc)); //apply accelerometer X rotation/tilt 
    rotateY(-asin(accelY/resultAcc)); //apply accelerometer Y rotation/tilt 

    resultAccNorm = resultAcc/0.6f; // normalize resultant vector. We see a resultant vector of 0.6 when the board is flat. Should be 1.
    if (resultAccNorm >= maxAccel) { // accelerometer threshold is exceeded! Set off alarm on GUI. (Emit ring)
      accelAlarm = true;
      alarmTime = millis();//time of alarm
    }
    if (accelAlarm == true && (millis()-alarmTime < 900)) {// if accel alarm goes off, emit single ring for 3 seconds
      fill(0, 0, 0, 0);// transparent fill


      if (temp>=maxTemp) // check if temperature threshold has been exceeded
        stroke((temp-maxTemp)*5+110, 110-(temp-maxTemp)*5, 110-(temp-maxTemp)*5, 256-( (ping-6*planetRadius)*256)/(planetRadius*3)); // ring fades away in color over time (becomes more transparent, eventually disappears)
      else// check if temperature threshold has been exceeded
      stroke(110-(maxTemp-temp)*5, 110-(maxTemp-temp)*5, (maxTemp-temp)*5+110, 256-( (ping-6*planetRadius)*256)/(planetRadius*3));  // ring fades away in color over time (becomes more transparent, eventually disappears)

      strokeWeight(4-(ping/planetRadius-6) ); // set thickness of the ring that is emitted
      ellipse(0, 0, ping, ping); // draw in the ring
      ping=ping+1; // ring gets larger over time
      if (ping>planetRadius*12) // once ring is certain size, we reset the ring alarm size.
        ping=(int)planetRadius*6;
    }
    else {
      accelAlarm = false;
      ping =(int)planetRadius*6; //reset ring size
    }

    //Draw the planet's saturn ring. X and Y tilt has already been applied.
    noStroke(); // no stroke
    fill(110, 110, 110, clear); // the ring should have the same fill as planet (enless temp threshold has been exceeded)
    if (temp>=maxTemp) // check if temperature threshold has been exceeded
      fill((temp-maxTemp)*5+110, 110-(temp-maxTemp)*5, 110-(temp-maxTemp)*5, clear); // temperature threshold exceeded. Planet fill should be red
    else// check if temperature threshold has been exceeded
    fill(110-(maxTemp-temp)*5, 110-(maxTemp-temp)*5, (maxTemp-temp)*5+110, clear); // temperature threshold exceeded. Planet fill should be red
    ellipse(0, 0, planetRadius*6, planetRadius*6); // draw the "saturn ring"

    sphere(planetRadius*2); //draw planet/sphere in the middle of the saturn ring. 

    //Show IP address, temp, Acc & Vcc value for planet
    fill(255, 255, 255, clear); // change fill to white for text.
    textSize(30); // change font size
    text(""+IP.substring(1), 38, 35); // Display the IP address of the node being updated
    textSize(17); // change font size    
    text("- Vcc: "+Vcc/10, 38, 50); // Show latest Vcc data. The FRAM board sends voltage. The GUI just has to divide by 10.
    text("V", 106, 50);
    text("- Temp: "+ temp, 38, 70); // Temperature is already sent by the FRAM board in degrees Farenheit. Simply have to display it.
    if (temp > 99)
    {
      text("\u00b0F", 125, 70);
    }
    else
    {
      text("\u00b0F", 119, 70);
    }
    text("- Accel: "+nf(resultAccNorm, 1, 4), 38, 90); // Show latest normalized Resultant vector/acceleration     

    // revert back the translation and rotation of the drawing plane
    rotateY(asin(accelY/resultAcc)); //undo accelerometer Y rotation/tilt 
    rotateX(-asin(accelX/resultAcc)); //undo accelerometer X rotation/tilt
    translate(-px, -py);         // undo translation to the middle (where the sun is)
    angle+=orbitSpeed;          // change the planet's position around the orbit for next update.
  }
}

/***************************************************************************************************************  
 *  Handle class. This class is defined for the threshold/alarm scrollers
 **************************************************************************************************************/
class Handle
{
  int maxLength = 400; // max length of scroller
  int x, y; // x,y position of box handle, top left corner
  int boxx, boxy; // x,y position of box handle, bottom right corner
  float thresh; // holds translated threshold data into temp or acc.
  int length; //holds length of current scroller position
  int size; // size of the scrollbar handle
  boolean over; // boolean to check if mouse is over the handle
  boolean press; // boolean to check if user is pressing/clicking on handle
  boolean locked = false; 
  boolean otherslocked = false;
  Handle[] others;

  Handle(int ix, int iy, int il, int is, Handle[] o)
  {
    x = ix;
    y = iy;
    length = il;
    size = is;
    boxx = x+length - size/2;
    boxy = y - size/2;
    others = o;
  }

  /***************************************************************************************************************  
   *  Update scroller information. 
   **************************************************************************************************************/
  public void update() 
  {
    boxx = x + length;
    boxy = y - size/2;

    for (int i=0; i<others.length; i++) {
      if (others[i].locked == true) {
        otherslocked = true;
        break;
      } 
      else {
        otherslocked = false;
      }
    }

    if (otherslocked == false && handleLock==false) {
      over();
      press();
    }

    if (press) {
      length = lock(mouseX-(x+size/2), 0, 200); // for some reason mouseX is offset by 55
    }
  }

  /***************************************************************************************************************  
   *  Check to see if mouse is hovering over a scrollbar handle. 
   **************************************************************************************************************/
  public void over()
  {
    if (overRect(boxx, boxy, size, size)) {
      over = true;
    } 
    else {
      over = false;
    }
  }
  /***************************************************************************************************************  
   *  Check to see if user is clicking/pressing the scrollbar handle. 
   **************************************************************************************************************/
  public void press()
  {
    if (over && mousePressed || locked) {
      press = true;
      locked = true;
      handleLock = true;
    } 
    else {
      press = false;
    }
  }

  /***************************************************************************************************************  
   *  Check to see if user released the mouse. 
   **************************************************************************************************************/
  public void release() 
  {
    locked = false;
    handleLock = false;
  }

  /***************************************************************************************************************  
   *  Display the updated scrollbar position on the GUI. 
   *  Pass in "type" to tell which info to display. "Temp" or "acc"
   **************************************************************************************************************/
  public void display(String type) 
  {
    strokeWeight(1);
    if (type == "temp")
      thresh = length*0.4f+50; //0.2 for temp range (60-100 degrees) - translate length of scrollbar to temp
    else if (type == "accel")
      thresh = length*(.01f)+1; //.005 for accel range (1-3g) - translate length of scrollbar to acc (g's)
    fill(255); //change handle fill color to white
    stroke(128, 128, 128); //change scroll bar line to black
    line(x, y, x+200, y); // draw line with updated size info
    stroke(255, 255, 255); //change scroll bar line to black
    line(x, y, x+length, y); // draw line with updated size info
    rect(boxx, boxy, size, size); // draw box handle in new location

    textSize(15); // set text size    
    text(""+nf(thresh, 3, 3), length+x-2, y+22); // display current threshold information
    stroke(0);
    fill(0);
    if (over || press) {
      rect(boxx+2, boxy+1, 7, 8);
      //line(boxx, boxy+size, boxx+size, boxy);
    }
  }

  /***************************************************************************************************************  
   *  check to see if mouse is over the square handle bar
   *  if so, return true.
   **************************************************************************************************************/
  public boolean overRect(int x, int y, int width, int height) 
  {
    if (mouseX >= x && mouseX <= x+width && 
      mouseY >= y && mouseY <= y+height) {
      return true;
    } 
    else {
      return false;
    }
  }

  /***************************************************************************************************************  
   *  locks onto handle even if the mouse is not over the square handle anymore.  Prevents scroller from going over.
   **************************************************************************************************************/
  public int lock(int val, int minv, int maxv) 
  { 
    return  min(max(val, minv), maxv);
  }
}

/*******************************************************************************************************************
 * \brief Starts the Timeout Dialog Box's timer
 * 
 * \param none
 * \return none
 *
 *******************************************************************************************************************/
public void startDialogTimer()
{
  timeoutDialogTime = getCurrrentTime();
}

/*******************************************************************************************************************
 * \brief Restarts the Timeout Dialog Box's timer
 * 
 * \param none
 * \return none
 *
 *******************************************************************************************************************/
public void resetDialogTimer()
{
  timeoutDialogTime = getCurrrentTime();
}

/*******************************************************************************************************************
 * \brief Returns the Start time at which the timer was started
 * 
 * \param none
 * \return the timeout timer's start time
 *
 *******************************************************************************************************************/
public long dialogTimerStartTime()
{
  return timeoutDialogTime;
}

/*******************************************************************************************************************
 * \brief Gets the current time and converts to seconds
 * 
 * \param none
 * \return the current time in seconds
 *
 *******************************************************************************************************************/
public long getCurrrentTime()
{
  return ((hour()*3600)*+(minute()*60)+second());
}

/*******************************************************************************************************************
 * \brief Checks whether the timeout dialog's timer expired
 * 
 * \param none
 * \return true if it expired, false otherwise
 *
 *******************************************************************************************************************/
public boolean timeoutDialogCountExpired()
{
  if (getCurrrentTime() >= dialogTimerStartTime() + timeoutDialogTimeoutSeconds) 
    return true;
  else
    return false;
}

public class Logger
{
  FileOutputStream fout;
  String fname;

  public Logger(String lfn)
  {
    fname = lfn;

    // Open an output stream
    try
    {
      fout = new FileOutputStream (fname, true);
      // Print a line of text
      new PrintStream(fout).println ("=== Debug Log === ");
    }
    // Catches any error conditions
    catch (IOException e)
    {
      System.err.println ("Unable to write to file");
      System.exit(-1);
    }
  }

  public void Write(String st)
  {
    try
    {
      // Open an output stream
      fout = new FileOutputStream (fname, true);
      // Print a line of text
      new PrintStream(fout).println (hour() + ":" + minute() + ":" + second()+ " - "  + st);

      // Close our output stream
      fout.close();
    }
    // Catches any error conditions
    catch (IOException e)
    {
      System.err.println ("Unable to write to file");
      System.exit(-1);
    }
  }
}

public class SerialPortController
{
  Serial portList;  

  public SerialPortController()
  {	
    if (Serial.list().length == 0)
    {
      serialLog.Write("Serial Port list is empty");
	  if(debugLevel > 0)
		println("ERROR: No serial ports found!!!");
      JOptionPane.showMessageDialog(frame, "No Serial COM Ports have been found\n\n" +
        "Please ensure that the FRAM board is connected to the computer\n" +
        "and that the drivers have been installed.\n\n"+
        " Check the Device Manger to ensure that the port exists");
    }
    else
    {
    }
  }

  public int getPortCount()
  {
    return Serial.list().length;
  }

  public String getPortName(int i)
  {
    return Serial.list()[i];
  }
}


public class SerialPort
{
  Serial sPort = null; 
  String portname;
  int timeoutVal = 10; // For 3 seconds
  String readBuffer = "";
  int baudrate = 9600;

  public SerialPort(String pn )
  {	
    portname = pn;
  }

  public SerialPort(String pn, int brate)
  {	
    portname = pn;
    baudrate = brate;
  }

  public int open(PApplet pr)
  {
    try
    {	
      sPort = new Serial(pr, portname, 9600);
    }
    catch (Exception piue)
    {

      return -1;
    }
    return 0;
  }

  // Returns number of bytes redeiced
  public int sendCommand(String st)
  {
    Thread th = new Thread();
    int timeout = timeoutVal;
    // Clear old commands
    sPort.write("\r\n");  // Enter

    //sPort.clear();
    while (sPort.available () == 0 && timeout > 0)
    {      
      try
      {
        th.sleep(100);
      }
      catch (InterruptedException ie)
      {
      }
      timeout--;
    }
    if (timeout == 0)
    {
      serialLog.Write(portname + " Port answer timed out");
      // Timeout condition
      sPort.stop();
      return 0;
    }
    else
    {
      serialLog.Write(portname + " Received " + sPort.available() + " bytes");
	  if(debugLevel > 0)
		println(sPort.available() + " bytes available");

      // Delay

      String inBuffer = sPort.readString();
	  if(debugLevel > 0)
		println("Received: " + inBuffer);
      sPort.clear();
      if (inBuffer != null)
      {
        if (inBuffer.indexOf('>') != -1)
        {
          // The FRAM board responded and we will issue a command to program it
		  if(debugLevel > 0)
			println("Sending: " + st);
          serialLog.Write(portname + " Sending: " + st);
          sPort.write(st + "\r\n");
          timeout = timeoutVal; // For 3 seconds
          while (sPort.available () < 2 && timeout > 0)
          {
            try
            {
              th.sleep(100);
            }
            catch (InterruptedException ie)
            {
            }
            timeout--;
          }
          if (timeout == 0)
          {
            // Timeout condition
            serialLog.Write(portname + " Port answer timed out");
            sPort.stop();
            return 0;
          }
          else
          {
			if(debugLevel > 0)
				println(sPort.available() + " bytes available");
            serialLog.Write(portname + " Received " + sPort.available() + " bytes");
            readBuffer = sPort.readString();
			if(debugLevel > 0)
				println("Received: " + readBuffer);
            return readBuffer.length();
          }
        }
      }
      return 0;
    }
  }
  public String getReadBuffer()
  {
    return readBuffer;
  }
  
   // Returns -1 if no string received, 1 otherwise
  public int waitForString(String st, int sec)
  {
	// Check buffer
	if(readBuffer.indexOf(st) != -1)
	{
		// String was received
		return 0;
	}
	Thread th = new Thread();	
	int timeout = sec*10;
	
	int res = -1;
	
	if(sPort.available() > 0)
		res = sPort.readString().indexOf(st);
	while (res == -1 && timeout > 0)
	{
		try
		{
		  th.sleep(100);
		}
		catch (InterruptedException ie)
		{
		}
		timeout--;
		if(sPort.available() > 0)
			res = sPort.readString().indexOf(st);
	}
	if(res != -1)
	{
	    // String was received
		return 0;
	}
	else
	{
		// Timeout
		return -1;
	}
  }
  
  // Closes COM Port
  public void close()
  {
	sPort.stop();  
  }
}

public boolean checkIfJREVersionCorrect()
{
	int index = -1;
	String verStr = System.getProperty("java.version");
	index = verStr.indexOf("1.6");
	if (index != -1)
	{
		return true;
	}
	else
	{
		return false;
	}
}
public void printJREData()
{
	if(debugLevel > 0)
	{
		println(System.getProperty("os.arch"));
		println( System.getProperty("java.vendor"));
		println(System.getProperty("java.version"));
		println(System.getProperty("java.class.path"));
		println(System.getProperty("java.ext.dirs"));
		println(System.getProperty("java.home"));
	}
	
  infLog.Write(System.getProperty("os.arch"));
  infLog.Write( System.getProperty("java.vendor"));
   infLog.Write(System.getProperty("java.version"));
    infLog.Write(System.getProperty("java.class.path"));
   
   infLog.Write(System.getProperty("java.ext.dirs"));
    infLog.Write(System.getProperty("java.home"));
}


 public static String removeChar(String s, char c) {
    String r = "";
    for (int i = 0; i < s.length(); i ++) {
       if (s.charAt(i) != c) r += s.charAt(i);
       }
    return r;
    }

	
	
// Wrapper class for Windows ping
class AddressPing
{
	String addr;
	final int attempts = 1;
	// Construtor for individual octets
	public AddressPing(int oct1, int oct2, int oct3, int oct4)
	{		
		byte[] bAddr;
		bAddr = new byte[4];
		
		bAddr[0] = (byte)oct1;
		bAddr[1] = (byte)oct2;
		bAddr[2] = (byte)oct3;
		bAddr[3] = (byte)oct4;
		
		InetAddress  inetAddr = null;
		
		try 
		{
			inetAddr = InetAddress.getByAddress("", bAddr);
		}
		catch (UnknownHostException unknownHost) 
		{         
		  System.out.println("Unknown Host Exception " + inetAddr.getHostAddress() + "\n" + unknownHost.getMessage());		 		 
		}
		addr = inetAddr.getHostAddress();
	}
	
	// Constructor for string
	public AddressPing(String ip)
	{
		if(ip != null)
			addr = ip;
	}
	
	// Pings the address and lets us know whether 
	public boolean ping()
	{
		if(addr != null)
		{
			String pingCmd = "ping " + addr + " -n " + attempts + " -w 100";

			try 
			{
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(pingCmd);

				BufferedReader in = new BufferedReader(new
				InputStreamReader(p.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) 
				{
					// Look at whether we were successful
					if(debugLevel > 0)
						System.out.println(inputLine);
					
					// Check to see if successful
					// Ping says 100% if all packets failed, or
					// (0% if all packets successful and host is up
					if(inputLine.indexOf("(0%") != -1)
						return true;
				}
				in.close();
			}
			catch (IOException e) 
			{
				System.out.println(e);
			}
			return false;
		}
		else
		{
			return false;
		}		
	}	
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#F0F0F0", "CC3000SensorAppGUI" });
  }
}

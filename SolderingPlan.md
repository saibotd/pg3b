# Introduction #

The following diagrams show the steps required to assemble a PG3B Prototype II circuit board. The parts are listed in the PG3B Prototype II : <a href='http://code.google.com/p/pg3b/wiki/BillOfMaterials'>Bill Of Materials</a>.

# Details #

There are four parts to the assembly instructions. Part 1 lists the steps, 1 - 8, required to solder the circuit for XBox 360 analog controls. Part 2 shows steps 9 - 24. For a wired controller these components convert the 5.0V from the USB port to the 1.8V required by the digital buttons. The 2.7K common bus resistor network is not required by the Wireless controller and can be left unpopulated. Part 3 shows the Teensy++ module which is soldered to the bottom of the PG3B circuit. Finally, Part 4 shows the soldering and test plan for wiring the XBox 360 Wired and Wireless controllers.

This part of the circuit is used by PG3B to manipulate Left and Right Thumbsticks, and Left and Right Triggers.

![http://pg3b.googlecode.com/svn/wiki/SolderingPlanPart1of4.jpg](http://pg3b.googlecode.com/svn/wiki/SolderingPlanPart1of4.jpg)

Working from left to right solder the eight resistors, the socketed resistor network, and the last seven resistors. The resistor network and the socket are not required for the Wireless controller.

![http://pg3b.googlecode.com/svn/wiki/SolderingPlanPart2of4.jpg](http://pg3b.googlecode.com/svn/wiki/SolderingPlanPart2of4.jpg)

Having followed the soldering plan Part I and Part II, the bottom of the board should look like this:

![http://pg3b.googlecode.com/svn/wiki/Prototype2Bottom.jpg](http://pg3b.googlecode.com/svn/wiki/Prototype2Bottom.jpg)

Notice that the solder joints are shiny and conical. This is how your solder joints should appear. If any are dull then re-heat the joint. Add a bit of flux and a very small amount of solder.

![http://pg3b.googlecode.com/svn/wiki/Prototype2Top.jpg](http://pg3b.googlecode.com/svn/wiki/Prototype2Top.jpg)

Once the mainboard is finished, it's time to solder the Teensy onto the other side.
The pins are labeled for you.  Gently insert the Teensy++ MCU, and solder the legs up.

Now all that's left is the connection from the output control pins on the mainboard to
the corresponding points on the Controller PCB.  You will need a good reference of
your controller PCB, here are two links:

http://forums.xbox-scene.com/index.php?showtopic=471958
(scroll down to Controller Modifications and look at the High Res images provided by RDC)

http://www.slagcoin.com/joystick/pcb_wiring.html#PCB_DIAGRAMS

![http://img23.imageshack.us/img23/3069/web2e.png](http://img23.imageshack.us/img23/3069/web2e.png)

![http://img714.imageshack.us/img714/3098/web1e.png](http://img714.imageshack.us/img714/3098/web1e.png)


In Progress ....
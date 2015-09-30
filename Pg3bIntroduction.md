# Introduction #

PG3B is a hardware circuit and software modules that provide everything from low level XBox 360 control to sophisticated applications that integrate and enhance your favorite gaming devices including Arcade, Mouse, Keyboard, and Joystick peripherals.

# Details #

### SOFTWARE ###

Application modules are assembled like building blocks to make the perfect controller for your game. The system is completely open and ready to meet your needs. Example applications under development include XBoxMouse, XBoxKeyboard, XBoxRapidFire, and Machine Vision Targeting. These are standalone applications written in Java or C#.

<img width='600' align='middle' alt='PG3B Development Environment' src='http://i711.photobucket.com/albums/ww111/rjburke377/IRobot/PG3BDevelopmentSoftware-1.png' />

The Driver layer is similar to the Microsoft XNA 3.0 Game Studio API. Unlike XNA the values are writable because they send GamePad instructions to the XBox 360 game console. The Driver layer is available for Java and C# development environments.

The Physical layer establishes a communications channel with the PG3B circuit board. This module is managed by the driver layer so you don't need to worry about low level communications details. The Physical layer is available for Java and C# development environments.

The PG3B software has three distinct layers. The lowest layer is implemented within the XBoxConnection Class. It establishes the USB communications device class (CDC) connection to the PG3B hardware. Applications typically don’t do anything directly with this class. The XBoxController Class in the driver layer takes the port name from the application then establishes and manages the XBoxConnection.

The Driver Layer has a series of classes for setting button, trigger, and thumbstick states. If you are familiar with Microsoft XNA 3.0 then the classes in the PG3B driver will already be familiar to you. Where XNA has GamePadButtons Class the PG3B development environment has an XBoxButtonsClass. The major difference is that the Microsoft interface is intended for read-only applications and PG3B is intended for complete control. When writing a button’s state, the driver layer collaborates with the classes in the physical layer.

Here’s an application example of “Pressing” the A Button using PG3B, then using the XNA studio to monitor the button state.

Normally the GamePad USB Port is connected to your XBox 360. During development you can connect it to your PC for testing purposes. In this scenario your PC has two USB connections: one to the PG3B USB port, and the second to the GamePad USB port.

```
{
  gamepadController = new PG3B.Interface.GamepadController(playerIndex);
  xboxController = new PG3B.Interface.XBoxController(portName);
 
  xboxController.Buttons.A = XBoxButtonState.Pressed;
  Console.Writeline("Button A is now being pressed.")
 
  while (gamepadController.Buttons.A == ButtonState.Released)
    ;
  Console.Writeline("Button is reported 'Pressed' by XNA.")
}
```

This example shows squeezing the Left Trigger rapidly ten times in a row.

```
{
  xboxController = new PG3B.Interface.XBoxController(portName);
 
  for ( i = 0; i < 10; i++)
  {
    xboxController.Triggers.Left = 1.0;
    xboxController.Triggers.Left = 0.0;
  }
}
```

Controlling the XBox 360 from your computer is really easy. All of the development tools are free including the development environment. Microsoft, for example, allows you to download and use Visual Studio 2008 and XNA 3.0 at no cost. The PG3B development platform is open source which means you can freely access the hardware and software for your needs.

### HARDWARE ###

PG3B has a surprisingly small number of functional blocks but it allows full control of XBox 360 Wired and Wireless controllers. The core of the system is a complete USB-based micro-controller development system. It has a lot of power packed into a very small footprint! All programming is done via the USB port. No special programmer is needed, only a standard "Mini-B" USB cable and a PC  with a USB port.

<img width='600' align='middle' alt='PG3B Prototype II' src='http://i711.photobucket.com/albums/ww111/rjburke377/IRobot/PG3BPrototypeII-1.png' />

The PG3B circuit has an AT90USB646 AVR micro-controller with 64512 bytes of flash memory for your code, 4096 bytes of RAM for program variables, and 2048 bytes of EEPROM to store configuration data. This is small compared to the resources available to a PC but plenty to run the USB port and drivers for up to sixteen input devices.

> The following paragraphs describe PG3B support for the XBox 360 Wired Common Line controller architecture. Wireless controller architectures are supported too. The description for Wireless is similar but has a few minor differences that are not described in the following text.

The core provides 5v from the USB connector to the rest of the circuit. This means the Digital Output from the core is 0v for Off and 5V for On. A voltage divider translates this into the voltage levels required by the XBox 360 Wired Controller; button pressed is translated into 1.8v and button released is 0v. The PG3B circuit has a digital connection to each of the XBox 360 buttons.

There's a PG3B XBox360 Driver running on the core that allows an application to select a button and to make the button state pressed, or released. When a button is pressed the Digital Output for that button is set to "high", so the voltage is raised to 5v. The voltage divider converts the level to 1.8v, as expected by the controller, which causes the XBox 360 controller to think that the physical button is pressed.

Similarly, when a button is released, the Digital Output for that button is set to "low". The voltage for that button is lowered to 0v. The voltage divider passes the 0v signal to the XBox 360 controller which now thinks the physical button is released. The PG3B XBox 360 driver allows control of all fifteen buttons: A, B, X, Y, Up, Down, Left, Right, Left Stick, Right Stick, Left Bumper, Right Bumper, Back, Guide, Stop.

The core is also attached to a Serial Peripheral Interface (SPI) bus and a series of 10K Ohm Digital Potentiometers (POTs). The POTs allow the application software full control of the XBox 360 thumbsticks and triggers. Thumbsticks have an X and Y axis. Each axis has a range of motion. Triggers also have a range of motion. These devices are controlled with a value between 0 and 255. The value is translated into a voltage from 0v to 1.6v and is divided into 256 steps.

Its easier to think about the range of motion by skewing the values. For the thumbsticks, you can think of the value as -127 to +128 with 0 as idle. Triggers are usually operated with values from 0 to 128 where 0 is not pressed and 128 is fully pressed.

The PG3B XBox360 Driver running on the core allows an application to select a thumbstick or trigger and set its value. The value is transmitted over the SPI Bus to a Digital POT. The Digital POT translates the value into a voltage between 0v to 1.6v. The voltage causes the XBox 360 controller to think that the thumbstick has been deflected left / right on the X axis, or up / down on the Y axis. The amount of deflection is proportional to the value from -127 to +128 with 0 as idle or "no deflection". Triggers are pulled proportional to the value from 0 to 128 where 0 is not pulled.

The PG3B XBox 360 driver allows control of all six analog controls including Left Stick X & Y, Right Stick X & Y, Left Trigger, and Right Trigger.
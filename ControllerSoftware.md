![http://pg3b.googlecode.com/svn/wiki/controller/controller.jpg](http://pg3b.googlecode.com/svn/wiki/controller/controller.jpg)

# Controller #

Controller is a desktop software application. It can monitor input devices such as a keyboard, mouse, or joystick and manipulate the PG3B, XIM1, or XIM2 (aka XIM360) accordingly. This allows nearly any PC peripheral to be used with an Xbox 360. It also has built-in support for scripting

### Supported Devices ###

Controller supports both the PG3B and [XIM](http://xim360.com) hardware. The PG3B is an open source, low cost hardware solution for manipulating an Xbox 360 controller. The XIM360 is a closed source, commercial product that also manipulates an Xbox 360 controller. Because multiple devices are supported, throughout this documentation the term "device" will refer to either the PG3B, XIM1, or XIM2.

## Configs ##

The `Configuration` tab shows a list of configs on the left and a table of triggers on the right. When activated, a config monitors its triggers and executes the triggers' actions as needed.

_Tip: Right click a config on the left to export the config file and any script files it uses into a zip that you can share with others._

_Tip: Right click a trigger to jump to the script it uses, if any._

### Triggers ###

The `New` button under the triggers table will display the new trigger screen:

![http://pg3b.googlecode.com/svn/wiki/controller/newtrigger.jpg](http://pg3b.googlecode.com/svn/wiki/controller/newtrigger.jpg)

Click in the box labeled `Trigger`, then press a key on your keyboard, joystick, mouse, or move the mouse or a joystick axis.

### Trigger Deadzone ###

If the trigger input you have chosen is an axis, a `Deadzone` button will appear. This will allow the trigger to ignore values close to zero. A deadzone should be used when the input device is a joystick that does not return exactly to zero when the joystick is released.

### Actions ###

On the new trigger screen, you may choose what type of action to take in response to the trigger.

### Device Action ###

A device action tells the PG3B or XIM to manipulate the Xbox 360 controller. Eg, if your trigger is a keyboard key and you have chosen a device action set to the A button, then when you press the keyboard key, the A button will be pressed. When you release your key, the A button will be released.

_Tip: To easily set the target for a device action, click a button on the controller image at the top of the application. You can also drag the sticks to set the target to an axis._

### Script Action ###

This type of action runs a script. The script can do just about anything you like, such as press a button rapidly, change mouse translation settings, or activate a different config. See ControllerScripting for more on scripting.

### Mouse Action ###

This type of action changes the mouse translation settings. Eg, if the input is a button, then when the button is down, the specified mouse settings will be used. When the button is released, the old mouse settings will be restored. This is commonly used to increase mouse sensitivity when in an "aim down sights" mode for a game.

## Mouse Translation ##

On the 'Configuration' tab, under the triggers table, you will find the `Mouse` button. This button opens a dialog and allows you to customize how the mouse movement will be translated into a thumbstick deflection. Currently, only the XIM mouse translation algorithms are supported. See [this page](http://xim360.com/community/index.php?topic=1069.0), about halfway down, for more information on the individual settings. If the XIM1 software is installed, the XIM1 mouse translation can be used. If the XIM1 software is installed, the XIM2 mouse translation can be used. This is true even if the connected device is a PG3B.

## Deadzones ##

Next to the `Mouse` button is the `Deadzones` button. This configures _device deadzones_ and should not be confused with the _trigger deadzones_ explained above. This button opens a dialog and allows you to configure the deadzone compensation for a particular game. To determine the deadzone for a game, slowly increase the deadzone until you see the game respond, then go back one step.

The device deadzone, when configured properly, effectively removes the deadzone from the game. This is essential to allow small mouse movements to be translated properly into thumbstick deflections.

## Targets ##

Next to the `Deadzones` button is the `Targets` button. This button opens a dialog that allows you to rename the Xbox controller's buttons and axes. You will see these names throughout the application. It is much more intuitive to map the spacebar key to "Throw Grenade" than it is to "Y button". Also, you may use the alternate names in scripts.

## Activate ##

When your config is ready, pressing the `Activate` button will cause the application to monitor the triggers and execute the actions as needed.

If any trigger uses keyboard or mouse input, then the user interface will be disabled until ctrl+F4 is pressed. This can be disabled on the `View` menu.

_Tip: You can click the statusbar at the bottom of the application to activate the currently selected config._

## Logs ##

The `Show Log` item on the `View` menu allows you to see debug and error messages while the application is running:

![http://pg3b.googlecode.com/svn/wiki/controller/log.jpg](http://pg3b.googlecode.com/svn/wiki/controller/log.jpg)

The logging level can be changed to received more detailed information. Generally the `Info` level should be used. The `Debug` and especially `Trace` levels output an enormous amount of information and are typically only useful to track down problems.

## Diagnostics ##

The `Diagnostics` menu items are disabled until both a device and controller are connected. The device and the same controller in which the device is installed should be plugged into to your computer and connected using the `Device` menu. The `Round Trip` menu item uses the device to set each button and axis on the controller, then reads from the state of the controller to ensure the correct button or axis was set. This can help determine if the device is wired correctly to the controller.

_Tip: You can click the statusbar at the bottom of the application to connect a device or controller._

![http://pg3b.googlecode.com/svn/wiki/controller/roundtrip.jpg](http://pg3b.googlecode.com/svn/wiki/controller/roundtrip.jpg)
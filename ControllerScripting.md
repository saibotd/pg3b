![http://pg3b.googlecode.com/svn/wiki/controller/scripting.jpg](http://pg3b.googlecode.com/svn/wiki/controller/scripting.jpg)

# Scripting #

The Controller application provides built-in support for scripting the PG3B and XIM devices. The `Scripts` tab provides a script editor with syntax highlighting, code completion, and both compile time and runtime error highlighting.

_Tip: Press ctrl+space for code completion and ctrl+enter to quickly execute the current script._

_Tip: Right click a script to jump to any config that uses that script._

## The Language ##

The scripting language used is called Pnuts. It is a JVM language, so it has access to all of Java in addition to enhanced expressive power. The following links may prove useful:

  * [Quick language highlights](http://pnuts.org/articles/pnutsHighlights.html)
  * [Official documentation](http://pnuts.org/1.2.1/snapshot/20070724/doc/)

Pnuts syntax is straightforward and can be similar to Java if you choose. Semicolons are optional. Functions are declared with syntax similar to JavaScript. One unique feature Pnuts has is that curly braces are optional for functions containing a single statement.

## Lifecycle ##

There are four important parts to a script's lifecycle: initialize, activate, continuous, and deactivate.

### Initialize ###

When a config is activated, each associated script is executed. This gives each script a chance to do any initialization needed and also to declare functions.

### Activate ###

A trigger is considered active when its payload is non-zero. When this happens, a function named `activate` is called in the script, if it exists. This is a good place to put code you want to run when a button is pressed.

### Continuous ###

After `activate`, a function named `continuous` is called in the script, if it exists. This function is called repeatedly until the trigger becomes inactive. This function is typically called extremely rapidly, so is not a good place to output any logging.

### Deactivate ###

Once a trigger's payload becomes zero, the `continuous` function is called one last time with a payload of zero, then a function named `deactivate` is called in the script, if it exists. This is a good place to put code you want to run when a button is released.

## Payload ##

Triggers provide their payload to scripts as a float. For a button, 0 means not pressed and 1 means pressed. For an axis, 0 means centered, -1 means left or up, and 1 means right or down. For a mouse axis, the payload is the number of pixels the mouse has moved.

## Predefined Variables ##

The framework exposes some variables to scripts:

  * `payload` is the float value from the trigger.
  * `action` is the action that executed the script.
  * `trigger` is the trigger that caused the action to be executed.
  * `config` is the active config.
  * `device` is the connected PG3B or XIM, or null.
  * `controller` is the connected Xbox controller, or null.
  * `ui` is the UI instance that provides access to the application's UI.

## Predefined Functions ##

By default, the `pnuts.lib` module is used, which makes available [a large number of functions](http://pnuts.org/1.2.1/snapshot/20070724/modules/pnuts.lib/doc/index.html). The following are a few of the most commonly used:

  * `eval` executes a string as script.
  * `throw` throws a string as an exception. An uncaught exception will deactivate the active config.
  * `println` prints a line to `System.out`.
  * `getFile` creates a new `java.io.File` object.
  * `list`, `map`, `set` create a new List, Map, or Set.

In addition, there are many utility functions that make common tasks easier:

  * `sleep` waits for a number of milliseconds.
  * `play` plays a tone.
  * `beep` plays a high or low tone. Useful for knowing if toggled setting is on or off.
  * `interval` returns true only if X milliseconds have passed since it last returned true. Useful to execute code every X milliseconds without using a thread.
  * `set` sets a global value. Useful to store a value between script executions or even between scripts.
  * `get` gets a global value.
  * `getConfig` looks up a config by name.
  * `setConfig` sets the currently active config.
  * `toggle` toggles a global variable. More convenient than calling `get` and `set`.
  * `isAltDown`, `isCtrlDown`, `isShiftDown` return true if alt, ctrl, or shift are pressed.
  * `fork` executes a function in a new thread. A thread pool is used, so don't worry about efficiency.
  * `millis` returns the current time in milliseconds.
  * `nanos` returns the value in nanoseconds of the most precise available system timer.

## Threading ##

When activated, a config monitors its triggers using a single thread. If a script blocks this thread, no other triggers can be checked. If you want other triggers to be checked while your script executes, use the `fork` function to execute code in a new thread. The following is an example that implements "autofire" (also known as "turbo") using a separate thread:

```
function activate () {
	set("autofire", true)
	fork(function () {
		while (get("autofire")) {
			device.set("x", true)
			sleep(60)
			device.set("x", false)
			sleep(60)
		}
	})
}

function deactivate () set("autofire", false)
```

This code runs a loop on a new thread until a global variable named `autofire` is set to false. Becuase the sleep intervals in this example are so simple, it could easily be rewritten with the `interval` function to use the config thread:

```
function continuous () {
	if (interval("autofire", 60))
		device.set("x", !device.get("x"))
}

function deactivate () device.set("x", false)
```

Some scripts have complex sleep intervals and cannot easily be rewritten in this way. Often this is true for scripts generated with the `Record` button. However, for these scripts you usually want to block the config thread anyway, to avoid input from other triggers. For example, this script presses down, down and forward, then forward and punch to throw a fireball in Street Fighter:

```
function activate () {
	device.set("down", true)
	sleep(60)
	device.set("right", true)
	sleep(60)
	device.set("down", false)
	sleep(60)
	device.set("y", true)
	sleep(60)
	device.set("y", false)
	device.set("right", false)
}
```
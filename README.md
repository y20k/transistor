README
======

Transistor - Radio app for Android
----------------------------------

**Version 0.9.x ("Young Americans")**

Transistor is a bare bones app for listening to radio programs over the internet. The app stores stations as files on your device's external storage. It currently only understands streams embedded within m3u links. Support for m3u8 and pls is coming to a future release.

Transistor is free software. It is published under the [MIT open source license]("https://opensource.org/licenses/MIT">https://opensource.org/licenses/MIT). 

How to use Transistor
---------------------
### How to add a new radio station?
The easiest way to add a new station is to search for m3u streaming links and then choose Transistor as a your default handler for m3u. You can also tap the (+) symbol in the top bar and paste in m3u links directly.

### How to play back a radio station?
Tap the big Play button ;).

### How to stop playback?
Tap the big Stop button or unplug your headphones or swipe off the notification from the lockscreen.

### How to rename or delete a station?
The rename and delete options can be accessed both from the list of stations and from the now playing screen. Just tap on the three dots symbol. You can manage the list of stations also from a file browser (see next question).

### Where does Transistor store its stations?
Transistor does not save its list of stations in a database. Instead it stores stations as m3u files on your device's external storage. Feel free to tinker with those files using the texteditor of your choice. The files are stored in /Android/data/org.y20k.transistor/Collection.

### Why does Transistor not have any setting?
There is nothing to be set ;). Transistor is a very simple app. Depending on your point of view "simple" is either great or lame.

Which Permissions does Transistor need?
---------------------------------------
### Permission "INTERNET"
Transistor streams radio stations over the internet.

### Permission "READ\_EXTERNAL\_STORAGE" and "WRITE\_EXTERNAL\_STORAGE"
Transistor reads its list of stations from (and writes it to) external storage.

### Permission "READ\_PHONE\_STATE"
Transistor stops playback when the device receives a phone call or when the user initiates a call.
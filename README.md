README
======

Transistor - Radio app for Android
----------------------------------

**Version 2.1.x ("Moonage Daydream")**

Transistor is a bare bones app for listening to radio programs over the internet. The app stores stations as files on your device's external storage. It currently understands streams encoded in MP3 and OGG.

Important note: This is an app of type BYOS ("bring your own station"). It does not feature any kind of built-in search option. You will have to manually add radio stations.

Transistor is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/transistor/blob/master/CONTRIBUTE.md) first.

Install Transistor
------------------
[<img src="https://play.google.com/intl/de_de/badges/images/generic/en_badge_web_generic.png" width="192">](https://play.google.com/store/apps/details?id=org.y20k.transistor)

[<img src="https://cloud.githubusercontent.com/assets/9103935/14702535/45f6326a-07ab-11e6-9256-469c1dd51c22.png" width="192">](https://f-droid.org/repository/browse/?fdid=org.y20k.transistor)

[... or get a Release APK here on GitHub](https://github.com/y20k/transistor/releases)

How to use Transistor
---------------------
### How to add a new radio station?
The easiest way to add a new station is to search for streaming links and then choose Transistor as a your default handler for those file types. You can also tap the (+) symbol in the top bar and paste in streaming links directly. Please note: Transistor does not feature any kind of built-in search option.

### How to play back a radio station?
Tap the big Play button ;).

### How to stop playback?
Tap the big Stop button or unplug your headphones or swipe off the notification from the lock screen.

### How to start the sleep timer?
Tapping the Clock symbol starts a 15 minute countdown after which Transistor stops playback. An additional tap adds 15 minutes to the clock. Playback must be running to be able to activate the sleep timer. 

### How to place a station shortcut on the Home screen?
The option to place a shortcut for a station on the Home screen can be accessed from the station's three dots menu. A tap on a shortcut will open Transistor - playback will start immediately.

### How to rename or delete a station?
The rename and delete options can be accessed both from the station's context menu. Just tap on the three dots symbol. You can manage the list of stations also from a file browser (see next question).

### Where does Transistor store its stations?
Transistor does not save its list of stations in a database. Instead it stores stations as m3u files on your device's external storage. Feel free to tinker with those files using the text editor of your choice. The files are stored in /Android/data/org.y20k.transistor/files/Collection.

### How do I backup and transfer my radio stations?
Transistor supports Android 6's [Auto Backup](http://developer.android.com/about/versions/marshmallow/android-6.0.html#backup) feature. Radio stations are always backed up to your Google account and will be restored at reinstall. On devices running on older versions of Android you must manually save and restore the "Collection" folder.

### Why does Transistor not have any setting?
There is nothing to be set ;). Transistor is a very simple app. Depending on your point of view "simple" is either great or lame.

Which Permissions does Transistor need?
---------------------------------------
### Permission "INSTALL_SHORTCUT" and "UNINSTALL_SHORTCUT"
This permission is needed to install and uninstall radio station shortcuts on the Android Home screen.

### Permission "INTERNET"
Transistor streams radio stations over the internet.

### Permission "READ_EXTERNAL_STORAGE"
Transistor needs access to images, photos and documents to be able customize radio station icons and to able to open locally saved playlist files.
            
### Permission "VIBRATE"
Tapping and holding a radio station will toggle a tiny vibration.

### Permission "WAKE_LOCK"
During Playback Transistor acquires a so called partial wake lock. That prevents the Android system to stop playback for power saving reasons.

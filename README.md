README
======

Transistor - Radio app for Android
----------------------------------

**Version 1.2.x ("Cygnet Committee")**

Transistor is a bare bones app for listening to radio programs over the internet. The app stores stations as files on your device's external storage. It currently understands streams encoded in MP3 and OGG.

Important note: This is an app of type BYOS ("bring your own station"). It does not feature any kind of built-in search option. You will have to manually add radio stations.

Transistor is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/transistor/blob/master/CONTRIBUTE.md) first.

Install Transistor
------------------
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" width="172">](https://play.google.com/store/apps/details?id=org.y20k.transistor)

[<img src="https://upload.wikimedia.org/wikipedia/commons/0/0d/Get_it_on_F-Droid.svg" width="172">](https://f-droid.org/repository/browse/?fdid=org.y20k.transistor)

[... or get a Release APK here on GitHub](https://github.com/y20k/transistor/releases)

How to use Transistor
---------------------
### How to add a new radio station?
The easiest way to add a new station is to search for streaming links and then choose Transistor as a your default handler for those file types. You can also tap the (+) symbol in the top bar and paste in streaming links directly. Please note: Transistor does not feature any kind of built-in search option.

### How to play back a radio station?
Tap the big Play button ;).

### How to stop playback?
Tap the big Stop button or unplug your headphones or swipe off the notification from the lock screen.

### How to rename or delete a station?
The rename and delete options can be accessed both from the list of stations and from the now playing screen. Just tap on the three dots symbol. You can manage the list of stations also from a file browser (see next question).

### Where does Transistor store its stations?
Transistor does not save its list of stations in a database. Instead it stores stations as m3u files on your device's external storage. Feel free to tinker with those files using the text editor of your choice. The files are stored in /Android/data/org.y20k.transistor/Collection.

### How do I backup and transfer my radio stations?
Transistor supports Android 6's [Auto Backup](http://developer.android.com/about/versions/marshmallow/android-6.0.html#backup) feature. Radio stations are always backed up to your Google account and will be restored at reinstall. On devices running on older versions of Android you must manually save and restore the "Collection" folder.

### Why does Transistor not have any setting?
There is nothing to be set ;). Transistor is a very simple app. Depending on your point of view "simple" is either great or lame.

Which Permissions does Transistor need?
---------------------------------------
### Permission "INTERNET"
Transistor streams radio stations over the internet.

### Permission "READ_EXTERNAL_STORAGE"
Transistor needs access to images, photos and documents to be able customize radio station icons and to able to open locally saved playlist files.
            
### Permission "VIBRATE"
Tapping and holding a radio station will toggle a tiny vibration.

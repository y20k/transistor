README
======

Open-Transistor - Radio App for Android
----------------------------------

**Version 3.1.1 (Beta) **

Transistor is a bare bones app for listening to radio programs over the internet. The app stores stations as files on your device's external storage. It currently understands streams encoded in MP3 and OGG.

Important note: This is an app of type BYOS ("bring your own station"). It does not feature any kind of built-in search option. You will have to manually add radio stations.

Transistor is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/malah-code/transistor/blob/master/CONTRIBUTE.md) first.

This open source application based on (y20k/transistor) "https://github.com/y20k/transistor".

What's new in Open-Transistor
-----------------------------
 - Use DB with SQLITE as main storage of the application. this will reduce the dependancy of files, and open application for features that we will have with SQLITE that we can add it to the application later easily, like Sorting,Filer,Grouping,relation with other tables like categories/rating/favourits, add easily add more station metadata anytime, and many more.
- Adding more metadata to stations (currently no way to change all metadata throw UI, we can only import xml with all metadata of channel, we may add editing later in next versions).
- new station rating modules (currently it's local rating only, not sync to central rating, may be later in next versions)
- Using Facebook fresco image viewer (SimpleDraweeView) with it's great loading features
- Apply Material design components and transitions
- Integrate with Firebase compoenents (Authentication , Analytics)
- Add login by Google account function (currently nothing happend after login, but in the next versions we can connect user by ID and save his data to cloud for example, and many more.)
	

Install Open-Transistor
------------------
Download the latest beta from HockeyApp :

[<img src="https://transistor-open.firebaseapp.com/assets/hockeyapp.jpg" width="192">](https://rink.hockeyapp.net/apps/2b8994e7a60b485dad0a5507ceb05c01)

[<img alt="Chart?cht=qr&amp;chl=https%3a%2f%2frink.hockeyapp" src="https://chart.googleapis.com/chart?cht=qr&amp;chl=https%3A%2F%2Frink.hockeyapp.net%2Fapps%2F2b8994e7a60b485dad0a5507ceb05c01&amp;chs=192x192">](https://rink.hockeyapp.net/apps/2b8994e7a60b485dad0a5507ceb05c01)

How to use Open-Transistor
---------------------
Vew a short video on how to use Transistor on [Vimeo](https://vimeo.com/215778690).
	[![IMAGE ALT TEXT HERE](https://i.vimeocdn.com/video/632580200_640.webp)](https://vimeo.com/215778690#)
### How to add a new radio station?
The easiest way to add a new station is to search for streaming links and then choose Transistor as a your default handler for those file types. You can also tap the (+) symbol in the top bar and paste in streaming links directly. Please note: Transistor does not feature any kind of built-in search option.
Also, it's new in that version , you are able to add bulk of stations with it's metadata from XML file, file format can be as below sample 

https://transistor-open.firebaseapp.com/radios.xml

Xml file should contains nelow metadata :
* unique_id :Station  UNIQUE ID: used to identify station in DB, and should be UNIQUE and it can be string value
* title : Station Title
* subtitle : Station Sub Title
* image : (IMAGE_PATH) This is image link (this should be external http link or in storage file link to image)
* small_image_URL : (SMALL_IMAGE_PATH) : This is small image link (icon) (this should be external http link or in storage file link to image)
* uri : (StreamURI): Station Stream URI (Mandatory)
* Station DESCRIPTION : (metadata) - string value and not have any formats
* content_type : Station CONTENT TYPE (value auto detected / or can be read from xml metadata - if it's imported using xml file)
* rating : Station RATING
* category : Station CATEGORY
* html_description : (HtmlDescription) : Station Html Description , with HTML formal, it will be visible inside in-app WebView with default header\styles located in \assets\webViewStyleDefaults.html
 
# Sample Xml file
            <channels>
                        <entry>
                                    <unique_id>BBCArabic_1</unique_id>
                                    <title>BBC Arabic</title>
                                    <subtitle>BBC Arabic Main Channel</subtitle>
                                    <description>This is description of BBC Arabic Main Channel</description>
                                    <html_description>...</html_description>
                                    <small_image_URL>
                                    http://www.liveonlineradio.net/wp-content/uploads/2011/06/BBC-Arabic1.jpg
                                    </small_image_URL>
                                    <image>
                                    http://ichef.bbci.co.uk/corporate2/images/width/live/p0/1w/4y/p01w4yxr.jpg/624
                                    </image>
                                    <uri>
                                    http://bbcwssc.ic.llnwd.net/stream/bbcwssc_mp1_ws-araba
                                    </uri>
                                    <content_type>audio/mpeg</content_type>
                                    <rating>5</rating>
                                    <category>news</category>
                                    </entry>
                                    <entry>
                                    <unique_id>ABCNewsRadio_1</unique_id>
                                    <title>ABC News Radio</title>
                                    <subtitle>ABC News Radio Main Channel</subtitle>
                                    <description>This is description of ABC news Main Channel</description>
                                    <image>
                                    https://cdn.pixabay.com/photo/2016/04/19/17/54/radio-1339200_960_720.jpg
                                    </image>
                                    <uri>
                                    http://www.abc.net.au/res/streaming/audio/mp3/news_radio.pls
                                    </uri>
                                    <content_type>audio/mpeg</content_type>
                                    <rating>5</rating>
                                    <small_image_URL>
                                    http://vignette2.wikia.nocookie.net/onceuponatime/images/3/3b/ABC_logo.png/revision/latest?cb=20150913120112&path-prefix=fr
                                    </small_image_URL>
                                    <category>news</category>
                        </entry>
            </channels>

### How to play back a radio station?
Tap the big Play button ;)
 
### How to stop playback?
Tap the big Stop button or unplug your headphones or swipe off the notification from the lockscreen
 
### How to start the sleep timer?
Tapping the Clock symbol starts a 15 minute countdown after which Open-Transistor stops playback. An additional tap adds 15 minutes to the clock. Playback must be running to be able to activate the sleep timer
 
### How to place a station shortcut on the Home screen?
The option to place a shortcut for a station on the Home screen can be accessed from the station's three dots menu. A tap on a shortcut will open Transistor - playback will start immediately.
 
### How to rename or delete a station?
The rename and delete options can be accessed both from the station's context menu.Just tap on the three dots symbol.
 
### Where does Open-Transistor store its stations?
Open-Transistor saves its list of stations in a database. and stores stations images files on your device's external storage. The files are stored in /Android/data/{{Application-Name}}/files/Collection.
 
### How do I backup and transfer my radio stations?
Open-Transistor has support for the Auto Backup feature in Android 6 [Auto Backup](http://developer.android.com/about/versions/marshmallow/android-6.0.html#backup) . Radio stations are always backed up to your Google account and will be restored at reinstall.On devices running on older versions of Android you must manually save and restore the &quot;/Android/data/{{Application-Name}}/files/Collection&quot; folder and &quot;//data/data/{{Application-Name}}/databases/&quot; folder.
 
### Why are there no settings in Open-Transistor?
There is nothing to set ;). Transistor is a very simple app.Depending on your point of view &quot;simple&quot; is either great or lame.
  
  
Which Permissions does Transistor need?
---------------------------------------
### Permission "INSTALL_SHORTCUT" and "UNINSTALL_SHORTCUT"
This permission is needed to install and uninstall radio station shortcuts on the Android Home screen.

### Permission "INTERNET"
Transistor streams radio stations over the internet.

### Permission "READ_EXTERNAL_STORAGE"
Transistor needs access to images, photos and documents to be able to customize radio station icons and to able to open locally saved playlist files.
            
### Permission "VIBRATE"
Tapping and holding a radio station will toggle a tiny vibration.

### Permission "WAKE_LOCK"
During Playback Transistor acquires a so called partial wake lock. That prevents the Android system to stop playback for power saving reasons.

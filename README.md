# boothclient
Public source code for the DJBooth android client.  [Shortcut to the actual code](https://github.com/gilmore606/boothclient/tree/master/app/src/main/java/com/dlfsystems/BoothClient)


## What's this?
This is an Android app for the DJBooth community radio streaming service, a work in progress.  DJBooth lets users create, curate
and live-DJ their own radio stations collaboratively with other users.  This app provides the full functionality of the system, letting 
users create stations, search and explore the music library, and control the DJ queue and behavior.

This client is fully functional as-is, but the system itself is a work in progress and further features will be added as the server side supports
them.


## Can I try it?
The app isn't publically available (yet) but I've uploaded the code here as a portfolio example.  If you're interested in hiring me for Android
work and you'd like to test drive the app, contact me and I can add you to the Fabric beta or provide you the .apk directly.


## How is it made?
It's all in Kotlin (save for one Java view class), and makes use of these libraries:

* **Retrofit**
* **RxJava / RxAndroid / RxBinding**
* **Auth0 (with JWT)** - for universal auth via Google, Facebook, etc
* **SimpleStack** - an extension of the popular Flow library for fragment backstack management
* **PagedList**

The architecture uses no specific MV* pattern but follows the MVI principles of unidirectional data flow, single state, and
an overall reactive/functional approach.


## Screenshots

![splash screen](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot1.png?raw=true)

![stream list](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot2.png?raw=true)

![stream view](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot3.png?raw=true)

![detail view](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot4.png?raw=true)

![links to explore views](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot5.png?raw=true)

![search view](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot6.png?raw=true)

![notification](https://github.com/gilmore606/boothclient/blob/master/screenshots/shot7.png?raw=true)

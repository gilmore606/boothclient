# boothclient
Public source code for the DJBooth android client.
### What's this?
This is an Android app for the DJBooth community radio streaming service, a work in progress.  DJBooth lets users create, curate
and live-DJ their own radio stations collaboratively with other users.  This app provides the full functionality of the system, letting 
users create stations, search and explore the music library, and control the DJ queue and behavior.

This client is fully functional as-is, but the system itself is a work in progress and further features will be added as the server side supports
them.


### Can I try it?
The app isn't publically available (yet) but I've uploaded the code here as a portfolio example.  If you're interested in hiring me for Android
work and you'd like to test drive the app, contact me and I can add you to the Fabric beta or provide you the .apk directly.


### How is it made?
It's all in Kotlin (save for one Java view class), and makes use of these libraries:

* **Retrofit**
* **RxJava / RxAndroid / RxBinding**
* **Auth0 (with JWT)** - for universal auth via Google, Facebook, etc
* **SimpleStack** - an extension of the popular Flow library for fragment backstack management
* **PagedList**

The architecture uses no specific MV* pattern but follows the MVI principles of unidirectional data flow, single state, and
an overall reactive/functional approach.

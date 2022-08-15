# dissertation
Spatial context aware activity recognition system for Msc Connected Environments, UCL.
Project demo: https://www.youtube.com/watch?v=V5IYxz5yRX8

## What the Project is about

The launch of the first generation 'FitBit' wearable device in 2009 catapulted activity trackers into the spotlight prompting other companies to join the revolution. Activity trackers in smart-phones and wearable devices use Human Activity Recognition(HAR) to monitor physical and mental wellness indicators like sleep patterns, health parameters and activity patterns. HAR aims to extract meaningful activity patterns of users for use in context aware applications.  

Activity sensors extract basic activities like 'Standing', 'Running', or 'Walking', which by itself is insufficient to incite effective self-reflection without the context. In support of this, many activity trackers provides a Global positioning system(GPS) trail as context to user’s outdoor activities. While these devices support outdoors, GPS is not a viable option in enclosed buildings due to signal loss in the presence of obstructions. Therefore, this necessitates the exploration of systems that can provide indoor spatial context in an enclosed building.

In this work, a novel prototype has been proposed to provide spatial context to indoor activities. The indoor location data adds more meaning to the sensor data and is conspicuously absent in the existing activity trackers. The setup uses the 'accelerometer' and 'step counter' sensor data using the user’s smart phone as the focal point to classify between the activity states. Indoor geolocation using Bluetooth low energy (BLE) beacons are placed strategically at the test site to provide the room localization context to the activity recognition data. The proposed sensing system aims to create a low-cost solution using cheap BLE beacons and a mobile android application called "Retrace Workplace" which collects, integrates and reflects data. The combination of indoor spatial context and activity classification gives the proposed setup an edge over existing systems.

The test site is a university lab consisting of multiple rooms where performance evaluation was done by comparing the complex activities recorded by the system against observed activities across multiple test points at the site. The experiments analyze the correlation between system accuracy and factors such as mobile position, beacon parameters and user preferences. The experimental results conducted with multiple smart phone devices in various positions showed that the proposed sensor system can achieve an accuracy of 80.5\% while the indoor geolocation proved to be 90\% accurate. 

## Design

The project has the following objectives: 
1. Identification of location
2. Classification of user activity
3. Combining location and activity

The identification of location is done using 'BEEKS' BLE beacons installed in multiple rooms across 5 rooms in the test site. The beacons are configured in 'Eddystone' beacon layout using 'BEEKs beacon maker' Android app. The activity classification uses the on-board sensors ('accelerometer' and 'step sensor') to classify user's activity into 'Stationary','Moving' and 'Walking' categories. A smart-phone application called 'Retrace Workplace' has been created that extracts the location and activity data and synchronises them into a single visualization.

## How to get started

The following apps have been referenced during the research process
1. BEEKS beacon maker App(https://play.google.com/store/apps/details?id=com.bluvision.beaconmaker&gl=US)
2. PetBuddy App(https://www.raywenderlich.com/24859773-activity-recognition-api-tutorial-for-android-getting-started)
3. Android beacon scanner(https://github.com/Bridouille/android-beacon-scanner)

### Beacon configuration
1. Set the advertisement frequency and transmission power based on the chosen indoor area. For my project, beacon discovery was found to be optimum at an advertisement frequency at 2Hz and transmission power at -16dBm. 
2. Configure the beacons by setting a unique eddystone id to each beacon and save the list of beacons in each room. 
3. Stick the beacons on the wall with a clear line of sight(if possible) at an approximate height of 1 - 1.5 meters from the ground.
4. The distance between the beacons depends on the dimensions of the smallest room. In my project, 38 beacons were placed in 5 rooms, each beacon 3-4 meters apart.

### Application installation
1. Download the repository and compile it to create an apk for a smart-phone containing 'Accelerometer' and 'step-sensor' sensors. 
The THRESHOLD and sample size values in DetectActivity.kt are set to 0.3 and 50 respectively. However, they can be changed to optimise the behavior for each phone hardware.
2. Enable bluetooth and location on the phone.
2. Launch the application and allow activity recognition permissions on the phone when prompted.

## Flow of application
The app contains 3 pages: Home, Tracking and History.
The primary purpose of the Home page is to scan for the geofencing beacon that is installed to detect whether the user has entered the office premises.
However, for the app to work, user needs to switch on the bluetooth and location, as well as allow activity tracking permissions for the app. 
If the bluetooth is not switched on, the app shows a snackbar message to remind the user. When these permissions are allowed and user enters the office geofencing range, the 'Tracking' page is enabled, and the status message is updated as shown. 

Once the user moves to the tracking page, the user can start the activity and location tracking by pressing on the start button. The card on top of the page shows the current readings of activity and location separately. The card on bottom shows a line chart visualization of a complex activity. 
For example, if the user is not walking and they are in 'Lunch room', they are assumed to be having lunch. Similiarly, if the user is in Lecture room, they are assumed to be Working. The app supports five complex activities: Lunch, Work, Meeting, Transit and Socialize. Once the user exits the office premises, they can press the 'Stop' button to stop tracking. The reset button clears the line chart and resets the activity and location cards.

To support long term reflection, the data thus collected is stored in SQLite based local database using roomDb android library. On the third page called Analytics, the consolidated duration of each activity can be seen for any particular day. 

Guess-work-of-art
GuessWorkOfArt GuessWorkOfArt is a simple Android game that shows a work of art and offers four possible descriptions. The player has to choose the right one. 

This project uses Kotlin programming language and the following components:

AndroidX AppCompat AndroidX Core AndroidX Lifecycle Kotlin Coroutines Installation Clone this repository and open it using Android Studio. Then, build and run the app on an emulator or a physical device.

Usage Once the app is launched, the player will see an image of a work of art and four possible descriptions. The player has to choose the right one by clicking the corresponding button. After the player's answer, the app shows another image and descriptions for the player to guess.

How it works GuessWorkOfArt uses the WikiArt API to obtain the images and descriptions of works of art. The app downloads the content of the API and extracts the necessary information using regular expressions.

The app also downloads the images using coroutines and displays them using an ImageView.


# Waste - An Open Source Arduino based food monitor

### What's in the page?
* [What's all about](https://github.com/gabrielemaffoni/waste#whats-all-about)
* [Requirements](https://github.com/gabrielemaffoni/waste#what-are-the-requirements)
* [FAQ and graphic assets](https://github.com/gabrielemaffoni/waste#faq)
* Looking for the [Wiki](https://github.com/gabrielemaffoni/waste/wiki)?

## What's all about?
Waste is a project I worked on for my honours. It is a smart cap that answers the question people with busy lives always ask themselves at the supermarket: "How much milk do I still have?".

## What are the requirements?

### Bluno beetle
- VL6180X ToF Sensor (I've used Sparkfun's here)
- Two TMP36 Temperature sensors
- One button
- One LED - Adafruit
- One "Two coincell batteries holder"

### Raspberry Pi
- The bluetooth antenna from DFRobot (otherwise, Bluno Beetle won't communicate)
- A wifi connection

### Database
I've used Firebase here, because I was familiar with it and it was easy to work with. I would suggest you to do the same, especially if you don't want the hassle of creating your own database.

### Push notification
Still Firebase, I'm using firebase functions

# FAQ

## Why is this code so messy?
I am a rookie developer. My profession is UX designer, developing is just a hobby for me. Hence, it is messy and I'll try to clean it up. But if you want to contribute, feel free to do it!

## Is ther any documentation?
YES! go in the wiki and you will find it ;)

## Do you have any logo or what?
Yes I do. I was helped by my friend [Andrea Gaspari](https://www.behance.net/andreagaspari) and we created the corporate. I am going to update it from time to time, but everything available so fare is in [graphic-assets](https://github.com/gabrielemaffoni/waste/tree/master/graphic-assets).

## How about the app? Any UX study?
Of course, I'm a UX designer. [Here](https://sketch.cloud/s/YjPbM)'s the sketch file I will always update. You can use it as library, so you can see the updates from time to time!

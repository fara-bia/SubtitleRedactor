⚠️ **Important:** You need to download the required AI models (approx. 5 GB) separately for the program to work. Download the models **[From this link: "https://drive.google.com/drive/folders/1cHR25XjXTTQFPMr0Ueokjrt3upnDgZsQ"]** and paste the `ai` folder inside it into the root directory of the project.

# SubtitleRedactor

SubtitleRedactor is a foundational, AI-powered subtitle spelling and grammar editing tool that offers CPU and GPU (CUDA) acceleration options.

Requirements to run the program on your computer:

1. Java Development Kit (JDK 21)
2. Eclipse IDE for Java Developers

Installation steps:

1. Extract the ZIP file. (Since you are reading this, you probably already have.)
2. Open Eclipse.
3. Click on File > Import from the top-left menu.
4. In the window that opens, select General > Existing Projects into Workspace and click Next.
5. Click the Browse button in the "Select root directory" section.
6. Ensure the project is checked in the Projects section and click Finish.

Execution steps:

1. Expand the `SubtitleRedactor` project from the Package Explorer panel on the left side of Eclipse.
2. Open the src > subtitleRedactor folders respectively.
3. Right-click on the Main.java file.
4. Click on Run As > Java Application. (Alternatively, you can double-click on Main.java and use the Run button from the top menu of Eclipse.)

Congratulations! The SubtitleRedactor interface will appear. You can now select your `.ass` file, set your CPU or GPU (CUDA) preference, and start your process.

---

Extra Note: Running it via the IDE will be the most stable method. If you manage to create a successful standalone build that runs in non-JRE environments, feel free to let me know.
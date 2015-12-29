# Simple File I/O Helper App for Scratch Extension
This is a helper app to be used with the [File I/O Scratch Extension](https://github.com/Znapi/scratchx/wiki/file-io).

[**Download**](https://github.com/Znapi/scratchx-file-io-helper-app/releases/download/v1.2/file-io-helper-app.jar) (Runnable JAR file)

**Safety notice**: It technically is not safe to use this helper app. It allows anything on your computer, Scratch extensions, programs, etc., to gain full access to your file system. However, is unlikely that anything else on your computer will expect that you are running this helper app and exploit it. Nevertheless, be careful. Only use this helper app when you are using Scratch extensions and projects that you trust.

The most recent version of the source code does implement some security, but it is very basic and inconvenient. It needs major changes before it is a full release.

**Java is required** to run this helper app.

How to use
---
To use this helper app with the Scratch extension, simply download it using the link above, and double click it to run it. It will ask for you to choose a 'root' directory.

The root directory is where the extension begins to access files at. For example, if you set the root directory to `/example/directory/`, then when the extension says `read file [example.txt]`, it will open and read the file  `/example/directory/example.txt`. Choose a directory that doesn't contain important files.

The extension will set it's status to 'Ready' once it realizes the helper app is running.

### Usage from command line
The helper app can also be used from the command line, with or without making a new window.

To run JAR files from the command line, use `java -jar path/to/.jar`

On the command line, the helper app takes two arguments. They go after `path/to/.jar`
* `path/to/root/dir/`: the root directory. **Required** unless the `-gui` option is also used, in which case it is *optional*.
* `-gui`: **Not required**. If not used, the app runs in console mode. No extra window is created, and the app logs to the console. If the option is used, the app runs in the normal GUI mode, and it creates and logs to a window.

Technical Details
---
The helper app is a simple HTTP server that listens on port 8080. The port cannot be changed. This means that if you already have a service on port 8080, you will have to stop it. If the port was allowed to be changed, then the Scratch extension would have to ask for a port number, and it becomes a usability issue. I don't want to confuse Scratchers by asking them for a port number.

The extension initially checks for the helper app by making an OPTIONS request to http://localhost:8080/. It verifies that the response is from the helper app by looking for a `X-Is-ScratchX-File-IO-Helper-App` header that has a value of `yes`.

Credits
---
Uses [NanoHTTPD](http://nanohttpd.com).

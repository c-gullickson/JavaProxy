# JavaProxy
Network proxy built in Java for CSCI 846, assignment 1

The application code can be cloned or downloaded from GitHub: https://github.com/c-gullickson/JavaProxy, specifically the ProxyServer.java and RequestHandler.java files. When pulling these files down, they should be placed into an accessible directory within the local environment.
Open a command prompt window as an administrator, and navigate to the folder where the source files are located. Command "cd {file path}" 
 
Within the command prompt window, run the command “javac ProxyServer.java” and the command “javac RequestHandler.java”. These two commands will compile the Java files in order to make them runnable. 
 
After the files have been compiled, run the command “java ProxyServer 1234”. This command will initialize the proxy server listening on port 1234, and create two new directories. One for logging, and one for the cache. With the proxy started, the browser client can be set up to utilize the proxy client. For the testing, I have used a combination of the Firefox browser, and Microsoft Edge browser. 
Firefox browser setup to utilize the proxy application listening on port 1234
 
Microsoft Edge browser setup to utilize the proxy application listening on port 1234

The proxy can then be utilized by using a browser set up with the proxy, and navigating to an internet link. The first test link was to URL: http://www.cs.ndsu.nodak.edu/aboutus.htm, with the proxy identifying the client request along with sequential GET requests. Since this was the first time the proxy has been utilized during, there was no previous cache file found with the lookup, so the connection was made separate with resulting data creating a new data file for caching.
 
When navigating to a different website, and then back to http://www.cs.ndsu.nodak.edu/aboutus.htm the proxy once again looked for a cached file based on the requested url of the client within the map of files. Since the page requested had already been loaded and cached to prior, the files were found within the cache and read back to the client.

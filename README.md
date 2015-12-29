# Simple File I/O Scratch Extension
This branch contains the Scratch File I/O Extension in ext.js, and a minified version in ext.min.js.

[Extension documentation](https://github.com/Znapi/scratchx/wiki/File-I-O/). In the future this branch will contain user friendly project pages for users to get the extension and helper app from, rather than the wiki pages on the other repository linked above.

The extension initially checks for the helper app by making an OPTIONS request to http://localhost:8080/. It verifies that the response is from the helper app by looking for a `X-Is-ScratchX-File-IO-Helper-App` header that has a value of `yes`.

Every request to the helper app it makes is repeated every five seconds until it succeeds. If a request fails and needs to be repeated, the extension status is set to 1,'Waiting for helper app'. Once a repeated request succeeds, the status is reset to 2,'Ready'.
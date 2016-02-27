# File I/O Scratch Extension
This is the File I/O Scratch Extension. It also requires a helper app for full functionality, which is located [here](https://github.com/Znapi/scratchx-file-io-app/).

The extension code is in `ext.js`, and a minified version is in `ext.min.js`. The documentation/project pages for the Scratch extension and the helper app are also located in this repository.

See the [project pages](https://znapi.github.io/scratchx-file-io/).

---

The extension initially checks for the helper app by making an OPTIONS request to http://localhost:8080/. It verifies that the response is from the helper app by looking for a `X-Is-ScratchX-File-IO-Helper-App` header that has a value of `yes`.

Every request to the helper app it makes is repeated every five seconds until it succeeds. If a request fails and needs to be repeated, the extension status is set to 1,'Waiting for helper app'. Once a repeated request succeeds, the status is reset to 2,'Ready'.

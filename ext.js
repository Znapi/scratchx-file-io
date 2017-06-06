(function(ext) {

		ext.read = function(dir, callback) {
				makeRequestToHelperApp('GET', dir, callback);
		};

		ext.write = function(text, dir, callback) {
				makeRequestToHelperApp('PUT', dir, callback, text);
		};

		ext.append = function(text, dir, callback) {
				makeRequestToHelperApp('PUT', dir, callback, text, "append");
		};

		ext.make = function(dir, callback) {
				makeRequestToHelperApp('POST', dir, callback);
		};

		ext.newline_char = function() {
				return '\n';
		}

		var helperDetected = false;

		var intervals = {};

		ext._shutdown = function(){
				for(var prop in intervals) {
						if(intervals.hasOwnProperty(prop))
								window.clearInterval(prop);
				}
		};

		ext._getStatus = function() {
				if(helperDetected)
						return {status:2, msg:'Ready'};
				else
						return {status:1, msg:'Waiting for Helper App'};
		};

		ScratchExtensions.register('Simple File I/O',
															 {
																	 blocks: [
																			 ['R', 'read from file %s', 'read', 'text.txt'],
																			 ['w', 'write %s to file %s', 'write', 'hello world', 'text.txt'],
																			 ['w', 'append %s to file %s', 'append', 'hello world', 'text.txt'],
																			 ['w', 'create file %s', 'make', 'text.txt'],
																			 ['r', 'newline', 'newline_char']
																	 ],
																	 url: 'http://znapi.github.io/scratchx-file-io/'
															 },
															 ext);

		function makeRequestToHelperApp(method, uri, callback, body, action) {
				var intervalID;
				function request(method, uri, body, action, onSuccess, onFailure) {
						var r = new XMLHttpRequest();
						r.onreadystatechange = function() {
								if(r.readyState === 4) {
										if(r.getResponseHeader('X-Is-ScratchX-File-IO-Helper-App')==='yes')
												onSuccess(r.responseText);
										else
												onFailure();
								}
						};
						r.open(method, 'http://localhost:8080/'+uri, true);
						if(action !== undefined)
								r.setRequestHeader("X-Action", action);
						if(body !== undefined)
								r.send(body);
						else
								r.send();
				}
				request(method, uri, body, action,
								callback,
								function() {
										helperDetected = false;
										intervalID = window.setInterval(request, 5000, method, uri, body, action,
																										function(rsp) {window.clearInterval(intervalID); delete intervals[intervalID]; helperDetected=true; callback(rsp)},
																										function() {}
																									 );
										intervals[intervalID] = undefined;
								}
							 );
		}

		makeRequestToHelperApp('OPTIONS', '', function() {
				helperDetected = true;
		});

})({});

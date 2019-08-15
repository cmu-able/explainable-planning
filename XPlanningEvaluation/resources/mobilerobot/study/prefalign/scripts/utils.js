/** 
 * This function getQueryStringParameters takes url as parmater and returns 
 * parameters name and value in JSON key value format
 * @parameter {String} url 
 * (if url is not passed it takes the current url from window.location.href) 
 *
 **/
function getQueryStringParameters(url) {
	var urlParams={},
	match,
	additional = /\+/g, // Regex for replacing additional symbol with a space
	search = /([^&=]+)=?([^&]*)/g,
	decode = function (s) { 
		return decodeURIComponent(s.replace(additional, " "));
	},
	query;
	
	if (url) {
		if(url.split("?").length > 0) {
			query = url.split("?")[1];
		}
	} else {
		url = window.location.href;
		query = window.location.search.substring(1);
	}
	
	while (match = search.exec(query)) {
		urlParams[decode(match[1])] = decode(match[2]);
	}
	return urlParams;
}

function getQueryStringParameter(url, paramName) {
	var query_string_params = getQueryStringParameters(url);
	var paramValue = query_string_params[paramName];
	return paramValue;
}

function saveQueryStringParameterToLocalStorage(url, paramName) {
	var paramValue = getQueryStringParameter(url, paramName);
	
	if (typeof(Storage) !== "undefined") {
		localStorage.setItem(paramName, paramValue);
	}
}

function saveMTurkParametersToLocalStorage() {
	var url = window.location.href;
	saveQueryStringParameterToLocalStorage(url, "assignmentId");
	saveQueryStringParameterToLocalStorage(url, "hitId");
	saveQueryStringParameterToLocalStorage(url, "turkSubmitTo");
	saveQueryStringParameterToLocalStorage(url, "workerId");
}
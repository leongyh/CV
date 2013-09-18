/*---Start Chart---*/
$(function() { //ensures DOM ready
	var seriesData = [];
	var lastTime = null; //last time of data picked up
	var isAJAXReady = false; //used as checkpoint to get ajax synchronicity

	var interval = 5000;

	var options = {
		series: { shadowSize: 1 },
		lines: { fill: true, fillColor: { colors: [ { opacity: 1 }, { opacity: 0.1 } ] }},
		//yaxis: { min: 0, max: 20 },
		xaxis: { 
				mode: "time",
				minTickSize: [1, "minute"],
				timeformat: "%b/%d %H:%M:%S",
				},
		colors: ["#F4A506"],
	};

	//This is only temporary to pick up the correct stream based 
	//on what chart is being displayed. need to find a better 
	//solution.
	function getStream()
	{
		//needs to be DOM ready
		var chartName = document.getElementById("chartName").innerHTML;
		
		if (chartName == "Tank Water Temperature (SI)") {
			return 0
		} else if (chartName == "Tank Water Temperature (US)") {
			return 1;
		} else if (chartName == "External Temperature (SI)") {
			return 2;
		} else if (chartName == "External Temperature (US)") {
			return 3;
		} else if (chartName == "Flow Rate (SI)") {
			return 4;
		}
	}

	function getInitialData(system)
	{	
		var request = new XMLHttpRequest();
		var data = null;
		var currentTime = Math.round((new Date()).getTime()/1000);
		//find better way to get last X minutes and to update xaxis ticker
		var lastFive = currentTime - (10 * 60);
		var stream = getStream();
		
		request.open("GET", "http://50.18.107.49:8080/data/" + system
							+ "?" + "app=plot" 
							+ "&" + "stream=" + stream 
							+ "&" + "since=" + lastFive 
							, true);
		request.send(null);
		request.onreadystatechange = function() {
			if (request.readyState == 4){
				data = JSON.parse(request.responseText);
				initializeSeries(data);
				isAJAXReady = true;
			}
		};
	}

	function initializeSeries(jsonObj)
	{
		for (var i = 0; i < jsonObj.numPairs; i++)
			//Multiply time value by 1000 to convert seconds to microseconds.
			//Reason is JSON returns seconds unit and Flot accepts microseconds unit.
			seriesData.push([jsonObj.xaxis[i] * 1000, jsonObj.yaxis[i]]);
			
		$.plot($("#plot"), [seriesData], options);
		
		lastTime = ((seriesData[seriesData.length - 1][0]) / 1000) + 1;
	}

	function getNewData(system)
	{
		if (isAJAXReady == true)
		{
			isAJAXReady = false;
			
			var request = new XMLHttpRequest();
			var data = null;
			var stream = getStream();
			
			request.open("GET", "http://50.18.107.49:8080/data/" + system
							+ "?" + "app=plot" 
							+ "&" + "stream=" + stream 
							+ "&" + "since=" + lastTime
							, true);
			request.send(null);
			request.onreadystatechange = function() {
				if (request.readyState == 4){
					data = JSON.parse(request.responseText);
					updateSeries(data);
					isAJAXReady = true;
				}
			};
		}
	}

	function updateSeries(jsonObj)
	{
		seriesData.splice(0, jsonObj.numPairs);
		
		for (var i = 0; i < jsonObj.numPairs; i++)
			//Multiply time value by 1000 to convert seconds to microseconds.
			//Reason is JSON returns seconds unit and Flot accepts microseconds unit.
			seriesData.push([jsonObj.xaxis[i] * 1000, jsonObj.yaxis[i]]);
			
		$.plot($("#plot"), [seriesData], options);
		
		lastTime = ((seriesData[seriesData.length - 1][0]) / 1000) + 1;
	}
	
	getInitialData("ArduinoBravo");
	//setTimeout(function(){ /*Delay*/ }, interval);
	setInterval(function(){ getNewData("ArduinoBravo"); }, interval);
});
/*---End Chart---*/
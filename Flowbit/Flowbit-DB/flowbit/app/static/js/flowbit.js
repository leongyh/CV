/*---Start Dashboard: Nick's Aquarium---*/
//Current way to get collection name. Need to refactor
//such that collection name is loaded based on what
//user is viewing on the HTML. Make it dynamic whereas
//currently static.
var systemName = "ArduinoBravo";

/*---Start Data AJAX and Realtime Update---*/
function getData(system)
{
    var request = new XMLHttpRequest();
    var data = null;
    
    request.open("GET", "http://50.18.107.49:8080/data/" + system + "?app=dashboard", true);
    request.send(null);
    request.onreadystatechange = function() {
        if (request.readyState == 4){
            data = JSON.parse(request.responseText);
            updateData(data);
        }
    };
}

function updateData(data)
{
	for (var i = 0; i < data.datastreams.length; i++) {
		writeSpan(data.datastreams[i].id,
					data.datastreams[i].current_value + " " + data.datastreams[i].unit.symbol);
	}
}

function writeSpan(spanID, text)
{
	var spanElements = document.getElementsByTagName("span");
	
	//this is necessary for now as id attributes for span tag is not unique.
	//span has no name attribute which alllows none-unique mapping.
	//span tag is necessary for the jquery mobile numbers to display.
	//Either optimize looping (worst case n^2)
	//or change the template design frontend.
	for (var i = 0; i < spanElements.length; i++) {
		if (spanElements[i].getAttribute("id") == spanID) {
			spanElements[i].innerHTML = text;
		}
	}
}
/*---End Data AJAX and Realtime Update---*/

/*---Start Controls AJAX and Update---*/
function getControls(system)
{
    var request = new XMLHttpRequest();
    var controlsData = null;
    
    request.open("GET", "http://50.18.107.49:8080/controls/" + system + "?format=json", true);
    request.send(null);
    request.onreadystatechange = function() {
        if (request.readyState == 4){
            controlsData = JSON.parse(request.responseText);
            initializeControls(controlsData);
        }
    };
}

function initializeControls(controls)
{
	var MASTER = $("#masterSwitch");
	var PUMP = $("#pumpSwitch");
    var LIGHTING = $("#lightingSwitch");
	
    if (controls["instructionstream"]["PUMP"] == "ON") {
        PUMP.val('on').slider('refresh');
    } else if (controls["instructionstream"]["PUMP"] == "OFF") {
        PUMP.val('off').slider('refresh');
    }
	
    if (controls["instructionstream"]["LIGHTING"] == "ON") {
        LIGHTING.val('on').slider('refresh');
    } else if (controls["instructionstream"]["LIGHTING"] == "OFF") {
        LIGHTING.val('off').slider('refresh');
    }
	
	var pumpVal = PUMP[0].selectedIndex == 1 ? true:false;
    var lightingVal = LIGHTING[0].selectedIndex == 1 ? true:false;
	
	if (pumpVal || lightingVal) {
		MASTER.val('on').slider('refresh');
    } else {
        MASTER.val('off').slider('refresh');
    }
}

//Updates controls internally without having to request control file from DB.
//Might be a good idea to prevent multiple request exploit?
function updateMasterControls()
{
	var MASTER = $("#masterSwitch");
	var PUMP = $("#pumpSwitch");
    var LIGHTING = $("#lightingSwitch");
	
	var pumpVal = PUMP[0].selectedIndex == 1 ? true:false;
    var lightingVal = LIGHTING[0].selectedIndex == 1 ? true:false;
	
	if (pumpVal || lightingVal) {
		MASTER.val('on').slider('refresh');
    } else {
        MASTER.val('off').slider('refresh');
    }
}

function updateControls()
{
	var MASTER = $("#masterSwitch");
	var PUMP = $("#pumpSwitch");
    var LIGHTING = $("#lightingSwitch");
	
	var masterVal = MASTER[0].selectedIndex == 1 ? true:false;
	
	if (masterVal) {
        PUMP.val('on').slider('refresh');
        LIGHTING.val('on').slider('refresh');
    } else {
        PUMP.val('off').slider('refresh');
        LIGHTING.val('off').slider('refresh');
    }
}

//Can probably refactor master lock and update controls into a pair instead of
//two separate pairs
function postMasterLockControls(system)
{
    var request = new XMLHttpRequest();
    var controls = createMasterLockControls();
    
    request.open("PUT", "http://50.18.107.49:8080/controls/" + system, true);
    
    request.setRequestHeader("Content-Type", "application/json");
    
    request.send(JSON.stringify(controls));
}

function postControls(system)
{
    var request = new XMLHttpRequest();
    var controls = createControls();
    
    request.open("PUT", "http://50.18.107.49:8080/controls/" + system, true);
    
    request.setRequestHeader("Content-Type", "application/json");
    
    request.send(JSON.stringify(controls));
}

function createMasterLockControls()
{
    var controls = {};
	var MASTER = $("#masterSwitch");
    
	var masterVal = MASTER[0].selectedIndex == 1 ? true:false;
	
    controls["Time"] = Math.round((new Date()).getTime()/1000);
    controls["username"] = "user1"; //Make this dynamic when login system in place
    controls["SystemID"] = "00001";
    controls["instructionstream"] = {};
    
	if (masterVal) {
		controls["instructionstream"]["PUMP"] = "ON";
		controls["instructionstream"]["LIGHTING"] = "ON";
	} else {
		controls["instructionstream"]["PUMP"] = "OFF";
		controls["instructionstream"]["LIGHTING"] = "OFF";
	}
	
	controls["instructionstream"]["FLOWRATE_LPM"] = "N/A";

    return controls;
}

function createControls()
{
    var controls = {};
    var PUMP = $("#pumpSwitch");
    var LIGHTING = $("#lightingSwitch");
    
    var pumpVal = PUMP[0].selectedIndex == 1 ? true:false;
    var lightingVal = LIGHTING[0].selectedIndex == 1 ? true:false;
    
    controls["Time"] = Math.round((new Date()).getTime()/1000);
    controls["username"] = "user1"; //Make this dynamic when login system in place
    controls["SystemID"] = "00001";
	
	//might need to sort by array rather than dictionary.
	//order is important when converting to csv.
	//either sort here or sort server side,
	//or dont use csv at all for arduino.
    controls["instructionstream"] = {}; 
    
    if (pumpVal) {
        controls["instructionstream"]["PUMP"] = "ON";
    } else {
        controls["instructionstream"]["PUMP"] = "OFF";
    }
    
    if (lightingVal) {
        controls["instructionstream"]["LIGHTING"] = "ON";
    } else {
        controls["instructionstream"]["LIGHTING"] = "OFF";
    }
    
    controls["instructionstream"]["FLOWRATE_LPM"] = "N/A";
    
    return controls;
}

function btnMasterLockControlsUpdate()
{
    postMasterLockControls(systemName);
	updateControls();
}

function btnControlsUpdate()
{
    postControls(systemName);
	updateMasterControls();
}
/*---End Controls AJAX and Update---*/

/*---Start Dashboard Initialization and Interval Update---*/
function initialize()
{
    getData(systemName);
    getControls(systemName);
}

//interval in ms. Static as of now, but should be dynamic as user specified.
var interval = 5000; 

initialize();
setInterval(function(){ getData(systemName); }, interval);
/*---End Dashboard Initialization and Interval Update---*/
/*---End Dashboard: Nick's Aquarium---*/

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
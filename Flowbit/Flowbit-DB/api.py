import pymongo, gviz_api, json, os, time, csv, datetime
from bottle import run, route, request, response, hook
from bson import json_util

connection = pymongo.MongoClient('localhost', 27017);

@hook('after_request')
def enableCORS():
	response.headers['Access-Control-Allow-Origin'] = '*'
	response.headers['Access-Control-Allow-Methods'] = 'PUT, GET, POST, DELETE, OPTIONS'
	response.headers['Access-Control-Allow-Headers'] = 'Origin, Accept, Content-Type, X-Requested-With, X-CSRF-Token'

@route('/sysinfo/<system>', method = 'PUT')
def systemInformation(system):
	if request.method == 'OPTIONS':
		return "PASS"
		
	else:
		basedir = os.path.abspath(os.path.dirname(__file__))
		f = open(os.path.join(basedir, 'sysinfo') + '/' + system + '.txt', 'w+')
		f.write(str(request.body))
		f.close()
		
		return "SUCCESS. DATA PUSHED"

@route('/data/<system>', method = 'GET')
def getData(system):
	db = connection[system]
	
	if request.query.app == 'excel':
		response.content_type = 'text/csv'
		
		return createExcelCSV(db['data'], int(request.query.fromTime), int(request.query.toTime))
	
	elif request.query.app == 'charts':
		response.content_type = 'application/json'

		return createChartsJSON(db['data'], int(request.query.stream), int(request.query.since))
		
		# Flot format------------------
		# return json.dumps(createPlotJSON(db['data'], int(request.query.stream), int(request.query.since)), default=json_util.default)

	elif request.query.app == 'updatecharts':
		response.content_type = 'application/json'

		return json.dumps(updateChartsJSON(db['data'], int(request.query.stream), int(request.query.since)), default=json_util.default)

	elif request.query.app == 'dashboard':
		response.content_type = 'application/json'
		
		for item in db['data'].find().sort("_id", -1).limit(1):
			data = json.dumps(item, default=json_util.default)

		return data

@route('/data/<system>', method = ['OPTIONS', 'PUT'])
def pushData(system):
	if request.method == 'OPTIONS':
		return "PASS"
		
	else:
		db = connection[system]
		data = json.load(request.body)
		db['data'].insert(data)
		
		return "SUCCESS. DATA PUSHED"
	
@route('/controls/<system>', method = 'GET')
def getControls(system):
	db = connection[system]
	
	for item in db['controls'].find().sort("_id", -1).limit(1):
		params = item
	
	if request.query.format == 'json':
		response.content_type = 'application/json'
		
		return json.dumps(params, default=json_util.default)
		
	elif request.query.format == 'csv':
		response.content_type = 'text/csv'
	
		return createControlsCSV(params)
	
@route('/controls/<system>', method = ['OPTIONS', 'PUT'])
def pushControls(system):
	if request.method == 'OPTIONS':
		return "PASS"
	
	else:
		db = connection[system]
		params = json.load(request.body)
		db['controls'].insert(params)
		
		return "SUCCESS. CONTROL PARAMETERS PUSHED"

def isRegistered(system):
	return collection.find_one({"System Name": system}) == None

def createControlsCSV(document):
	return ((',').join(document['instructionStream'].values())).upper() + '\n'
	
def createChartsJSON(collection, stream, sinceTime):
	columns = 	{
					"time": ("datetime", "Time"),
					"value": ("number", "Value")
				}
	rows = []
	
	for item in collection.find({"unix_time": {"$gt": sinceTime}}).sort("_id", 1):
		rows.append({"time": datetime.datetime.utcfromtimestamp(item["unix_time"]), "value": item["datastreams"][stream]["current_value"]})
	
	data_table = gviz_api.DataTable(columns)
	data_table.LoadData(rows)

	return data_table.ToJSon()

	# #------ Flot format ----------------------
	# #stream will be default 0. Add rest later.
	# jsonObj = {'xaxis': [], 'yaxis': [], 'numPairs': 0}
	# pairs = 0
	# for item in collection.find({"Time": {"$gt": sinceTime}}).sort("_id", 1):
	#     jsonObj['xaxis'].append(item["Time"])
	#     jsonObj['yaxis'].append(item["datastreams"][stream]["current_value"])
	#     pairs += 1
	
	# jsonObj['numPairs'] = pairs
	
	# return jsonObj

def updateChartsJSON(collection, stream, sinceTime):
	jsonObj = {'xaxis': [], 'yaxis': [], 'numPairs': 0}
	pairs = 0

	for item in collection.find({"unix_time": {"$gt": sinceTime}}).sort("_id", 1):
	    jsonObj['xaxis'].append(item["unix_time"])
	    jsonObj['yaxis'].append(item["datastreams"][stream]["current_value"])
	    pairs += 1
	
	jsonObj['numPairs'] = pairs
	
	return jsonObj

def createExcelCSV(collection, fromTime, toTime):
	csvString = "Time,TankWaterTempSI,TankWaterTempUS,OutsideTankTempSI,OutsideTankTempUS,FlowrateSI\n"
	
	for item in collection.find({"Time": {"$gt": fromTime, "$lt": toTime}}).sort("_id", 1):
		csvString += str(item["Time"]) + "," \
					+ str(item["datastreams"][0]["current_value"]) + "," \
					+ str(item["datastreams"][1]["current_value"]) + "," \
					+ str(item["datastreams"][2]["current_value"]) + "," \
					+ str(item["datastreams"][3]["current_value"]) + "," \
					+ str(item["datastreams"][4]["current_value"]) + "\n"
					
	return csvString
	
run(host='0.0.0.0', port=8080, reloader=True, debug=True)

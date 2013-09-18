import pymongo, json, os, time, csv
from bottle import run, route, request, response, hook
from bson import json_util

connection = pymongo.MongoClient('localhost', 27017);

@hook('after_request')
def enableCORS():
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'PUT, GET, POST, DELETE, OPTIONS'
    response.headers['Access-Control-Allow-Headers'] = 'Origin, Accept, Content-Type, X-Requested-With, X-CSRF-Token'
    
@route('/register/<system>', method = 'PUT')
def registerSystem(system):
    collection = connection['registeredSystems']['list']
    
    if isRegistered(system) == True:
        id = json.load(request.body)
        id["System Name"] = system
        collection.insert(id)
    
        return "SUCCESS. SYSTEM REGISTERED"
        
    else:
        return system + " ALREADY REGISTERED"

@route('/data/<system>', method = 'GET')
def getData(system):
    db = connection[system]
    
    if request.query.app == 'excel':
		response.content_type = 'text/csv'
		
		return createExcelCSV(db['data'], int(request.query.fromTime), int(request.query.toTime))
	
    elif request.query.app == 'plot':
        response.content_type = 'application/json'
		
        return json.dumps(createPlotJSON(db['data'], int(request.query.stream), int(request.query.since)), default=json_util.default)

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
    return (',').join(document['instructionstream'].values()) + '\n'
    
def createPlotJSON(collection, stream, sinceTime):
    #stream will be default 0. Add rest later.
    jsonObj = {'xaxis': [], 'yaxis': [], 'numPairs': 0}
    pairs = 0
    #print(sinceTime)
    for item in collection.find({"Time": {"$gt": sinceTime}}).sort("_id", 1):
        jsonObj['xaxis'].append(item["Time"])
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
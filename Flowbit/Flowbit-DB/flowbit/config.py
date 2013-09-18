import os

CSRF_ENABLED = True
SECRET_KEY = 'Flowbetches2012'

ORGANIZATION_DBS = [ 
	{ 'DBALIAS': 'Flowbit', 'DBNAME': 'Flowbit', 'HOST': '50.18.107.49', 'PORT': 27017 },
	{ 'DBALIAS': 'NuestraAgua', 'DBNAME': 'Nuestra Agua', 'HOST': '0.0.0.0', 'PORT': 27017 } ]

#very temporary. needs to pull from DB in the future when system reg is up
SYSTEM_LOCS = [
	{'SYSTEMNAME': 'DEMO System', 'Lat': 37.768204, 'Long': -122.393274},
	{'SYSTEMNAME': 'Nick\'s Aquarium', 'Lat': 37.852422, 'Long': -122.257375},
	{'SYSTEMNAME': 'Nuestra Agua - San Cristobal Water Kiosk', 'Lat': 16.710803, 'Long': -92.628739},
	{'SYSTEMNAME': 'LoLo - Bihar Water Kiosk', 'Lat': 26.332807, 'Long': 85.407715},
	{'SYSTEMNAME': 'Pyong Yang - Kim Jong Un\'s Mechanical Bull', 'Lat': 39.039986, 'Long': 125.754662} ]
	
ROLES = [ 'ADMIN', 'ENGINEER' ]

PARSE_ID = "7h8TXODIXq3dxx0yRk5TY1CRALyDcwjwdHYzLi7Q"
PARSE_API_KEY = "iy5G8coOQTsKlfqaGQgkN0f1cQnQyEtbLUN7PcuN"
			
basedir = os.path.abspath(os.path.dirname(__file__))

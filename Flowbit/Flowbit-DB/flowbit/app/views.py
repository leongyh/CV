from flask import render_template, flash, redirect, session, url_for, request, g, jsonify
from flask.ext.login import login_user, logout_user, current_user, login_required

from app import app, lm, organization_dbs
from forms import LoginForm, RegistrationForm, MasterControlsForm, ControlsForm, CommentForm
from models import User

import json, urllib, httplib
from datetime import datetime

def connectParse():
	#print("Connecting to Parse.com API...\n")
	
	parse = httplib.HTTPSConnection('api.parse.com', 443)
	parse.connect()
	
	return parse
	
@lm.user_loader
def loadUser(id):
	if 'id' in session:
		db_parse = connectParse()
		db_parse.request('GET', '/1/users/' + str(id), '', {
			"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY']
		})
		user_obj = json.loads(db_parse.getresponse().read())
		#print(user_obj)
		#print("\n")
		#print(id)
		if 'code' in user_obj and user_obj['code'] == 101:
			return None
		else:
			return User(user_obj)
	else:
		return None

@app.before_request
def before_request():
	g.user = None
	
	if 'id' in session:
		g.user = current_user

@app.route('/login', methods = ['GET', 'POST'])
def login():
	print('Login page\n')
	if g.user is not None and not g.user.is_anonymous():
		return redirect(url_for('index'))
	
	login_form = LoginForm()
	
	if request.method == 'POST' and login_form.validate():
		#print('Login page form submit\n'+login_form.username.data+'\n')
		
		login_params = urllib.urlencode({"username": login_form.username.data, "password": login_form.password.data})	
		db_parse = connectParse()
		db_parse.request('GET', '/1/login?%s' % login_params, '', {
			"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY']
		})
		
		session['remember_me'] = login_form.remember_me.data
		
		return registerOrLogin(json.loads(db_parse.getresponse().read()))
    
	return render_template('login.html', 
							title = 'Login',
							form = login_form)

def registerOrLogin(resp):
	#print('Register or login\n')
	if 'code' in resp:
		if resp['code'] == 101:
			#print('Redirect to register page\n') 
			return redirect(url_for('register'))
	
	session['id'] = resp['objectId']
	session['organization'] = resp['organization']
	session['name'] = resp['firstName'] + ' ' + resp['lastName']
	session['username'] = resp['username']
	remember_me = False
	
	if 'remember_me' in session:
		remember_me = session['remember_me']
		session.pop('remember_me', None)
	
	login_user(User(resp), remember = remember_me)
	#print('Logged in sucess\n')
	return redirect(request.args.get('next') or url_for('index'))
    
@app.route('/register', methods = ['GET', 'POST'])
def register():
	if g.user is not None and not g.user.is_anonymous():
		return redirect(url_for('index'))
	
	reg_form = RegistrationForm()
	
	if request.method == 'POST' and reg_form.validate():
		#print('Register form submit\n')
		if reg_form.password.data != reg_form.password_repeat.data:
			return redirect(url_for('register'))
		
		reg_params = json.dumps({
			"username": reg_form.username.data,
			"password": reg_form.password.data,
			"email": reg_form.email.data,
			"organization": reg_form.organization.data,
			"title": reg_form.title.data,
			"firstName": reg_form.firstName.data,
			"lastName": reg_form.lastName.data,
			"phone": reg_form.phone.data
		})
		db_parse = connectParse()
		db_parse.request('POST', '/1/users', reg_params, {
			"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
			"Content-Type": "application/json"
		})
		print(json.loads(db_parse.getresponse().read()))
		login_params = urllib.urlencode({"username": reg_form.username.data, "password": reg_form.password.data})
		db_parse2 = connectParse()
		db_parse2.request('GET', '/1/login?%s' % login_params, '', {
			"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY']
		})
		remember_me = False
		login_user(User(json.loads(db_parse2.getresponse().read())), remember = remember_me)
		
		return redirect(url_for('index'))
		
	return render_template('register.html', title = 'Register', form = reg_form)

@app.route("/favicon.ico")
def favicon():
	return "PASS"

@app.route('/')
@app.route('/index')
@login_required
def index():

	return render_template('index.html', title = 'Home', pageid = 'index')

@app.route('/dashboard', methods = ['GET', 'POST'])
def dashboard():
	#helper function to connect to DBs, parse or mongo. also does the action
	#looks neater?
	def db_action(db_connection, action, data = None):
		pass
	
	#somehow needs to check if anyone has updated since
		
	database_host = '0.0.0.0'
	system_name = request.args.get('system') #find way to streamline this rather than query
	
	#can i just do 1 form instead and have js fill the form for the
	#master switch?
	master_ctrl_form = MasterControlsForm()
	ctrl_form = ControlsForm()
	comment_form = CommentForm()
	
	#very inefficient loops? maybe do this once and store in session?
	for db in app.config['ORGANIZATION_DBS']:
		if db['DBALIAS'] == session['organization']:
			database_host = db['HOST']
	
	db_connection = organization_dbs[session['organization']]
	
	#init controls via js or via backend? both? js for now since
	#i already have code for it.
	# db_connection['controls'].find()
	
	if request.method == 'POST':
		utc_datetime = datetime.utcnow()
		
		if request.args.get('form') == 'master':
			if master_ctrl_form.master_switch.data == "off":
				#need to generate dynamically somehow. for now
				#using static params for nick's aquarium
				ctrl_params = {
					"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
					"unixtime": int(utc_datetime.strftime('%s')),
					"submittedBy": session['name'],
					"username": session['username'],
					"instructionStream": {"pump": "off", "light": "off"}
				}
				db_connection[system_name]['controls'].insert(ctrl_params)
			elif master_ctrl_form.master_switch.data == "on":
				ctrl_params = {
					"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
					"unixtime": int(utc_datetime.strftime('%s')),
					"submittedBy": session['name'],
					"username": session['username'],
					"instructionStream": {"pump": "on", "light": "on"}
				}
				db_connection[system_name]['controls'].insert(ctrl_params)

			#for notifications
			#there has to be a neater way:
			#find a dynamic way to generate the keys for the action dict
			notif_params = {
				"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
				"unixtime": int(utc_datetime.strftime('%s')),
				"submittedBy": session['name'],
				"type": request.args.get('form'),
				"action": {"master":  master_ctrl_form.master_switch.data}
			}
			db_parse = connectParse()
			db_parse.request('POST', '/1/classes/notifications', json.dumps(notif_params), {
				"X-Parse-Application-Id": app.config['PARSE_ID'],
				"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
				"Content-Type": "application/json"
			})
		elif request.args.get('form') == 'controls':
			ctrl_params = {
				"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
				"unixtime": int(utc_datetime.strftime('%s')),
				"submittedBy": session['name'],
				"username": session['username'],
				"instructionStream": {"pump": ctrl_form.pump_switch.data, "light": ctrl_form.light_switch.data}
			}
			db_connection[system_name]['controls'].insert(ctrl_params)

			#for notifications
			#there has to be a neater way:
			#find a dynamic way to generate the keys for the action dict
			notif_params = {
				"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
				"unixtime": int(utc_datetime.strftime('%s')),
				"submittedBy": session['name'],
				"type": request.args.get('form'),
				"action": {"pump": ctrl_form.pump_switch.data, "light": ctrl_form.light_switch.data}
			}
			db_parse = connectParse()
			db_parse.request('POST', '/1/classes/notifications', json.dumps(notif_params), {
				"X-Parse-Application-Id": app.config['PARSE_ID'],
				"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
				"Content-Type": "application/json"
			})
		elif request.args.get('form') == 'comments':
			comment_params = {
				"comment": comment_form.comment.data,
				"author": session['name'],
				"username": session['username'],
				"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
				"unixtime": int(utc_datetime.strftime('%s'))
			}
			db_parse = connectParse()
			db_parse.request('POST', '/1/classes/comments', json.dumps(comment_params), {
				"X-Parse-Application-Id": app.config['PARSE_ID'],
				"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
				"Content-Type": "application/json"
			})

			#for notifications
			#there has to be a neater way:
			#find way to store and display comment content
			notif_params = {
				"datetime": utc_datetime.strftime("%A, %d. %B %Y %I:%M%p"),
				"unixtime": int(utc_datetime.strftime('%s')),
				"submittedBy": session['name'],
				"type": request.args.get('form'),
				"action": {"null": "null"} #for DB consistency
			}
			db_parse.request('POST', '/1/classes/notifications', json.dumps(notif_params), {
				"X-Parse-Application-Id": app.config['PARSE_ID'],
				"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
				"Content-Type": "application/json"
			})
	
	return render_template('dashboard.html', title = 'Dashboard', pageid = 'dashboard', system = system_name, host = database_host, mastercontrolsform = master_ctrl_form, controlsform = ctrl_form, commentform = comment_form)

@app.route('/charts')
@login_required
def charts():
	database_host = '0.0.0.0'
	system_name = request.args.get('system') #find way to streamline this
	
	#very inefficient loop? maybe do this once and store in session?
	for db in app.config['ORGANIZATION_DBS']:
		if db['DBALIAS'] == session['organization']:
			database_host = db['HOST']
	
	return render_template('charts.html', title = 'Charts', pageid = 'charts', host = database_host, system = system_name)
		
@app.route('/maps')
@login_required
def maps():
	#right now, a static list of systems is used to
	#plot the points on the map. need to eventually pull 
	#from db instead when systems start to populate.
	
	#if the maps is to display live data, we will need
	#ajax. ajax also to progressively load points as user
	#moves map. math needed!
	
	#imagine 1000 systems, thus 1000 points. needs a filter
	#option.
	
	return render_template('maps.html', title = 'Maps', pageid = 'maps', systems = app.config['SYSTEM_LOCS'])

@app.route('/appendsystem', methods = ['GET', 'POST'])
def appendSystem():
	if request.method == 'GET':
		#left somehow for authentication for legitimacy of system.
		doStuff = 1
	elif request.method == 'POST':
		db_parse = connectParse()
		db_parse.request('POST', '/1/classes/PendingSystems', request.body, {
			"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
			"Content-Type": "application/json"
		})
		db_parse.getresponse().read()
	
	return "Pass"

@app.route('/pendingsystems')
@login_required
def pendingSystems():
	query = urllib.urlencode({"limit":10,"skip":0})
	db_parse = connectParse()
	db_parse.request('GET', '/1/classes/PendingSystems?%s' % query, '', {
		"X-Parse-Application-Id": app.config['PARSE_ID'],
			"X-Parse-REST-API-Key": app.config['PARSE_API_KEY'],
	})
	result = json.loads(db_parse.getresponse().read())
	
	system_list = []
	for system in result['results']:
		system_list.append(system) 
	
	return render_template('pendingsystems.html',
        title = 'Pending Systems',
		systems = system_list)

@app.route('/_load_pending_systems')
def _load_pending_systems():
	#What if a new system was added before we loaded more? Can't use skip method?
	#Maybe use $gte query for timestamp?
	query = urllib.urlencode({"limit": 10, "skip": request.args.get('skip', 0, type=int)}) #loads 10 systems max per call
	db_parse = connectParse()
	db_parse.request('GET', '/1/classes/PendingSystems?%s' % query, '', {
		"X-Parse-Application-Id": app.config['PARSE_ID'],
		"X-Parse-REST-API-Key": app.config['PARSE_API_KEY']
	})
	result = json.loads(db_parse.getresponse().read())
	
	system_list = []
	for system in result['results']:
		system_list.append(system) 
	
	return jsonify(systems = system_list) #wrong syntax? can i not use the jsonify lib?

@app.route('/registersystem/<systemName>')
@login_required
def registerSystem():
	return 'PASS'

@app.route('/logout')
def logout():
	#try to batch this
	session.pop('id', None)
	session.pop('organization', None)
	session.pop('name', None)
	session.pop('username', None)
	logout_user()
	return redirect(url_for('login'))

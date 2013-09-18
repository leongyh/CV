from flask.ext.wtf import Form, TextField, TextAreaField, BooleanField, SelectField, HiddenField, PasswordField, DecimalField
from flask.ext.wtf import Required
from app import app

class LoginForm(Form):
	username = TextField('username', validators = [Required()])
	password = PasswordField('password', validators = [Required()])
	remember_me = BooleanField('remember_me', default = False)
	
class RegistrationForm(Form):
	org_choices = []
	
	for orgs in app.config['ORGANIZATION_DBS']:
		org_choices.append((orgs['DBALIAS'], orgs['DBNAME']))
	
	username = TextField('username', validators = [Required()])
	password = PasswordField('password', validators = [Required()])
	password_repeat = PasswordField('password_repeat', validators = [Required()])
	firstName = TextField('firstName', validators = [Required()])
	lastName = TextField('lastName', validators = [Required()])
	email = TextField('email', validators = [Required()])
	organization = SelectField('organization', choices = org_choices, validators = [Required()]) #SelectField to hold better constraints?
	title = TextField('title', validators = [Required()])
	phone = TextField('phone', validators = [])

class SystemRegistrationForm(Form):
	system_name = TextField('system_name', validators = [Required()])
	location = TextField('location', validators = [Required()])
	longitude = DecimalField()
	latitude = DecimalField()
	mac_address = TextField('mac_address', validators = [Required()])
	ip_address = TextField('ip_address', validators = [Required()])
	date_requested = TextField('date_requested', validators = [Required()])
	
	#Add dynamic fields below for sensors

class MasterControlsForm(Form):
	master_switch = SelectField(label = "Master Switch", id = 'master_switch', choices = [('off', 'Off'), ('on', 'On')], validators = [Required()])

class ControlsForm(Form):
	#eventually needs to be dynamically generated based on the
	#parameters the system accepts. for now static just for demo sake.
	
	#need to decide and think on how these fields are generated
	#dynamically. based on what data?
	light_switch = SelectField(label = "Light Switch", id = 'light_switch', choices = [('off', 'Off'), ('on', 'On')], validators = [Required()])
	pump_switch = SelectField(label = "Pump Switch", id = 'pump_switch', choices = [('off', 'Off'), ('on', 'On')], validators = [Required()])

class CommentForm(Form):
	comment = TextAreaField(label = 'Comments', id = 'comments')

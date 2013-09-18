from flask import Flask
from flask.ext.login import LoginManager
from config import basedir, ORGANIZATION_DBS

import pymongo

import os, httplib

#app init
application = Flask(__name__)
app = application #AWS Elastic Beanstalk requirement

app.config.from_object('config')

# DB connection to system DBs
organization_dbs = {}
for orgs in ORGANIZATION_DBS:
	organization_dbs[orgs['DBALIAS']] = pymongo.MongoClient(orgs['HOST'], 27017)

#login manager
lm = LoginManager()
lm.init_app(app)
lm.login_view = 'login'

#Parse.com connection.
#can i only connect once and not everytime i need to do an action? 
#didn't work on first attempt
db_parse = httplib.HTTPSConnection('api.parse.com', 443)
db_parse.connect()

from app import views, models

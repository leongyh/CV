# Create your views here.
import ast, datetime
from string import atoi

from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from HardwareAPI import idGen, sendSMS
from HardwareAPI.models import *
from HardwareAPI.forms import *

from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from django.db import IntegrityError
from django.core.exceptions import ValidationError
from django.utils.timezone import utc

#dangerous!
@csrf_exempt #dont use this in production!
def createSystem(request):
	if request.method == 'POST':
		form = SystemRegistrationForm(request.POST)

		if form.is_valid():
			system_id = form.cleaned_data['system_id']
			secret_key = idGen(20)
			
			system = System(system_id=system_id, secret_key=secret_key)

			try:
				system.save()
			except IntegrityError:
				#raises this if id and key is not unique
				return HttpResponse('System ID not unique')

			return HttpResponse('Secret key is %s' % secret_key) # Redirect after POST
	else:
		form = SystemRegistrationForm()

	context = {'form': form}

	return render(request, 'create.html', context)

@csrf_exempt
def twilio(request):
	POST = request.POST
	message = POST['Body'][2:]
	number = POST['From']

	if POST['Body'][0] == 'r':
		return systemRegisterHandler(message,number)

	elif POST['Body'][0] == 'd':
		return dataHandlerSD(message, number)

	else:
		sendSMS("Error", number)
		return HttpResponse("Error")

@csrf_exempt
def viewData(request):
	if request.method == 'GET':
		form = SystemRequestForm()
		context = {'form': form}

		return render(request, 'view.html', context)

	elif request.method == 'POST':
		form = SystemRequestForm(request.POST)
		system_id = form.cleaned_data['system_id']
		system = System.objects.get(system_id=system_id)

		context = {'form': form}

		return render(request, 'view.html', context)

#-------------Handler Methods---
def systemRegisterHandler(message, number):
	print("before:")
	print(message + "|")
	
	#change '(' and ')' to '{' and '}'as arduino cant support '{' or '}'
	message = '{' + str(message[1:-1]) + '}'

	print("after:")
	print(message + "|")

	data = ast.literal_eval(message)
	data['sim_number'] = number

	requesting_system = System.objects.get(system_id=data['system_id'])

	try:
		requesting_system.activate(data['secret_key'])
		requesting_system.save()
	except ValidationError:
		#already activated. add number to blacklist?
		registrationSuccess(False, data['system_id'], data['secret_key'], number)
		return HttpResponse("failed to authenticate")

	#super hacky
	del data['system_id']
	del data['secret_key']

	data['latitude'] = latConvert(data)
	data['longitude'] = lonConvert(data)

	system = SystemDetail(system=requesting_system, **data)
	
	try:
		system.save()
		registrationSuccess(True, data['system_id'], data['secret_key'], number)
		return HttpResponse("Success")
	except IntegrityError:
		registrationSuccess(False, data['system_id'], data['secret_key'], number)
		return HttpResponse("Error")

def dataHandlerEEPROM(message, number):
	#change '(' and ')' to '{' and '}'as arduino cant support '{' or '}'
	message_split = message.split(".")
	message_data = message_split[1].replace('(', '[').replace(')', ']')
	reference_day = atoi(message_split[0])
	
	data = ast.literal_eval(message_data)
	system = SystemDetail.objects.get(sim_number=number)
	count = 0
	
	for pulse in data:
		try:
			point = DataPoint(system=system.system, date=dateCalculatorEEPROM(reference_day, count), flow=pulseToLiter(pulse, system.meter_conv))
			point.save()
			count += 1
		except Exception as e:
			dataSuccess(False, count, reference_day, system['secret_key'], number)
			return HttpResponse("Failed")

	dataSuccess(True, count, reference_day, system['secret_key'], number)

	return HttpResponse("Success")

def dataHandlerSD(message, number):
	#change '(' and ')' to '{' and '}'as arduino cant support '{' or '}'
	data_set = message.split("|")
	
	data = ast.literal_eval(message_data)
	system = SystemDetail.objects.get(sim_number=number)
	
	for data in data_set:
		try:
			values = data.split(",")
			point = DataPoint(system=system.system, date_start=dateCalculatorSD(values[0]), date_start=dateCalculatorSD(values[1]), flow=pulseToLiter(values[2], system.meter_conv))
			point.save()
		except Exception as e:
			dataSuccess(False, count, reference_day, system['secret_key'], number)
			return HttpResponse("Failed")

	dataSuccess(True, count, reference_day, system['secret_key'], number)

	return HttpResponse("Success")

#-------------Helper Methods---
#Returns a datetime.date object of days after 1 Jan. 2012
def dateCalculatorEEPROM(days, count):
	dt = (datetime.datetime.fromtimestamp(days * 60) + datetime.timedelta(minutes=count))
	dt.replace(tzinfo=utc)
	
	return dt

def dateCalculatorSD(seconds):
	dt = datetime.datetime.fromtimestamp(seconds)
	dt.replace(tzinfo=utc)
	
	return dt

#Converts pulses to liter based on meter conversion factor and returns a float
def pulseToLiter(pulses, factor):
	return pulses / 60 / factor

def latConvert(data):
	lat_split = data['latitude'].split(".")
	lat_split[1], direction = lat_split[1][:-1], lat_split[1][-1]

	d = atoi(lat_split[0][:2])
	m = atoi(lat_split[0][2:])
	s = atoi(lat_split[1])

	lat = d + (m * 60 + s) / 3600

	if direction == 'S':
		lat = lat * -1

	return lat

def lonConvert(data):
	lon_split = data['longitude'].split(".")
	lon_split[1], direction = lon_split[1][:-1], lon_split[1][-1]

	d = atoi(lon_split[0][:3])
	m = atoi(lon_split[0][3:])
	s = atoi(lon_split[1])

	lon = d + (m * 60 + s) / 3600 

	if direction == 'W':
		lon = lon * -1

	return lon

def registrationSuccess(isSuccess, system_id, secret_key, number):
	message = system_id + "," + secret_key

	if isSuccess:
		message += "," + "Success"

	else:
		message += "," + "Failure"

	sendSMS(message, number)

	return isSuccess

def dataSuccess(isSuccess, data_length, start_index, secret_key, number):
	message = data_length + "," + start_index + "," + secret_key

	if isSuccess:
		message += "," + "Success"

	else:
		message += "," + "Failure"

	sendSMS(message, number)

	return isSuccess
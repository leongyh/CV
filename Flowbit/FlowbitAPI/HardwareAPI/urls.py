from django.conf.urls import patterns, include, url
from HardwareAPI import views

urlpatterns = patterns('',
	url(r'^(?i)create/$', views.createSystem, name='create'),
	url(r'^(?i)twilio/$', views.twilio, name='twilio'),
	url(r'^(?i)view/$', views.viewData, name='view'),
)

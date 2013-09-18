from django.contrib import admin
from HardwareAPI.models import *


#find way to register all non abstract models
admin.site.register(System)
admin.site.register(SystemDetail)
admin.site.register(DataPoint)
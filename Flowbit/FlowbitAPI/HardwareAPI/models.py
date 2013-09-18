from django.db import models
from django.core.exceptions import ValidationError

# Create your models here.

#System Related -------------------------------------------
class System(models.Model):
	system_id = models.CharField(max_length=20, unique=True)
	secret_key = models.CharField(max_length=20, unique=True)

	activated = models.BooleanField(default=False);

	def activate(self, key):
		if self.activated:
			raise ValidationError('Already activated!')
		else:
			if key == self.secret_key:
				self.activated = True
			else:
				raise ValidationError('Bad key!')


class SystemDetail(models.Model):
	system = models.ForeignKey(System, primary_key=True)
	
	latitude = models.DecimalField(max_digits=8, decimal_places=5)
	longitude = models.DecimalField(max_digits=8, decimal_places=5)
	sim_number = models.CharField(max_length=15, unique=True)

	meter_size = models.CharField(max_length=10)
	meter_conv = models.DecimalField(max_digits=8, decimal_places=3)


#Data Related----------------------------------------------
class DataPoint(models.Model):
	system = models.ForeignKey('System')

	date = models.DateTimeField()
	flow = models.FloatField()

	class Meta:
		order_with_respect_to = 'system'

class DataPointFlow(models.Model):
	system = models.ForeignKey('System')

	date_start = models.DateTimeField()
	date_end = models.DateTimeField()
	flow = models.FloatField()

	class Meta:
		order_with_respect_to = 'system'
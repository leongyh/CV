from django import forms

class SystemRegistrationForm(forms.Form):
	system_id = forms.CharField(max_length=20)


class SystemRequestForm(forms.Form):
	system_id = forms.CharField(max_length=20)
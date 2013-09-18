import string, random

from twilio.rest import TwilioRestClient

account_sid = "AC32a3c49700934481addd5ce1659f04d2"
auth_token  = "0329d2504d6b45722a62f7ac427eee50"
client = TwilioRestClient(account_sid, auth_token)

def idGen(size, chars = string.ascii_uppercase + string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))

#send SMS only from 1 number. need multiple. load balancer?
def sendSMS(message, number):
	client.sms.messages.create(body=message, to=number, from="+15104021337")

	return
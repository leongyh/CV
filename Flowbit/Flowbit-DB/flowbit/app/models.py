class User(dict):
	def __init__(self, user):
		self.username = user['username']
		self.firstName = user['firstName']
		self.lastName = user['lastName']
		self.email = user['email']
		self.organization = user['organization']
		self.title = user['title']
		self.phone = user['phone']
		
		self.id = user['objectId']
	
	#All 4 attributes below are required by the LoginManager	
	def is_authenticated(self):
		return True #True for now. Needs an 'authenticated' attribute later
		
	def is_active(self):
		return True
	
	def is_anonymous(self):
		return False

	def get_id(self):
		return unicode(self.id) #Requires to be returned in unicode

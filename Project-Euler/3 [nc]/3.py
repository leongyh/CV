def largestPrimeFactor(val):
	

def egcd(x,y):
	if (y == 0):
		return [x, 1, 0]
	else:
		vals = egcd(y, x%y)
		return [vals[0], vals[2], vals[1] - (x // y) * vals[2]]

def isPrime(val):
	
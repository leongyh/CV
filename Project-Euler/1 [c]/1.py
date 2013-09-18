from operator import add
from functools import reduce

def natSum(max_val):
	s = set()
	for i in range(0, max_val // 3 + 1):
		s.add(3*i)
		s.add(5*i)

	return reduce(add, filter(lambda x: x < max_val, s))

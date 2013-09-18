def fibEvenSum(max_val, num_prev, num, _sum):
	if num % 2 == 0:
		_sum += num

	if num + num_prev < max_val:
		fibEvenSum(max_val, num, num + num_prev, _sum)
	else:
		print(_sum)
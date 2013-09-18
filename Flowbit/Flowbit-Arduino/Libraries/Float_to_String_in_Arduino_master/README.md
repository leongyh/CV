Float-to-String-in-Arduino
==========================================

A simple patch that allows you to do String to Float and Float to String in Arduino.

Overwriting to your ``\Arduino\hardware\arduino\cores\arduino\`` folder.

Note: Everytime you update your IDE you will lose this patch.

一个简单的补丁，让你能在 Arduino IDE 中自由转换浮点数和字符串。

覆盖到 ``\Arduino\hardware\arduino\cores\arduino\`` 文件夹。

注：每次更新 IDE 后需要重新覆盖补丁。


Examples:

	String example1 = String(1.852); // = "1.852000"
		   example1 = String(1.852, 2); // = "1.85"
	String example2 = String(example1.toFloat() * 1.784561, 4); // = "3.3014"
		   example1 = example2 + 1.58356; // = "3.30141.583560";
import json,httplib,urllib

user = raw_input("Username: ")
password = raw_input("Password: ")

connection = httplib.HTTPSConnection('api.parse.com', 443)
params = urllib.urlencode({"username": user,"password": password})
connection.connect()
connection.request('GET', '/1/login?%s' % params, '', {
       "X-Parse-Application-Id": "7h8TXODIXq3dxx0yRk5TY1CRALyDcwjwdHYzLi7Q",
       "X-Parse-REST-API-Key": "iy5G8coOQTsKlfqaGQgkN0f1cQnQyEtbLUN7PcuN"
     })
result = json.loads(connection.getresponse().read())
print(result)
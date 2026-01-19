import os
import ujson

class FileManager:
    def list_files(self):
        try:
            return os.listdir()
        except Exception as e:
            return ["error: " + str(e)]

    def read_file(self, filename):
        try:
            with open(filename, 'r') as f:
                return f.read()
        except Exception as e:
            return "error: " + str(e)

    def write_file(self, filename, content):
        try:
            with open(filename, 'w') as f:
                f.write(content)
            return "ok"
        except Exception as e:
            return "error: " + str(e)

    def delete_file(self, filename):
        try:
            os.remove(filename)
            return "ok"
        except Exception as e:
            return "error: " + str(e)

    def get_info(self, filename):
        try:
            stat = os.stat(filename)
            return {"size": stat[6]} # Size in bytes
        except:
            return {"size": -1}

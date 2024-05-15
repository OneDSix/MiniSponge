import typing, os, shutil

shadow_jar: str = ".\\build\\libs\\minimods-1.0.0.jar"
game_jar: str = "..\\core\\build\\libs\\core-d1.1.0.jar"
build_path: str = ".\\build\\distributable\\"

def checkIfExists(path: str) -> str:
	if not os.path.exists(path):
		inp: str = input(
			f"The jar {path} does not exist. Input a valid filepath or hit enter to exit.\n> ")
		if inp == "":
			os._exit(0)
		else:
			return checkIfExists(path)
	else:
		return path

if __name__ == "__main__":
	shadow_jar = checkIfExists(shadow_jar)
	print(f"{shadow_jar} exists.")
	game_jar = checkIfExists(game_jar)
	print(f"{game_jar} exists.")

	if not os.path.exists(build_path):
		os.mkdir(build_path)
		print(f"{build_path} created.")
	else:
		print(f"{build_path} exists.")


	shutil.copy(shadow_jar, build_path)
	print(f"{shadow_jar} copied.")
	shutil.copy(game_jar, build_path)
	print(f"{game_jar} copied.")

	YN: str = input(f"Done! Would you like to run {shadow_jar}? y/n\n> ")
	if YN.startswith("y"):
		os.run()

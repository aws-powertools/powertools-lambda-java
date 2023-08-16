import subprocess
import sys

if __name__ == '__main__':

    version = sys.argv[1]
    hash = sys.argv[2]


    print(version)
    print(hash)

    git_checkout = f"git checkout -f {hash}"
    mike_deploy = f"mike deploy --update-aliases {version} latest"

    message = subprocess.check_output(git_checkout, shell=True, universal_newlines=True)
    print(message)

    with open('mkdocs.yml', 'r') as file :
      filedata = file.read()

    # Replace the target string

    if "extra:" in filedata:
        filedata = filedata.replace('extra:', 'extra:\n  version:\n    provider: mike\n    default: latest')
    else:
        filedata = filedata + "\n\nextra:\n  version:\n    provider: mike\n    default: latest"

    # Write the file out again
    with open('mkdocs.yml', 'w') as file:
      file.write(filedata)

    message = subprocess.check_output(mike_deploy, shell=True, universal_newlines=True)
    print(message)
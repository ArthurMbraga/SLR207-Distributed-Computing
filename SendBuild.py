import getpass
import os
import paramiko
from paramiko import SSHClient
from scp import SCPClient

# Prompt for the login
login = input("Enter login: ")

# Prompt for the password securely
password = getpass.getpass("Enter password for {}: ".format(login))

node_project_name = "server"
master_project_name = "master"
file_suffix = "-1.0-jar-with-dependencies.jar"
remote_folder = "/tmp/{}/".format(login)

# List of computers
computers = ["tp-1a201-37", "tp-1a222-05", "tp-1a226-24",
             "tp-1a226-28", "tp-1a222-11", "tp-1a226-00", "tp-1a252-12", "tp-1a252-13"]
master = "tp-1a226-21"

# Clear the terminal
os.system("clear")

allComputers = computers + [master]

for c in allComputers:
    try:
        ssh = SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(c, username=login, password=password)

        ssh.exec_command("pkill -u {}".format(login))

        ssh.connect(c, username=login, password=password)
        # Remove and recreate the remote folder
        ssh.exec_command("rm -rf {}".format(remote_folder))
        ssh.exec_command("mkdir -p {}".format(remote_folder))

        project_name = master_project_name if c == master else node_project_name

        # Copy the jar file to the remote folder
        with SCPClient(ssh.get_transport()) as scp:
            scp.put("./{}/target/{}{}".format(project_name, project_name, file_suffix),
                    "{}{}{}".format(remote_folder, project_name, file_suffix))

        if c != master:
            print("Successfully deployed on {}".format(c))
            ssh.exec_command(
                "cd {}; java -jar {}{}".format(remote_folder, project_name, file_suffix))

        else:
            print("Deploying master")
            # if is master add a small delay to ensure that all nodes are ready
            ssh.exec_command("sleep 1")

            # Attach the terminal to the process and print all output until java process ends
            for i in range(0, len(computers)):
                stdin, stdout, stderr = ssh.exec_command(
                    "cd {}; java -jar {}{} {}".format(
                        remote_folder, project_name, file_suffix, ",".join(computers[:(i + 1)])))
                for line in stdout:
                    print(line, end="")
                for line in stderr:
                    print(line, end="")

    except paramiko.AuthenticationException:
        print("Authentication failed for {}".format(c))
    except paramiko.SSHException as ssh_exception:
        print("SSH connection failed for {}: {}".format(c, str(ssh_exception)))

    finally:
        ssh.close()

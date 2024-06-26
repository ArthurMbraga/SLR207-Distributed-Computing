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
computers = ["tp-1a226-24", "tp-1a201-37", "tp-1a207-30", "tp-1a226-03",
             "tp-1a201-02", "tp-1a201-01", "tp-1a201-07", "tp-1a226-28",
             "tp-1a201-05", "tp-1a201-18", "tp-1a201-08", "tp-1a201-09",
             "tp-1a207-31", "tp-1a207-32", "tp-1a207-33", "tp-1a207-35",
             "tp-1a207-36", "tp-1a207-37",
             ]
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

            # Save results.csv file to disk and delete old results.csv
            metrics_file_name = "results.csv"
            with SCPClient(ssh.get_transport()) as scp:
                scp.get("{}{}".format(remote_folder, metrics_file_name), "./")
            ssh.exec_command("rm -rf {}".format(remote_folder))

    except paramiko.AuthenticationException:
        print("Authentication failed for {}".format(c))
    except paramiko.SSHException as ssh_exception:
        print("SSH connection failed for {}: {}".format(c, str(ssh_exception)))

    finally:
        ssh.close()

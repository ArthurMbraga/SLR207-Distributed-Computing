import getpass
import os
import paramiko
from paramiko import SSHClient
from scp import SCPClient

# Prompt for the login
login = input("Enter login: ")

# Prompt for the password securely
password = getpass.getpass("Enter password for {}: ".format(login))

metrics_file_name = "results.csv"
node_project_name = "server"
master_project_name = "master"
file_suffix = "-1.0-jar-with-dependencies.jar"
remote_folder = "/tmp/{}/".format(login)

# List of computers
computers = [
    "tp-1a201-01", "tp-1a201-03", "tp-1a201-04", "tp-1a201-16", "tp-1a201-17",
    "tp-1a201-18", "tp-1a201-19", "tp-1a201-20", "tp-1a201-21", "tp-1a201-23",
    "tp-1a201-25", "tp-1a201-26", "tp-1a201-27", "tp-1a201-28", "tp-1a201-29",
    "tp-1a201-30", "tp-1a201-31", "tp-1a201-32", "tp-1a201-33", "tp-1a201-34",
    "tp-1a201-36", "tp-1a201-37", "tp-1a201-38", "tp-1a201-39", "tp-1a207-01",
    "tp-1a207-02", "tp-1a207-03", "tp-1a207-04", "tp-1a207-05", "tp-1a207-06",
    "tp-1a207-07", "tp-1a207-08", "tp-1a207-11", "tp-1a207-12", "tp-1a207-13",
    "tp-1a207-14", "tp-1a207-15", "tp-1a207-19", "tp-1a207-17", "tp-1a207-18",
]
master = "tp-m5-09"

# Clear the terminal
os.system("clear")

allComputers = computers + [master]

# Check if there is some repeated computer
if len(allComputers) != len(set(allComputers)):
    print("There are repeated computers")
    exit(0)

# Attach the terminal to the process and print all output until java process ends


def attach_and_run_master(ssh, ips, amount_of_data):
    stdin, stdout, stderr = ssh.exec_command(
        "cd {}; java -Xms8g -Xmx10g -jar {}{} {} {}".format(
            remote_folder, master_project_name, file_suffix, ",".join(ips), amount_of_data))

    # Print all output and error messages
    count = 0
    while not stdout.channel.exit_status_ready():
        for line in stdout:
            count += 1
            print(line, end="")
            if (count > 10000):
                break

        for line in stderr:
            count += 1
            print(line, end="")
            if (count > 10000):
                break

    for line in stderr:
        print(line, end="")

    # Update local metrics file
    with SCPClient(ssh.get_transport()) as scp:
        scp.get("{}{}".format(
            remote_folder, metrics_file_name), "./")


index = -1
for c in allComputers:
    try:
        index += 1
        ssh = SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(c, username=login, password=password)

        ssh.exec_command("pkill -u {}".format(login))

        ssh.connect(c, username=login, password=password)

        project_name = master_project_name if c == master else node_project_name

        # Remove and recreate the remote folder
        ssh.exec_command("rm -rf {}".format(remote_folder))
        ssh.exec_command("mkdir -p {}".format(remote_folder))

        # Copy the jar file to the remote folder
        with SCPClient(ssh.get_transport()) as scp:
            scp.put("./{}/target/{}{}".format(project_name, project_name, file_suffix),
                    "{}{}{}".format(remote_folder, project_name, file_suffix))

        if c != master:
            ssh.exec_command(
                "cd {}; java -Xms8g -Xmx10g -jar {}{}".format(remote_folder, project_name, file_suffix))
            print("Successfully deployed on {} ({})".format(c, index))

        else:
            print("Deploying master")
            # if is master add a small delay to ensure that all nodes are ready
            ssh.exec_command("sleep 1")

            # Varying number of nodes
            for v in [0.1, 0.05, 0.01]:
                for i in range(1, len(computers)):
                    attach_and_run_master(ssh, computers[:i], v)

            # Varying amount of data
            for percentage in range(1, 11):
                print("Running with {}% of the data".format(percentage*10))
                attach_and_run_master(
                    ssh, computers[:len(computers)//3], percentage/10)

            # delete old results.csv
            ssh.exec_command("rm -rf {}".format(remote_folder))

    except paramiko.AuthenticationException:
        print("Authentication failed for {}".format(c))
        break
    except paramiko.SSHException as ssh_exception:
        print("SSH connection failed for {}: {}".format(c, str(ssh_exception)))
    finally:
        ssh.close()


# Kill all processes
for c in allComputers:
    try:
        ssh = SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(c, username=login, password=password)
        ssh.exec_command("pkill -u {}".format(login))
    except paramiko.AuthenticationException:
        print("Authentication failed for {}".format(c))
        break
    except paramiko.SSHException as ssh_exception:
        print("SSH connection failed for {}: {}".format(c, str(ssh_exception)))
    finally:
        ssh.close()

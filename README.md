To successfully build, execute, and deploy this distributed computing project, follow the steps below:

### Prerequisites

- Ensure that Maven (`mvn`) and Python are installed on your local machine for building and deploying the project.

### Building the Project

1. **Compile the Projects:**
   Navigate to the directory that contain all projects (`core`,`master`,`server`) and run the Maven command to build the project. This command will compile all projects with all dependencies included.

   ```sh
   mvn clean compile assembly:single
   ```

### Preparing for Execution and Deployment

2. **Login Credentials in `SendDeploy.py`:**
   In the `SendDeploy.py` script, the login and password are prompted at runtime. Therefore, you do not need to change the script for different logins.

3. **Update Machine Addresses:**
   In the same `SendDeploy.py` script, update the `computers` list and the `master` variable with the addresses of the new target machines. This list is used to deploy the project across multiple machines.

   ```py
   computers = [
       "new-machine-1", "new-machine-2", # Add more as needed
   ]
   master = "new-master-machine"
   ```

### Deploying the Project

4. **Deploy to Machines:**
   Run the [`SendDeploy.py`](SendDeploy.py) script. This script automates the process of deploying the JAR files to the specified machines. It will:
   - Kill any existing instances of the project.
   - Clear the deployment directory.
   - Copy the new JAR files to the target machines.
   - Execute the project on each machine.
   - Prompt the output of the master machine.
   - Collect the metrics generated from the master machine.

   Before running the script, ensure you are connected to the network that the target machines are on (e.g., a VPN if required).

   ```sh
   python SendDeploy.py
   ```

   Enter your login and password when prompted. The script will handle the deployment based on the provided credentials and machine addresses.

### Running manually the Project

5. **Execute the Project:**
   The execution of the project on the master and server machines is handled by the `SendDeploy.py` script as part of the deployment process. However, if you need to manually run the project, use the following commands on each machine:

   - **For the Master Machine:**
     ```bash
     java -jar /dev/shm/your-login/master-1.0-jar-with-dependencies.jar
     ```

   - **For Server Machines:**
     ```bash
     java -jar /dev/shm/your-login/server-1.0-jar-with-dependencies.jar
     ```

   Replace `your-login` with your actual login on the target machines.

### Notes

- The target machines must have Java installed to run the JAR files.
- Adjust firewall settings or network permissions as necessary to allow communication between the master and server machines.

By following these instructions and making the necessary adjustments, you should be able to build, execute, and deploy the project in any environment.
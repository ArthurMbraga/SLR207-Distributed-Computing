To compile the projects:

```
mvn clean compile assembly:single
```

To run the project in a télécom machine:

fist, connected to Télécom VPN, check the available machines in this web site: https://tp.telecom-paris.fr/ 
Then, you need to copy the jar file to the télécom machine.


for the master machine:

```bash
scp -P 2222 master/target/master-1.0-jar-with-dependencies.jar user@telecom-machine:/dev/shm/your-login
```

for the server machines:

```bash
scp -P 2222 server/target/server-1.0-jar-with-dependencies.jar user@telecom-machine:/dev/shm/your-login
```

Then, you can run the project using the following command:

```bash
# For master
java -jar /dev/shm/your-login/master-1.0-jar-with-dependencies.jar

# For server
java -jar /dev/shm/your-login/server-1.0-jar-with-dependencies.jar
```

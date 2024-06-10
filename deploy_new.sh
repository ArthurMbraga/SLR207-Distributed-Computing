#!/bin/bash

# Prompt for the login
read -p "Enter login: " login

# Prompt for the password securely
echo -n "Enter password for $login: "
read -s password
echo

localFolder="./"
todeploy="server/target"
remoteFolder="/dev/shm/$login"

# List of computers
computers=("tp-1a201-17" "tp-1a201-18" "tp-1a201-19")

# skip host key checking MAJOR SECURITY RISK :-DD
sshopts="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

remoteFolder="/tmp/$login/"
cd server
mvn clean compile assembly:single
cd ..
fileName="server-1-jar-with-dependencies"
fileExtension=".jar"


for c in ${computers[@]}; do
  command0="sshpass -p '$password' ssh $sshopts $login@$c pkill -u $login"
  command1="sshpass -p '$password' ssh $sshopts $login@$c 'rm -rf $remoteFolder;mkdir $remoteFolder'"
  command2="sshpass -p '$password' scp $sshopts server/target/$fileName$fileExtension $login@$c:$remoteFolder$fileName$fileExtension"
  echo ${command0[*]}
  eval $command0
  echo ${command1[*]}
  eval $command1
  echo ${command2[*]}
  eval $command2
  command3="sshpass -p '$password' ssh $sshopts $login@$c 'cd $remoteFolder; java -jar $fileName$fileExtension'"
  echo ${command3[*]}
  eval $command3 &
done
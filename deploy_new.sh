#!/bin/bash

# Prompt for the login
read -p "Enter login: " login

# Prompt for the password securely
echo -n "Enter password for $login: "
read -s password
echo

filePrefix="server"
fileSufix="-1-jar-with-dependencies.jar"
remoteFolder="/dev/shm/$login"

# List of computers
computers=("tp-1a201-17" "tp-1a201-18" "tp-1a201-19")

# skip host key checking MAJOR SECURITY RISK :-DD
sshopts="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

mvn clean compile assembly:single

for c in ${computers[@]}; do
  command0="sshpass -p '$password' ssh $sshopts $login@$c pkill -u $login"
  command1="sshpass -p '$password' ssh $sshopts $login@$c 'rm -rf $remoteFolder;mkdir $remoteFolder'"
  command2="sshpass -p '$password' scp $sshopts $filePrefix/target/$filePrefix$fileSufix $login@$c:$remoteFolder$filePrefix$fileSufix"
  echo ${command0[*]}
  eval $command0
  echo ${command1[*]}
  eval $command1
  echo ${command2[*]}
  eval $command2
  command3="sshpass -p '$password' ssh $sshopts $login@$c 'cd $remoteFolder; java -jar $filePrefix$fileSufix'"
  echo ${command3[*]}
  eval $command3 &
done
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
nameOfTheJarToExecute="server-1-jar-with-dependencies.jar"

# List of computers
computers=("tp-1a201-17" "tp-1a201-18" "tp-1a201-19")

for c in "${computers[@]}"; do
  sshpass=("sshpass" "-p" "$password")
  # Command to kill all the user processes, remove the remote folder and create a new one
  command0=("ssh" "$login@$c" "lsof -ti | xargs kill -9 2>/dev/null; rm -rf $remoteFolder; mkdir $remoteFolder")
  
  # Command to copy the folder to the remote folder
  command1=("scp" "-r" "$localFolder$todeploy" "$login@$c:$remoteFolder")
  
  # Command to go to the remote folder, wait 3 seconds, and execute the jar
  command2=("ssh" "-tt" "$login@$c" "cd $remoteFolder; sleep 3; java -jar $remoteFolder/$nameOfTheJarToExecute")

  echo "${command0[*]}"
  "${sshpass[@]}" "${command0[@]}"
  echo "${command1[*]}"
  "${sshpass[@]}" "${command1[@]}"
  echo "${command2[*]}"
  "${sshpass[@]}" "${command2[@]}" &
  
done

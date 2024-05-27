package group;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

public class FTPServer {
    private static final int PORT = 3456;

    public FTPServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(PORT);
        serverFactory.addListener("default", listenerFactory.createListener());

        // Create a UserManager instance
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File userFile = new File("users.properties");

        if (!userFile.exists())
            createFile(userFile);

        userManagerFactory.setFile(userFile); // Specify the file to store user details
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor()); // Store plain text passwords
        UserManager userManager = userManagerFactory.createUserManager();

        // Create a user
        BaseUser user = createUser();
        String username = user.getName();
        String homeDirectory = getHomeDirectory(username);

        File directory = new File(homeDirectory); // Convert the string to a File object

        if (!directory.exists())
            createDirectory(directory);

        user.setHomeDirectory(homeDirectory);

        // Set write permissions for the user
        giveWritePermissions(user, homeDirectory);

        // Add the user to the user manager
        try {
            userManager.save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }

        // Set the user manager on the server context
        serverFactory.setUserManager(userManager);

        FtpServer server = serverFactory.createServer();

        // Start the server
        try {
            server.start();
            System.out.println("FTP Server started on port " + PORT);
        } catch (FtpException e) {
            e.printStackTrace();
        }
    }

    private static void createFile(File userFile) {
        try {
            if (userFile.createNewFile()) {
                System.out.println("File created: " + userFile.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void giveWritePermissions(BaseUser user, String homeDirectory) {
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        user.setHomeDirectory(homeDirectory);
    }

    private static void createDirectory(File directory) {
        if (directory.mkdirs()) {
            System.out.println("Directory created: " + directory.getAbsolutePath());
        } else {
            System.out.println("Failed to create directory.");
        }
    }

    private static BaseUser createUser() {
        BaseUser user = new BaseUser();
        user.setName("toto"); // Replace "username" with the desired username
        user.setPassword("tata"); // Replace "password" with the desired password
        return user;
    }

    private static String getHomeDirectory(String username) {
        String homeDirectory = "/dev/shm/braga-23/" + username;
        return homeDirectory;
    }
}
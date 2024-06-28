package group;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.ConnectionConfigFactory;
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
    private final int PORT = 3456;
    private final String HOME_DIRECTORY = "/dev/shm/braga-23";

    public FTPServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(PORT);

        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setMaxLogins(Constants.MAX_CONNECTIONS); // Max total connections
        connectionConfigFactory.setMaxThreads(Constants.MAX_CONNECTIONS); // Max connections per IP

        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
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

        File directory = new File(HOME_DIRECTORY); // Convert the string to a File object
        System.out.println(directory.exists());

        if (!directory.exists())
            createDirectory(directory);

        user.setHomeDirectory(HOME_DIRECTORY);

        // Set write permissions for the user
        giveWritePermissions(user, HOME_DIRECTORY);

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

}

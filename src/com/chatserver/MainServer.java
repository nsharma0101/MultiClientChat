
package com.chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;



public class MainServer {
    
  
  // MainServer Class Starts

    public static void main(String[] args) {
        System.out.println("S: Server Started");
        int port = 8918;
        Server server = new Server(port);
        server.start();

    }

    }
    
// MainServer Class Ends

//Server Class Starts Here

class Server extends Thread {
   private final int serverPort;
   private ArrayList<ServerWorker> workerList = new ArrayList<>();
  
    
    public Server(int serverPort) {
        this.serverPort = serverPort;       
  
    }
    public List<ServerWorker> getWorkerList(){
        return workerList;
    }
    
   @Override
    public void run(){
        
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                System.out.println("About to accept connection");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket);
               ServerWorker serverWorker = new ServerWorker(this, clientSocket);
                workerList.add(serverWorker);
                serverWorker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        
    }

   public void removeWorker(ServerWorker serverWorker) {
        workerList.remove(serverWorker);
        }
           
    
}
//Server Class Ends here

//ServerWorker Class Starts here

class ServerWorker extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream  outputStream; 

    public ServerWorker(Server server, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("Logoff".equalsIgnoreCase(cmd)|| "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handlelogin(outputStream, tokens);
                } else if ("msg".equalsIgnoreCase(cmd)){
                    String[] tokenMsg = line.split("\\s+", 3);
                    handleMessage(tokenMsg);
                }
                
                else {
                    String msg = "Unknown +" + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }
    public String getLogin(){
       return login; 
    }                
    
    private void handlelogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];
            
            if ((login.equals("guest") && password.equals("guest")) || (login.equals("neeraj") && password.equals("neeraj")) || (login.equals("admin") && password.equals("admin"))) {
                String msg = "Login Successful\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User Logged in Successfully " + login + "\n");

                List<ServerWorker> workerList = server.getWorkerList();
                //Send all other login status to current user
                for (ServerWorker serverWorker : workerList) {

                    if (serverWorker.getLogin() != null) {
                        if (!login.equals(serverWorker.getLogin())) {
                            String msg2 = "Online " + serverWorker.getLogin() + "\n";
                            send(msg2);
                        }
                    }
                }
                //Send current user login status to all other users.
                String onlineMsg = "Online " + login + "\n";
                for (ServerWorker serverWorker : workerList) {
                    if (!login.equals(serverWorker.getLogin())) {
                        serverWorker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "Invalid Username/Password\n";
                System.out.println("Invalid Username/Password\n");
                outputStream.write(msg.getBytes());
            }
        }

    }

    private void send(String msg) throws IOException {
        if(login!=null) {
        outputStream.write(msg.getBytes());
        }
        }

    private void handleLogoff() throws IOException {
         server.removeWorker(this);
         List<ServerWorker> workerList = server.getWorkerList();
         String onlineMsg = "Offline " + login + "\n";
                for (ServerWorker serverWorker : workerList) {
                    if (!login.equals(serverWorker.getLogin())) {
                        serverWorker.send(onlineMsg);
                        System.out.println(login+" logged out");
                    }
                }
         clientSocket.close();      
    }

    //format of the message will be "msg hostid body..."
    private void handleMessage(String[] tokens) throws IOException {
        String hostId = tokens[1];
        String body = tokens[2];
         List<ServerWorker> workerList = server.getWorkerList();
                for (ServerWorker serverWorker : workerList) {
                    if (hostId.equalsIgnoreCase(serverWorker.getLogin())) {
                        String outMsg = login+": "+body+"\n";
                         System.out.println(outMsg);
                        serverWorker.send(outMsg);
                       
                    }
                }
        
        
          }
   }

  //Server Worker Class Ends here



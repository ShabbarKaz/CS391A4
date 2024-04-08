/** Peer.java - A peer-to-peer file sharing program
 *
 *  @version CS 391 - Spring 2024 - A4
 *
 *  @author FIRST STUDENT'S FULL NAME GOES HERE
 *
 *  @author 2nd STUDENT'S FULL NAME GOES HERE
 *
 *  @author 3rd STUDENT'S FULL NAME GOES HERE (DELETE THIS LINE IF NOT NEEDED)
 * 
 *  @bug The initial join of a new peer [does/does not] work fully. [pick one]
 *
 *  @bug The Status command [does/does not] work fully. [pick one]
 *
 *  @bug The Find command [does/does not] work fully. [pick one]
 *
 *  @bug The Get command [does/does not] work fully. [pick one]
 * 
 *  @bug The Quit command [does/does not] work fully. [pick one]
 * 
 **/

import java.io.*;
import java.net.*;
import java.util.*;

class Peer
{
    String name,    // symbolic name of the peer, e.g., "P1" or "P2"
        ip,         // IP address in dotted decimal notation, e.g., "127.0.0.1"
        filesPath;  // path to local file repository, e.g., "dir1/dir2/dir3"
    int lPort,      // lookup port number (permanent UDP port)
        ftPort;     // file transfer port number (permanent TCP port)
    List<Neighbor> neighbors;      // current neighbor peers of this peer
    LookupThread  lThread;         // thread listening via the lookup socket
    FileTransferThread  ftThread;  // thread listening via file transfer socket
    int seqNumber;                 // identifier for next Find request (used to
                                   // control the flooding by avoiding loops)
    Scanner scanner;               // used for keyboard input
    HashSet<String> findRequests;  // record of all lookup requests seen so far

    /* Instantiate a new peer (including setting a value of 1 for its initial
       sequence number), launch its lookup and file transfer threads,
       (and start the GUI thread, which is already implemented for you)
     */
    Peer(String name2, String ip, int lPort, String filesPath, 
         String nIP, int nPort)
    {
        this.name = name2;
        this.ip = ip;
        this.lPort = lPort;
        this.filesPath = filesPath;
        this.neighbors = new ArrayList<>();
        this.seqNumber = 1;
        this.scanner = new Scanner(System.in);
        this.findRequests = new HashSet<>();

        try {
            this.lThread = new LookupThread();
            this.lThread.start();
            this.ftThread = new FileTransferThread();
            this.ftThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }//constructor

    /* display the commands available to the user
       Do NOT modify this method.
     */
    static void displayMenu()
    {
        System.out.println("\nYour options:");
        System.out.println("    1. [S]tatus");
        System.out.println("    2. [F]ind <filename>");
        System.out.println("    3. [G]et <filename> <peer IP> <peer port>");
        System.out.println("    4. [Q]uit");
        System.out.print("Your choice: ");
    }// displayMenu method

    /* input the next command chosen by the user
     */
    int getChoice()
    {
        displayMenu();
        String input = scanner.nextLine();
        switch (input.toLowerCase()) {
            case "1":
            case "s":
                return 1;
            case "2":
            case "f":
                return 2;
            case "3":
            case "g":
                return 3;
            case "4":
            case "q":
                return 4;
            default:
                System.out.println("Invalid choice. Please try again.");
                return -1;
        }
    }// getChoice method

    /* this is the implementation of the peer's main thread, which
       continuously displays the available commands, inputs the user's
       choice, and executes the selected command, until the latter
       is "Quit"
     */
    void run()
    {
        while (true) {
            int choice = getChoice();
            switch (choice) {
                case 1:
                    processStatusRequest();
                    break;
                case 2:
                    processFindRequest();
                    break;
                case 3:
                    processGetRequest();
                    break;
                case 4:
                    processQuitRequest();
                    return;
                default:
                    break;
            }
        }

    } //run method

    /* execute the Quit command, that is, send a "leave" message to all of the
       peer's neighbors, then terminate the lookup thread
     */
    void processQuitRequest()
    {
        for (Neighbor neighbor : neighbors) {
            System.out.println("Sending leave message to neighbor: " + neighbor.ip);
            }
        lThread.terminate();
      ftThread.close();
    }// processQuitRequest method

    /* execute the Status command, that is, read and display the list
       of files currently stored in the local directory of shared
       files, then print the list of neighbors. The EXACT format of
       the output of this method (including indentation, line
       separators, etc.) is specified via examples in the assignment
       handout.
     */
    void processStatusRequest()
    {
                       System.out.println("Files in local directory:");
                       File directory = new File(filesPath);
                       File[] fileList = directory.listFiles();
                       if (fileList != null) {
                           for (File file : fileList) {
                               if (file.isFile()) {
                                   System.out.println("    " + file.getName());
                               }
                           }
                       } else {
                           System.out.println("    No files found.");
                       }
                       printNeighbors();

    }// processStatusRequest method

    /* execute the Find command, that is, prompt the user for the file
       name, then look it up in the local directory of shared
       files. If it is there, inform the user. Otherwise, send a
       lookup message to all of the peer's neighbors. The EXACT format
       of the output of this method (including the prompt and
       notification), as well as the format of the 'lookup' messages
       are specified via examples in the assignment handout. Do not forget
       to handle the Find-request ID properly.
     */
    void processFindRequest()
    {

    System.out.print("Enter filename to find: ");
    String filename = scanner.nextLine().trim();

    // Check if file is in the local directory
    File directory = new File(filesPath);
    File[] fileList = directory.listFiles();
    boolean foundLocally = false;
    if (fileList != null) {
        for (File file : fileList) {
            if (file.isFile() && file.getName().equals(filename)) {
                foundLocally = true;
                break;
            }
        }
    }                                                                           

    if (foundLocally) {
        // File found locally, inform user
        System.out.println("File '" + filename + "' found locally.");
    } else {
        // File not found locally, send lookup message to neighbors
        System.out.println("File '" + filename + "' not found locally. Sending lookup message to neighbors.");

        // Generate Find-request ID
        int findRequestID = seqNumber++;

        // Send lookup message to all neighbors
        for (Neighbor neighbor : neighbors) {
            String lookupMessage = "lookup " + findRequestID + " " + filename;
            // Send lookup message to neighbor (implementation depends on communication method)
            System.out.println("Sending lookup message to neighbor " + neighbor.ip + ":" + neighbor.port);
        }
    }

    }// processFindRequest method

    /* execute the Get command, that is, prompt the user for the file
       name and address and port number of the selected peer with the
       needed file. Send a "get" message to that peer and wait for its
       response. If the file is not available at that peer, inform the
       user. Otherwise, extract the file contents from the response,
       output the contents on the user's terminal and save this file
       (under its original name) in the local directory of shared
       files.  The EXACT format of this method's output (including the
       prompt and notification), as well as the format of the "get"
       messages are specified via examples in the assignment
       handout.
     */
    void processGetRequest()
    {
        /* to be completed */

    }// processGetRequest method

    /* create a text file in the local directory of shared files whose
       name and contents are given as arguments.
     */
    void writeFile(String fileName, String contents)
    {


    }// writeFile method

    /* Send to the user's terminal the list of the peer's
       neighbors. The EXACT format of this method's output is specified by
       example in the assignment handout.
     */
    void printNeighbors()
    {   
      System.out.println("Neighbor Peers:");
      for (Neighbor neighbor : neighbors) {
          System.out.println("    " + neighbor.ip + ":" + neighbor.port);
      }

    }// printNeighbors method

    /* Do NOT modify this inner class
     */
    class Neighbor
    {
        String ip;
        int port;

        Neighbor(String ip, int port)
        {
            this.ip = ip;
            this.port = port;
        }// constructor

        public boolean equals(Object o)
        {
            if (o == this) { return true;  }
            if (!(o instanceof Neighbor)) { return false;  }        
            Neighbor n = (Neighbor) o;         
            return n.ip.equals(ip) && n.port == port;
        }// equals method

        public String toString()
        {
            return ip + ":" + port;
        }// toString method
    }// Neighbor class

    class LookupThread extends Thread
    {   
        DatagramSocket socket = null;           // UDP server socket
        private volatile boolean stop = false;  // flag used to stop the thread

        /* Stop the lookup thread by closing its server socket. This
           works (in a not-so-pretty way) because this thread's run method is
           constantly listening on that socket.
           Do NOT modify this method.
         */
        public void terminate()
        {
            stop = true;
            socket.close();
        }// terminate method

        /*This is the implementation of the thread that listens on
           the UDP lookup socket. First (at startup), if the peer has
           exactly one neighbor, send a "join" message to this
           neighbor. Otherwise, skip this step. Second, continuously
           wait for an incoming datagram (i.e., a request), display
           its contents in the GUI's Lookup panel, and process the
           request using the helper method below.*/
        public void run()
        {
            try {
              socket = new DatagramSocket(lPort);
                if (neighbors.size() == 1) {
                    Neighbor neighbor = neighbors.get(0);
                    String joinMessage = "join";
                    byte[] sendData = joinMessage.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(neighbor.ip), neighbor.port);
                    socket.send(sendPacket);
                }
              
                // Second, continuously wait for incoming datagrams
                while (!stop) {
                    byte[] receiveData = new byte[500];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Received: " + receivedMessage);
                    process(receivedMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }// run method

        /* This helper method processes the given request, which was
           received by the Lookup socket. Based on the first field of
           the request (i.e., the "join", "leave", "lookup", or "file"
           keyword), perform the appropriate action. All actions are
           quite short, except for the "lookup" request, which has its
           own helper method below.
         */
        void process(String request)
        {
            StringTokenizer tokenizer = new StringTokenizer(request);
            String keyword = tokenizer.nextToken();
            switch (keyword) {
                case "join":
                    // Process join request
                    break;
                case "leave":
                    // Process leave request
                    break;
                case "lookup":
                    processLookup(tokenizer);
                    break;
                case "file":
                    // Process file request
                    break;
                default:
                    System.out.println("Invalid request received.");
                    break;
            }

        }// process method

        /* This helper method processes a "lookup" request received
           by the Lookup socket. This request is represented by the
           given tokenizer, which contains the whole request line,
           minus the "lookup" keyword (which was removed from the
           beginning of the request) by the caller of this method.
           Here is the algorithm to process such requests:
           If the peer already received this request in the past (see request
           ID), ignore the request. 
           Otherwise, check if the requested file is stored locally (in the 
           peer's directory of shared files):
             + If so, send a "file" message to the source peer of the request 
               and, if necessary, add this source peer to the list 
               of neighbors of this peer.
             + If not, send a "lookup" message to all neighbors of this peer,
               except the peer that sent this request (that is, the "from" peer
               as opposed to the "source" peer of the request).
         */
        void processLookup(StringTokenizer line)
        {
                    int requestID = Integer.parseInt(line.nextToken());
            String fileName = line.nextToken();
            String fromIP = line.nextToken();
            int fromPort = Integer.parseInt(line.nextToken());

            // Check if the request ID has been seen before
            if (findRequests.contains(String.valueOf(requestID))) {
                // If the request ID has been seen before, ignore the request
                return;
            } else {
                // Add the request ID to the set of seen requests
                findRequests.add(String.valueOf(requestID));
            }

            // Check if the requested file is stored locally
            File directory = new File(filesPath);
            File[] files = directory.listFiles();
            boolean fileFound = false;
            for (File file : files) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    // If the file is found locally, send a "file" message to the source peer
                    String fileMessage = "file " + file.getName();
                    byte[] sendData = fileMessage.getBytes();
                    try (DatagramSocket sendSocket = new DatagramSocket()) {
                        InetAddress address = InetAddress.getByName(fromIP);
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, fromPort);
                        sendSocket.send(sendPacket);
                        fileFound = true;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!fileFound) {
                // If the file is not found locally, send a "lookup" message to all neighbors
                for (Neighbor neighbor : neighbors) {
                    if (!(neighbor.ip.equals(fromIP) && neighbor.port == fromPort)) {
                        String lookupMessage = "lookup " + requestID + " " + fileName + " " + fromIP + " " + fromPort;
                        byte[] sendData = lookupMessage.getBytes();
                        try (DatagramSocket sendSocket = new DatagramSocket()) {
                            InetAddress address = InetAddress.getByName(neighbor.ip);
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, neighbor.port);
                            sendSocket.send(sendPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }// processLookup method

    }// LookupThread class

    class FileTransferThread extends Thread
    {
        ServerSocket serverSocket = null;   // TCP listening socket
        Socket clientSocket = null;         // TCP socket to a client
        DataInputStream in = null;          // input stream from client
        DataOutputStream out = null;        // output stream to client
        String request, reply;

        /* this is the implementation of the peer's File Transfer
           thread, which first creates a listening socket (or welcome
           socket or server socket) and then continuously waits for
           connections. For each connection it accepts, the newly
           created client socket waits for a single request and processes
           it using the helper method below (and is finally closed).
        */      
        public void run()
        {
          try {
              serverSocket = new ServerSocket(ftPort);
              while (true) {
                  clientSocket = serverSocket.accept();
                  openStreams();
                  request = in.readUTF();
                  process(request);
                  close();
              }
          } catch (IOException e) {
              e.printStackTrace();
          }
          /* to be completed */

        }// run method

        /* Process the given request received by the TCP client
           socket.  This request must be a "get" message (the only
           command that uses the TCP sockets). If the requested
           file is stored locally, read its contents (as a String)
           using the helper method below and send them to the other side
           in a "fileFound" message. Otherwise, send back a "fileNotFound"
           message.
         */
        void process(String request)
        {           
            String[] parts = request.split(" ");
            String fileName = parts[1];
            File file = new File(filesPath + File.separator + fileName);
            try {
                if (file.exists()) {
                    byte[] fileContent = readFile(file);
                    out.writeUTF("fileFound");
                    out.writeInt(fileContent.length);
                    out.write(fileContent);
                } else {
                    out.writeUTF("fileNotFound");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }// process method

        /* Given a File object for a file that we know is stored at this
           peer, return the contents of the file as a byte array.
        */
        byte[] readFile(File file)
        {
            byte[] content = new byte[(int) file.length()];
             try (FileInputStream fis = new FileInputStream(file)) {
                 fis.read(content);
             } catch (IOException e) {
                 e.printStackTrace();
             }
             return content;

            //return null;
        }// readFile method

        /* Open the necessary I/O streams and initialize the in and out
           variables; this method does not catch any exceptions.
        */
        void openStreams() throws IOException
        {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

        }// openStreams method

        /* close all open I/O streams and the client socket
         */
        void close()
        {

          String leaveMessage= "leave " + ip
          out.writeUTF(leaveMessage);
           try {
            if (in != null)           { in.close();           } 
            if (out != null)          { out.close();          } 
            if (clientSocket != null) { clientSocket.close(); }  
           } catch (IOException e) {
               e.printStackTrace();

             
           }
        }// close method

    }// FileTransferThread class

}// Peer class

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        try {
            //openStreams();
            this.ip = ip;
            this.lPort = lPort;
            //ftPort = 

            //new Thread(new FileTransferThread()).start();
            //ftThread = new Thread(new LookupThread()).start();

            

            GUI.createAndShowGUI("Peer "+nIP);

            //TODO ping ip/port with join method

            this.filesPath = filesPath;
            
            seqNumber = 1;
        } catch (Exception e) {
            // TODO: handle exception
        }
        /* to be completed */

    }// constructor

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
        char choiceInput;
        int outputNumber = -1;
        displayMenu();
        try {
            choiceInput = scanner.nextLine().toLowerCase().charAt(0);
            if(choiceInput=='s'){outputNumber = 1;}
            if(choiceInput=='f'){outputNumber = 2;}
            if(choiceInput=='g'){outputNumber = 3;}
            if(choiceInput=='q'){outputNumber = 4;}
            if(choiceInput=='1'){outputNumber = 1;}
            if(choiceInput=='2'){outputNumber = 2;}
            if(choiceInput=='3'){outputNumber = 3;}
            if(choiceInput=='4'){outputNumber = 4;}
            if (outputNumber==-1){outputNumber=getChoice();}
        } catch (Exception e) {
            System.out.println( "Error on input, try again" );
            outputNumber = getChoice();
        }
        return outputNumber;
    }// getChoice method
        
    /* this is the implementation of the peer's main thread, which
       continuously displays the available commands, inputs the user's
       choice, and executes the selected command, until the latter
       is "Quit"
     */
    void run()
    {
        int currentChoice = -1;
        scanner = new Scanner(System.in);
        while (currentChoice != 4) {
            currentChoice = getChoice();
            if (currentChoice==1) {processStatusRequest();}
            if (currentChoice==2) {processFindRequest();}
            if (currentChoice==3) {processGetRequest();}
            if (currentChoice==4) {processQuitRequest();}
        }
    }// run method

    /* execute the Quit command, that is, send a "leave" message to all of the
       peer's neighbors, then terminate the lookup thread
     */
    void processQuitRequest()
    {
        for (Neighbor neigh : neighbors) {
            //TODO leave
        }

        //TODO terminate lookup thread

        //TODO terminate FileTransfer thread

        scanner.close();
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
        //TODO done/Test
        System.out.println( "Local files:" );
        String spacer = "    "; //TODO 4 or 5?
        for (File file : new File(filesPath).listFiles()) {
            System.out.println( spacer+file.getName() );
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
        //TODO ===========
        System.out.println("Name of file to find: ");
        String filename = scanner.nextLine();
        //TODO ===========

        try {
            File file = new File(filename);
            if(file.exists() && !file.isDirectory()) { 
                //it is local
                System.out.println( "This file exists locally in "+file.getAbsolutePath() );
            } else {
                //do find requests
                //TODO
            }
        } catch (Exception e) {
            // TODO: handle exception
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
        //get user input

        //parse UI
        String method = "get";
        String destinationip = ""; //Should be InetAddress
        int destinationPort = 0;

        //send request with ???
        //TODO
    }// processGetRequest method

    /* create a text file in the local directory of shared files whose
       name and contents are given as arguments.
     */
    void writeFile(String fileName, String contents)
    {
        try {
            File file = new File(filesPath, fileName);
            List<String> contentList = Arrays.asList(contents);
            //contentList = contents.split("\n");
            Files.write(file.toPath(), contentList, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }// writeFile method

    /* Send to the user's terminal the list of the peer's
       neighbors. The EXACT format of this method's output is specified by
       example in the assignment handout.
     */
    void printNeighbors()
    {   
        System.out.println( "Neighbors:" );
        String spacer = "    "; //TODO 4 or 5?
        for (Neighbor neighbor : neighbors) {
            System.out.println( spacer+neighbor);
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

        /* This is the implementation of the thread that listens on
           the UDP lookup socket. First (at startup), if the peer has
           exactly one neighbor, send a "join" message to this
           neighbor. Otherwise, skip this step. Second, continuously
           wait for an incoming datagram (i.e., a request), display
           its contents in the GUI's Lookup panel, and process the
           request using the helper method below.
        */
        public void run()
        {
            //TODO not done/tested
            try {
                socket = new DatagramSocket(lPort);
                if (neighbors.size() == 1) {
                    //send join message to neighbor
                    byte[] buff = new byte[256];
                    //TODO need to set contents of buffer
                    InetAddress address = InetAddress.getByName(neighbors.get(0).ip);
                    DatagramPacket packet = new DatagramPacket(
                        buff, 
                        buff.length,
                        address,
                        neighbors.get(0).port
                        );
                    socket.send(packet);
                }

                //start loop
                while (true) {
                    byte[] buff = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buff, buff.length);
                    socket.receive(packet);
                    process(new String(packet.getData(), 0, packet.getLength()));
                }
                
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
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
            /* to be completed */
            String[] parts = request.split(" "); //TODO parse command from the front of the string
            byte[] buff = new byte[256];
            
            try {
                InetAddress destAddress = InetAddress.getByName(parts[1]);
                int destPort = Integer.parseInt(parts[2]);
                switch (parts[0]) {
                    case "find": //remove??
                        //TODO need to set contents of buffer

                        break;
                    case "join":
                        //TODO send neighbors
                        //TODO need to set contents of buffer
                        destAddress = InetAddress.getByName(parts[1]);
                        destPort = Integer.parseInt(parts[2]);
                        break;
                    case "leave":
                        //TODO need to set contents of buffer
                        destAddress = InetAddress.getByName(parts[1]);
                        destPort = Integer.parseInt(parts[2]);
                        break;
                    case"lookup":
                        processLookup(null); //TODO
                        break;
                    case"file":
                        //TODO use filetransfer thread to send file contents?
                        
                        break;
                
                    default:
                        break;
                }

                DatagramPacket packet = new DatagramPacket(
                            buff, 
                            buff.length,
                            destAddress,
                            destPort
                            );
                socket.send(packet);
            } catch (Exception e) {

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
            /* to be completed */

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
                    clientSocket=serverSocket.accept();
                    openStreams();
                    process(in.readUTF()); //get the request from the client and processes
                    close();
                }
                //TODO serverSocket is never closed
            } catch (Exception e) {
                // TODO: handle exception
            }

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
            //TODO might have a "\n"
            String fileName = request.substring(request.indexOf("get ")+4);
            
            try {
                File toBeSent = new File(filesPath, fileName);  //Might throw error which should trigger the else?
                if (toBeSent.exists()) {
                    //file is on local dir, so get contents and send
                    out.write(readFile(toBeSent));
                } else {
                    //send fileNotFound
                }
            }catch (Exception e) {
                // TODO: handle exception
            }

        }// process method

        /* Given a File object for a file that we know is stored at this
           peer, return the contents of the file as a byte array.
        */
        byte[] readFile(File file)
        {
           try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                System.out.println(
                        "There was an error reading the file: " +
                                file.getName() + "\n" + e.getMessage()
            );}
            return null;
        }// readFile method

        /* Open the necessary I/O streams and initialize the in and out
           variables; this method does not catch any exceptions.
        */
        void openStreams() throws IOException
        {
            //TODO fix/test
            in = (DataInputStream) clientSocket.getInputStream();
            out = (DataOutputStream) clientSocket.getOutputStream();
        }// openStreams method

        /* close all open I/O streams and the client socket
         */
        void close()
        {
            //TODO not done/tested
            try {
                // if (serverSocket != null) {
                //     serverSocket.close();    //Not needed since we want the server socket open
                // }
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                System.err.println("Error in close(): "+e.getMessage());
            }

        }// close method
        
    }// FileTransferThread class

}// Peer class

/**
 * Peer.java - A peer-to-peer file sharing program
 *
 * @version CS 391 - Spring 2024 - A4
 * @author FIRST STUDENT'S FULL NAME GOES HERE
 * @author 2nd STUDENT'S FULL NAME GOES HERE
 * @author 3rd STUDENT'S FULL NAME GOES HERE (DELETE THIS LINE IF NOT NEEDED)
 * @bug The initial join of a new peer [does/does not] work fully. [pick one]
 * @bug The Status command [does/does not] work fully. [pick one]
 * @bug The Find command [does/does not] work fully. [pick one]
 * @bug The Get command [does/does not] work fully. [pick one]
 * @bug The Quit command [does/does not] work fully. [pick one]
 **/

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

class Peer {
    String name,    // symbolic name of the peer, e.g., "P1" or "P2"
            ip,         // IP address in dotted decimal notation, e.g., "127.0.0.1"
            filesPath;  // path to local file repository, e.g., "dir1/dir2/dir3"
    int lPort,      // lookup port number (permanent UDP port)
            ftPort;     // file transfer port number (permanent TCP port)
    List<Neighbor> neighbors;      // current neighbor peers of this peer
    LookupThread lThread;         // thread listening via the lookup socket
    FileTransferThread ftThread;  // thread listening via file transfer socket
    int seqNumber;                 // identifier for next Find request (used to
    // control the flooding by avoiding loops)
    Scanner scanner;               // used for keyboard input
    HashSet<String> findRequests;  // record of all lookup requests seen so far

    /* Instantiate a new peer (including setting a value of 1 for its initial
       sequence number), launch its lookup and file transfer threads,
       (and start the GUI thread, which is already implemented for you)
     */
    Peer(String name2, String ip, int lPort, String filesPath,
         String nIP, int nPort) {
        name = name2;
        this.ip = ip;
        this.lPort = lPort;
        this.filesPath = filesPath;
        scanner = new Scanner(System.in);
        findRequests = new HashSet<>();

        neighbors = new ArrayList<>();
        if (nIP != null && nPort != -1) {
            neighbors.add(new Neighbor(nIP, nPort));
        }

        String title = name;
        GUI.createAndShowGUI(title);

        seqNumber = 1;
        lThread = new LookupThread();
        lThread.start();

        ftThread = new FileTransferThread();
        ftThread.start();
    }// constructor

    /* display the commands available to the user
       Do NOT modify this method.
     */
    static void displayMenu() {
        System.out.println("\nYour options:");
        System.out.println("    1. [S]tatus");
        System.out.println("    2. [F]ind <filename>");
        System.out.println("    3. [G]et <filename> <peer IP> <peer port>");
        System.out.println("    4. [Q]uit");
        System.out.print("Your choice: ");
    }// displayMenu method

    /* input the next command chosen by the user
     */
    int getChoice() {
        int choice = -1;
        String option = scanner.nextLine().toLowerCase();
        choice = switch (option) {
            case "1", "s" -> 1;
            case "2", "f" -> 2;
            case "3", "g" -> 3;
            case "4", "q" -> 4;
            default -> choice;
        };
        return choice;
    }// getChoice method

    /* this is the implementation of the peer's main thread, which
       continuously displays the available commands, inputs the user's
       choice, and executes the selected command, until the latter
       is "Quit"
     */
    void run() {
        int choice = -1;
        while (choice != 4) {
            displayMenu();
            choice = getChoice();
            switch (choice) {
                case 1 -> processStatusRequest();
                case 2 -> processFindRequest();
                case 3 -> processGetRequest();
                case 4 -> processQuitRequest();
                default -> System.out.println("Invalid choice");
            }
        }


    }// run method

    /* execute the Quit command, that is, send a "leave" message to all of the
       peer's neighbors, then terminate the lookup thread
     */
    void processQuitRequest() {
        try {
            String message = "leave " +
                    InetAddress.getByName(ip).getHostAddress() +
                    " " + lPort;
            byte[] byteMessage = message.getBytes();
            for (Neighbor neighbor : neighbors) {
                int nPort = neighbor.port;
                String nIP = neighbor.ip;
                DatagramSocket neighborSocket = new DatagramSocket();
                neighborSocket.connect(
                        InetAddress.getByName(nIP),
                        nPort
                );
                DatagramPacket packet = new DatagramPacket(
                        byteMessage,
                        byteMessage.length
                );
                neighborSocket.send(packet);
                neighborSocket.close();
            }
        } catch (SocketException e) {
            System.out.println(
                    "Error creating or accessing a UDP socket: " +
                            e.getMessage()
            );
        } catch (UnknownHostException e) {
            System.out.println("Invalid IP: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(
                    "Error sending a message to a neighbor: " +
                            e.getMessage()
            );
        }
        lThread.terminate();
    }// processQuitRequest method

    /* execute the Status command, that is, read and display the list
       of files currently stored in the local directory of shared
       files, then print the list of neighbors. The EXACT format of
       the output of this method (including indentation, line
       separators, etc.) is specified via examples in the assignment
       handout.
     */
    void processStatusRequest() {
        String border = "==================";
        String indent = "    ";

        System.out.println();
        System.out.println(border);
        System.out.println("Local files:");

        File filesPathFolder = new File(filesPath);
        File[] files = filesPathFolder.listFiles();
        if (files != null) {
            TreeSet<String> fileNames = new TreeSet<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            for (String fileName : fileNames) {
                System.out.println(indent + fileName);
            }
        }

        printNeighbors();
        System.out.println(border);
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
    void processFindRequest() {
        String border = "==================";

        System.out.println();
        System.out.println(border);
        System.out.print("Name of file to find: ");
        String requestedFile = scanner.nextLine();

        File filesPathFolder = new File(filesPath);
        File[] files = filesPathFolder.listFiles();
        String requestedFileLocation = null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(requestedFile)) {
                    requestedFileLocation =
                            file.getParentFile().getPath();
                }
            }
        }

        if (requestedFileLocation != null) {
            System.out.println(
                    "This file exists locally in " +
                            requestedFileLocation + "/"
            );
        } else {
            try {
                String ipAddress =
                        InetAddress.getByName(ip).getHostAddress();
                String message = "lookup " +
                        requestedFile +
                        " " + name +
                        "#" + seqNumber++ +
                        " " + ipAddress +
                        " " + lPort +
                        " " + ipAddress +
                        " " + lPort;
                byte[] byteMessage = message.getBytes();
                for (Neighbor neighbor : neighbors) {
                    int nPort = neighbor.port;
                    String nIP = neighbor.ip;
                    DatagramSocket neighborSocket =
                            new DatagramSocket();
                    neighborSocket.connect(
                            InetAddress.getByName(nIP),
                            nPort
                    );
                    DatagramPacket packet = new DatagramPacket(
                            byteMessage,
                            byteMessage.length
                    );
                    neighborSocket.send(packet);
                    neighborSocket.close();
                }
            } catch (SocketException e) {
                System.out.println(
                        "Error creating or accessing a UDP socket: " +
                                e.getMessage()
                );
            } catch (UnknownHostException e) {
                System.out.println("Invalid IP: " + e.getMessage());
            } catch (IOException e) {
                System.out.println(
                        "Error sending a message to a neighbor: " +
                                e.getMessage()
                );
            }
        }

        System.out.println(border);
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
    void processGetRequest() {
        String border = "==================";

        System.out.println();
        System.out.println(border);
        System.out.print("Name of file to get: ");
        String requestedFile = scanner.nextLine();
        System.out.print("Address of source peer: ");
        String sourceIP = scanner.nextLine();
        System.out.print("Port of source peer: ");
        int sourcePort = Integer.parseInt(scanner.nextLine());

        try {
            Socket socket = new Socket(sourceIP, sourcePort);
            DataInputStream in =
                    new DataInputStream(socket.getInputStream());
            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());
            String message = "get " + requestedFile;
            out.writeUTF(message);
            String response = in.readUTF();

            if (response.equals("fileNotFound")) {
                System.out.println(
                        "The file ’" + requestedFile +
                                "’ is not available at " + sourceIP +
                                ":" + sourcePort
                );
            } else {
                String fileContents = new String(in.readAllBytes());
                writeFile(requestedFile, fileContents);

                String fileBorder = "- - - - - - - - - - - - - -";
                System.out.println(
                        "Contents of the received file between dashes:"
                );
                System.out.println(fileBorder);
                System.out.println(fileContents);
                System.out.println(fileBorder);
            }
            in.close();
            out.close();
            socket.close();
        } catch (UnknownHostException e) {
            System.out.println("Invalid IP: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(
                    "Error communicating with peer: " + e.getMessage()
            );
        }
        System.out.println(border);
    }// processGetRequest method

    /* create a text file in the local directory of shared files whose
       name and contents are given as arguments.
     */
    void writeFile(String fileName, String contents) {
        try {
            String path = filesPath + "/" + fileName;
            PrintWriter out = new PrintWriter(path);
            out.write(contents);
            out.close();
        } catch (FileNotFoundException e) {
            System.out.println(
                    "Could not write file: " + e.getMessage()
            );
        }

    }// writeFile method

    /* Send to the user's terminal the list of the peer's
       neighbors. The EXACT format of this method's output is specified by
       example in the assignment handout.
     */
    void printNeighbors() {
        String indent = "    ";
        System.out.println("Neighbors:");
        for (Neighbor neighbor : neighbors) {
            System.out.println(indent + neighbor.toString());
        }
    }// printNeighbors method

    /* Do NOT modify this inner class
     */
    class Neighbor {
        String ip;
        int port;

        Neighbor(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }// constructor

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Neighbor)) {
                return false;
            }
            Neighbor n = (Neighbor) o;
            return n.ip.equals(ip) && n.port == port;
        }// equals method

        public String toString() {
            return ip + ":" + port;
        }// toString method
    }// Neighbor class

    class LookupThread extends Thread {
        DatagramSocket socket = null;           // UDP server socket
        private volatile boolean stop = false;  // flag used to stop the thread

        /* Stop the lookup thread by closing its server socket. This
           works (in a not-so-pretty way) because this thread's run method is
           constantly listening on that socket.
           Do NOT modify this method.
         */
        public void terminate() {
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
        public void run() {
            try {
                if (neighbors.size() == 1) {
                    Neighbor neighbor = neighbors.getFirst();
                    int nPort = neighbor.port;
                    String nIP = neighbor.ip;
                    DatagramSocket neighborSocket = new DatagramSocket();
                    neighborSocket.connect(
                            InetAddress.getByName(nIP),
                            nPort
                    );
                    String message = "join " +
                            InetAddress.getByName(ip).getHostAddress() +
                            " " + lPort;
                    byte[] byteMessage = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            byteMessage,
                            byteMessage.length
                    );
                    neighborSocket.send(packet);
                    neighborSocket.close();
                }

                socket = new DatagramSocket(
                        lPort,
                        InetAddress.getByName(ip)
                );
                byte[] clientMessage = new byte[1024];
                DatagramPacket packet = new DatagramPacket(
                        clientMessage,
                        clientMessage.length
                );

                String message;
                while (!stop) {
                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                        System.exit(0);
                    }
                    message = new String(
                            packet.getData(),
                            0,
                            packet.getLength()
                    );
                    GUI.displayLU("Received:    " + message);
                    process(message);
                }
            } catch (SocketException e) {
                System.out.println(
                        "Error creating or accessing a UDP socket: " +
                                e.getMessage()
                );
            } catch (UnknownHostException e) {
                System.out.println("Invalid IP: " + e.getMessage());
            } catch (IOException e) {
                System.out.println(
                        "Error communicating with a peer: " +
                                e.getMessage()
                );
            }
            System.exit(1);
        }// run method

        /* This helper method processes the given request, which was
           received by the Lookup socket. Based on the first field of
           the request (i.e., the "join", "leave", "lookup", or "file"
           keyword), perform the appropriate action. All actions are
           quite short, except for the "lookup" request, which has its
           own helper method below.
         */
        void process(String request) {
            StringTokenizer tokens = new StringTokenizer(request);
            if (tokens.hasMoreTokens()) {
                String command = tokens.nextToken();
                switch (command) {
                    case "join" -> {
                        String nIp = tokens.nextToken();
                        int lookupPort = Integer.parseInt(
                                tokens.nextToken()
                        );
                        neighbors.add(new Neighbor(nIp, lookupPort));
                    }
                    case "lookup" -> processLookup(tokens);
                    case "file" -> {
                        tokens.nextToken();
                        tokens.nextToken();
                        tokens.nextToken();
                        String neighborIP = tokens.nextToken();
                        int neighborPort =
                                Integer.parseInt(tokens.nextToken());
                        Neighbor newNeighbor =
                                new Neighbor(neighborIP, neighborPort);
                        if (!neighbors.contains(newNeighbor)) {
                            neighbors.add(newNeighbor);
                        }
                    }
                    case "leave" -> {
                        String leavingIP = tokens.nextToken();
                        int leavingPort =
                                Integer.parseInt(tokens.nextToken());
                        Neighbor leavingNeighbor =
                                new Neighbor(leavingIP, leavingPort);
                        neighbors.remove(leavingNeighbor);
                    }
                    default -> System.out.println("Invalid command");
                }
            } else {
                System.out.println("Invalid command");
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
        void processLookup(StringTokenizer line) {
            String indent = "    ";
            String fileName = line.nextToken();
            String lookupId = line.nextToken();
            boolean isNewId = findRequests.add(lookupId);
            if (!isNewId) {
                GUI.displayLU(
                        indent + indent +
                                "Saw request before: no forwarding"
                );
            } else {
                File filesPathFolder = new File(filesPath);
                File[] files = filesPathFolder.listFiles();
                File requestedFile = null;
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().equals(fileName)) {
                            requestedFile = file;
                        }
                    }
                }

                String senderIP = line.nextToken();
                int senderPort = Integer.parseInt(line.nextToken());
                String requesterIP = line.nextToken();
                int requesterPort = Integer.parseInt(line.nextToken());
                Neighbor newNeighbor =
                        new Neighbor(requesterIP, requesterPort);
                if (!neighbors.contains(newNeighbor)) {
                    neighbors.add(newNeighbor);
                }
                try {
                    if (requestedFile != null) {
                        DatagramSocket requesterSocket =
                                new DatagramSocket();
                        requesterSocket.connect(
                                InetAddress.getByName(requesterIP),
                                requesterPort
                        );

                        String message = "file " +
                                requestedFile.getName() + " is at " +
                                InetAddress.getByName(ip)
                                        .getHostAddress() +
                                " " + lPort +
                                " (tcp port:" + indent + ftPort + ")";
                        byte[] byteMessage = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(
                                byteMessage,
                                byteMessage.length
                        );
                        requesterSocket.send(packet);
                        requesterSocket.close();
                        GUI.displayLU("Sent:" + indent + message);
                    } else {
                        GUI.displayLU(
                                indent + indent +
                                        "File is NOT available " +
                                        "at this peer"
                        );
                        String message = "lookup " + fileName +
                                " " + lookupId +
                                " " + InetAddress
                                .getByName(ip)
                                .getHostAddress() +
                                " " + lPort +
                                " " + InetAddress
                                .getByName(requesterIP)
                                .getHostAddress() +
                                " " + requesterPort;
                        byte[] byteMessage = message.getBytes();
                        for (Neighbor neighbor : neighbors) {
                            if (!(neighbor.ip.equals(senderIP) &&
                                    neighbor.port == senderPort) &&
                                    !(neighbor.ip.equals(requesterIP) &&
                                            neighbor.port == requesterPort)
                            ) {
                                DatagramSocket neighborSocket =
                                        new DatagramSocket();
                                neighborSocket.connect(
                                        InetAddress.getByName(
                                                neighbor.ip
                                        ),
                                        neighbor.port
                                );
                                DatagramPacket packet = new DatagramPacket(
                                        byteMessage,
                                        byteMessage.length
                                );
                                neighborSocket.send(packet);
                                neighborSocket.close();
                                GUI.displayLU(
                                        indent + indent +
                                                "Forward request to " +
                                                InetAddress.getByName(
                                                        neighbor.ip
                                                ).getHostAddress()
                                                + ":" + neighbor.port
                                );
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println(
                            "Error creating or accessing " +
                                    "a UDP socket: " + e.getMessage()
                    );
                } catch (UnknownHostException e) {
                    System.out.println(
                            "Invalid IP: " + e.getMessage())
                    ;
                } catch (IOException e) {
                    System.out.println(
                            "Error sending a message to a peer: " +
                                    e.getMessage()
                    );
                }
            }
        }// processLookup method

    }// LookupThread class

    class FileTransferThread extends Thread {
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
        public void run() {
            try {
                serverSocket = new ServerSocket(lPort + 1);
                ftPort = serverSocket.getLocalPort();
                while (!lThread.stop) {
                    clientSocket = serverSocket.accept();
                    openStreams();
                    request = in.readUTF();
                    process(request);
                    close();
                }
                serverSocket.close();
            } catch (IOException e) {
                System.out.println(
                        "Error setting up a file transfer socket: " +
                                e.getMessage()
                );
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
        void process(String request) {
            String indent = "    ";
            GUI.displayFT("Received:" + indent + request);
            StringTokenizer tokens = new StringTokenizer(request);
            String command = tokens.nextToken();
            if (command.equals("get")) {
                try {
                    String fileName = tokens.nextToken();
                    String filesFolder = filesPath + "/" + fileName;
                    File file = new File(filesFolder);
                    if (file.exists()) {
                        byte[] fileContents = readFile(file);
                        GUI.displayFT(
                                indent +
                                        "Read file " + file.toPath()
                        );
                        out.writeUTF("fileFound");
                        out.write(
                                fileContents,
                                0,
                                fileContents.length
                        );
                        GUI.displayFT(
                                indent + "Sent back file contents"
                        );
                    } else {
                        reply = "fileNotFound";
                        out.writeUTF(reply);
                        GUI.displayFT(
                                indent + "responded: " + reply
                        );
                    }
                } catch (IOException e) {
                    System.out.println(
                            "Error sending a message to a peer: " +
                                    e.getMessage()
                    );
                }
            }
        }// process method

        /* Given a File object for a file that we know is stored at this
           peer, return the contents of the file as a byte array.
        */
        byte[] readFile(File file) {
            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                System.out.println(
                        "Error reading a file: " + e.getMessage()
                );
            }
            return bytes;
        }// readFile method

        /* Open the necessary I/O streams and initialize the in and out
           variables; this method does not catch any exceptions.
        */
        void openStreams() throws IOException {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        }// openStreams method

        /* close all open I/O streams and the client socket
         */
        void close() {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (clientSocket != null) {
                    clientSocket.close();
                    clientSocket = null;
                }
            } catch (IOException e) {
                System.out.println(
                        "Error closing a client stream: " +
                                e.getMessage()
                );
            }
        }// close method

    }// FileTransferThread class

}// Peer class

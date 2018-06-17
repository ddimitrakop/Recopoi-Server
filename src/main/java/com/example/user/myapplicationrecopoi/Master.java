package com.example.user.myapplicationrecopoi;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Master {

    /* ---- WORKER SPECS ---- */
    protected static final int WORKERS = 3;
    int[] cores = new int[WORKERS];
    long[] ram = new long[WORKERS];

    /* ---- SOCKET VARIABLES ---- */
    private final String IP = "localhost";
    private static final int PORT = 60000;
    private ServerSocket providerSocket = null;
    private Socket clientSocket = null;
    Socket[] workerSockets = new Socket[WORKERS];
    ObjectInputStream[] in = new ObjectInputStream[WORKERS];
    ObjectOutputStream[] out = new ObjectOutputStream[WORKERS];
    Data received;

    /* ---- PROGRAM PHASES ---- */
    static boolean client_phase = true;
    static boolean training_phase = true;

    /* ---- DATASETS AND MATRICES ---- */
    int USERS = 0;
    int POIS = 0;
    String filepath = "src/main/datasets/dataset.csv";
    protected static OpenMapRealMatrix R_matrix;
    protected OpenMapRealMatrix C_matrix;
    protected OpenMapRealMatrix P_matrix;
    protected OpenMapRealMatrix X;
    protected OpenMapRealMatrix Y;
    protected static RealMatrix predictions;

    /* ---- IMPLICIT CALCULATION VARIABLES ---- */
    double l = 0.1;
    private double min = 0;
    private int features = 10;
    private int iterations = 5;
    private double threshold = 0.1;
    private RandomGenerator randomGenerator;

    /* ---- POI INFORMATION ---- */
    private static Map <Integer, Poi> poiList = null;

    /* Initializes R,C,P matrices */
    private void initialize() {
        /*Calculate R_Matrix-File dimensions*/
        calculateFile_R_MatrixDimensions(filepath);

        R_matrix = new OpenMapRealMatrix(USERS, POIS);
        predictions = new OpenMapRealMatrix(USERS, POIS);

        randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(1);

        X = createRandomMatrix(USERS, features);
        Y = createRandomMatrix(POIS, features);

        /*Read File and initialize R_Matrix*/
        ReadFileRMatrix(filepath);

        // Check input
        calculateCMatrix(40, R_matrix);
        calculatePMatrix(R_matrix);

        /* Parse the POI JSON data */
        poiList = JSONParser.parsePois();

        /* Now open the server and distribute work to Slaves */
        openServer();
    }

    /* Creates a Random sparse matrix */
    private OpenMapRealMatrix createRandomMatrix(int rows, int cols) {
        OpenMapRealMatrix rdm_matrix = new OpenMapRealMatrix(rows, cols);

        for (int i = 0; i < rdm_matrix.getRowDimension(); i++) {
            for (int j = 0; j < rdm_matrix.getColumnDimension(); j++) {
                rdm_matrix.setEntry(i, j, randomGenerator.nextDouble());
            }
        }
        return rdm_matrix;
    }

    /* Creates the C matrix */
    private void calculateCMatrix(int a, RealMatrix matrix) {
        C_matrix = new OpenMapRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                C_matrix.setEntry(i, j, (1 + a * matrix.getEntry(i, j)));
            }
        }
    }

    /* Initialise the P matrix */
    private void calculatePMatrix(RealMatrix matrix) {
        P_matrix = new OpenMapRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                P_matrix.setEntry(i, j, (matrix.getEntry(i, j) > 0 ? 1 : 0));
            }
        }
    }

    public void calculatePredictions() {
        predictions = X.multiply(Y.transpose());
    }

    public double calculateScore(int user, int poi) {
        return predictions.getEntry(user, poi);
    }

    public static int[] calculateBestLocalPoisForUser(int user, int K,double latitude,double longitude,String category,double distance) {
        double[] user_row = predictions.getRow(user);

        for(int i=0; i<user_row.length; i++){
            if (R_matrix.getEntry(user,i)>0){//an o xristis pou theloume exei episkeftei to ekastote poi tote tou midenizoume to prediction wste na min protathei
                user_row[i]=0;
            }
        }

        ArrayList<Poi> recommend_pois_with_predictions = new ArrayList<Poi>();      //i lista me ola ta pois pou proteinontai
        ArrayList<Poi> recom_pred_distance=new ArrayList<Poi>();                    //i lista me ta pois pou exoun ti swsti apostasi
        ArrayList<Poi> recom_pred_dist_category=new ArrayList<Poi>();               //i lista me ta pois pou exoun kai swsti apostasi kai swsti katigoria
        ArrayList<Poi> best_pois1 = new ArrayList<Poi>();           //i lista me ton k-arithmo pois pou zhtaei o xristis
        int j = 0;

        for (double d : user_row) {         //exoume pleon ti lista me ta POIS mazi me to prediction kai to arithmitiko ID
            recommend_pois_with_predictions.add(new Poi(poiList.get(j).getPOI(),poiList.get(j).getLatidude(),poiList.get(j).getLongitude(),poiList.get(j).getPhotos(),poiList.get(j).getPOI_category_id(),poiList.get(j).getPOI_name(),j++, d));
        }

        for(int i=0; i<recommend_pois_with_predictions.size(); i++){    //gemizoume ti lista me ta POIS pou exoun swsti apostasi
            if((Math.sqrt(Math.pow(recommend_pois_with_predictions.get(i).getLatidude()-latitude,2)+Math.pow(recommend_pois_with_predictions.get(i).getLongitude()-longitude,2)))<=distance){
                recom_pred_distance.add(recommend_pois_with_predictions.get(i));
            }
        }

        if(!category.equals("-")){      //an o xristis exei dwsei katigoria
            for(int i=0; i<recom_pred_distance.size(); i++){        //gemizoume ti lista me ta pois pou anikoun sti katigoria pou thelei o user
                if(recom_pred_distance.get(i).getPOI_category_id().toLowerCase().equals(category.toLowerCase())){
                    recom_pred_dist_category.add(recom_pred_distance.get(i));
                }
            }
        }else{
            for(int i=0; i<recom_pred_distance.size(); i++){        //gemizoume ti lista me ola ta pois pou exoun apla ti swsti apostasi
                recom_pred_dist_category.add(recom_pred_distance.get(i));
            }
        }

        Collections.sort(recom_pred_dist_category, new Comparator<Poi>() {       //descending order-sort
            @Override
            public int compare(Poi p1, Poi p2) {
                if (p1.getPredictions() - p2.getPredictions() > 0) {
                    return -1;
                } else if (p1.getPredictions() - p2.getPredictions() == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        int stop = 1;               //edw metrame akrivws ton aritmo twn pois pou zitise o xristis- diladi K-pois
        if(K>=recom_pred_dist_category.size()){      //an o arithmos twn POIS pou zitaei o xristis einai megaluteros-isos apo osa proteinei to susthma mexri stigmis,
            for (int i=0; i<recom_pred_dist_category.size(); i++){ //apla antigrafoume ta Pois tis listas sti lista me ta BEST_POIS
                best_pois1.add(recom_pred_dist_category.get(i));
            }

        }else {     //o xristis zitaei mikrotero arithmo POIS apo osa proteinei to susthma,tote metrame osa pois zitaei o user kai ta kratame sti lista me ta BEST POIS
            for (Poi p : recom_pred_dist_category) {    //
                if (stop <= K) {
                    best_pois1.add(p);
                    stop++;
                } else {
                    break;
                }
            }
        }

        if(best_pois1.size()==0){       //an i lista me ta best pois einai adeia ,tote prosthetoume apla to tin topothesia tou xristi kai tin epistrefoume me id kata ena megalutero apo to id tou teleutaiou Poi
            best_pois1.add(new Poi("No POI_code",latitude,longitude,"No photo of user's location","User is probably outdoors","No name for user's location",user_row.length+1,0.0));
        }

        RealMatrix bestPoisID = new OpenMapRealMatrix(1, best_pois1.size());    //Enas RealMatrix me ta id twn BEST_POIS
        for (int i = 0; i < best_pois1.size(); i++) {
            bestPoisID.setEntry(0, i, best_pois1.get(i).getId());
        }

        System.out.println(bestPoisID.toString());

        double [] double_ids = bestPoisID.getRow(0);
        int [] int_ids = new int[double_ids.length];

        for (int i = 0; i < double_ids.length; ++i){
            int_ids[i] = (int) double_ids[i];
        }

        for( int id : int_ids){
            System.out.println("ID: " + id);
            System.out.println(poiList.get(id).toString());
        }

        return int_ids;
    }

    public double calculateError() {
        //upologismos c_ui*(p_ui-x_uT*y_i)^2
        double SUM_c_pui_xy = 0;
        for (int u = 0; u < X.getRowDimension(); u++) {
            for (int poi = 0; poi < Y.getRowDimension(); poi++) {
                double p_xtu = P_matrix.getEntry(u, poi) - ((X.getRowMatrix(u).multiply(Y.getRowMatrix(poi).transpose())).getEntry(0, 0));
                double tetragwno = Math.pow(p_xtu, 2);
                SUM_c_pui_xy = SUM_c_pui_xy + C_matrix.getEntry(u, poi) * tetragwno;
            }
        }

        double SUM_l_norms = l * (Math.pow(X.getFrobeniusNorm(), 2) + Math.pow(Y.getFrobeniusNorm(), 2));        //athroizw tis normes

        double cost = SUM_l_norms + SUM_c_pui_xy;          //upologismos sunolikoy kostous

        System.out.println("1o term: " + SUM_c_pui_xy);
        System.out.println("2o term: " + SUM_l_norms);
        System.out.println("SUNOLIKO KOSTOS: " + cost);

        return cost;
    }

    public ArrayList<ArrayList<Integer>> splitData(String matrix_name, int total_cores) {
        int total_indexes;

        ArrayList<ArrayList<Integer>> worker_list = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> core_list = new ArrayList<ArrayList<Integer>>();

        for (int i = 0; i < WORKERS; i++) {
            ArrayList<Integer> list = new ArrayList<Integer>();

            worker_list.add(list);
        }


        for (int i = 0; i < total_cores; i++) {
            ArrayList<Integer> list = new ArrayList<Integer>();

            core_list.add(list);
        }

        if (matrix_name.equals("X")) {
            total_indexes = R_matrix.getRowDimension();
        } else if (matrix_name.equals("Y")) {
            total_indexes = R_matrix.getColumnDimension();
        } else {
            System.out.println("Wrong matrix!");
            return null;
        }

        /* Etoimh h list me ta cores works */
        for (int index = 0; index < total_indexes; index++) {
            int core = index % total_cores;
            core_list.get(core).add(index);
        }

        int from = 0;
        int to = 0;
        for (int worker = 0; worker < WORKERS; worker++) {
            to = from + cores[worker] - 1;
            for (int list = from; list <= to; list++) {

                for (int index : core_list.get(list)) {
                    worker_list.get(worker).add(index);
                }
            }
            from = to + 1;
        }

        return worker_list;
    }


    public void receive(ObjectInputStream in) {
        try {
            received = (Data) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void send(ObjectOutputStream out, Data send) {
        try {
            out.reset();
            out.writeObject(send);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setXpart() {

        ArrayList<Integer> user_list = received.indexes;
        OpenMapRealMatrix x_part = received.matrix_part;

        for (int user : user_list) {
            for (int feature = 0; feature < features; feature++) {
                X.setEntry(user, feature, x_part.getEntry(user, feature));
            }
        }
    }

    public void setYpart() {
        ArrayList<Integer> poi_list = received.indexes;
        OpenMapRealMatrix y_part = received.matrix_part;

        for (int poi : poi_list) {
            for (int feature = 0; feature < features; feature++) {
                Y.setEntry(poi, feature, y_part.getEntry(poi, feature));
            }
        }
    }

    /* Opens server and accepts connections */
    public void openServer() {

        int ini_worker = 0;
        int total_cores = 0;

        try {
            /* Create Server Socket */
            InetAddress addr = InetAddress.getByName(IP);
            providerSocket = new ServerSocket(PORT, 20,addr);
            System.out.printf("Server running at port [%d]\n", PORT);

            /* dexou sundeseis kai arxikopoihse */
            while (ini_worker < WORKERS) {
                System.out.println("Waiting for connection...");
                clientSocket = providerSocket.accept();
                System.out.printf("Got a new connection! ClientSocket port[%d]\n", clientSocket.getPort());

                /* Arxikopoihse ta connections */
                workerSockets[ini_worker] = clientSocket;
                in[ini_worker] = new ObjectInputStream(clientSocket.getInputStream());
                out[ini_worker] = new ObjectOutputStream(clientSocket.getOutputStream());

                ini_worker++;
            }

            /* Thelw ta specs sas */
            System.out.println("Asking for Worker Specs!");
            for (int i = 0; i < WORKERS; i++) {
                send(out[i], new Data("specs"));
            }

            System.out.println("Waiting...");
            for (int i = 0; i < WORKERS; i++) {
                receive(in[i]);
                cores[i] = received.cores;
                ram[i] = received.ram;

                total_cores += received.cores;
            }

            System.out.println("Got Worker Specs!");
            for (int i = 0; i < WORKERS; i++) {
                System.out.print("Worker " + i + ":");
                System.out.print("( cores =  " + cores[i]);
                System.out.println(", ram = " + ram[i] + ")");
            }

            /* Now that you have the cores lets split */
            ArrayList<ArrayList<Integer>> splitted_X_matrix = splitData("X", total_cores);
            ArrayList<ArrayList<Integer>> splitted_Y_matrix = splitData("Y", total_cores);

            System.out.println("Initialize your Matrices!");
            for (int i = 0; i < WORKERS; i++) {
                send(out[i], new Data(R_matrix, X, Y, "init"));
            }

            System.out.println("Waiting...");
            for (int i = 0; i < WORKERS; i++) {
                receive(in[i]);
            }
            System.out.println("Initializing done!");

            System.out.println("Starting Recommendation Training Phase");
            int loop = 0;

            int y_prints = 0;
            int x_prints = 0;
            while (training_phase) {

                /* UPOLOGISE TO X */
                System.out.println("Calculate the X matrix!");
                for (int i = 0; i < WORKERS; i++) {
                    send(out[i], new Data(splitted_X_matrix.get(i), "X"));
                }

                System.out.println("Waiting for x parts...");
                for (int i = 0; i < WORKERS; i++) {
                    receive(in[i]);
                    System.out.println(received.to_calculate);
                    setXpart();
                    x_prints++;
                    System.out.println("x_part_set_successful: " + x_prints);
                }
                System.out.println("------Full X calculation DONE!------");

                /* Kane update ton X stous Workers */
                System.out.println("Sending updated X matrix to Workers...");
                for (int i = 0; i < WORKERS; i++) {
                    send(out[i], new Data(X, "update X"));
                } // wait...
                for (int i = 0; i < WORKERS; i++) {
                    receive(in[i]);
                }
                System.out.println("Updating X matrix in Workers done!");


                /* UPOLOGISE TO Y */
                System.out.println("Calculate the Y matrix!");
                for (int i = 0; i < WORKERS; i++) {
                    send(out[i], new Data(splitted_Y_matrix.get(i), "Y"));
                }

                System.out.println("Waiting for y parts...");
                for (int i = 0; i < WORKERS; i++) {
                    receive(in[i]);
                    System.out.println(received.to_calculate);
                    setYpart();
                    y_prints++;
                    System.out.println("y_part_set_successful: " + y_prints);
                }
                System.out.println("------Full Y calculation DONE!------");

                /* Kane update ton Y stous Workers */
                System.out.println("Sending updated Y matrix to Workers...");
                for (int i = 0; i < WORKERS; i++) {
                    send(out[i], new Data(Y, "update Y"));
                } // perimene
                for (int i = 0; i < WORKERS; i++) {
                    receive(in[i]);
                }
                System.out.println("Updating Y matrix in Workers done!");

                loop++;

                double err = calculateError();

                /* kane tin sunthikh termatismou */
                if (loop == iterations || Math.abs(min - err) <= threshold) {
                    System.out.println("Terminating Workers...");
                    for (int i = 0; i < WORKERS; i++) {
                        send(out[i], new Data("done"));
                    }
                    training_phase = false;
                }
                min = err;
            }

            /* Now handle the com.example.user.myapplicationrecopoi.Client */
            calculatePredictions();

            System.out.println("Starting Client phase!");
            while (client_phase) {
                /* Accept the connection */
                System.out.println("Waiting for Client connection...");

                /* For every providerSocket.accept, a new Socket it created
                 * that is used to communicate with the Slave */
                clientSocket = providerSocket.accept();
                System.out.printf("Got a Client! ClientSocket port[%d]\n", clientSocket.getPort());

                /* ---- HANDLE THE CONNECTED CLIENT ---- */

                Thread clientHandler = new HandleClient(clientSocket);
                clientHandler.start();
                try {
                    clientHandler.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Couldnt wait for Client!");
                }
            }
            System.out.println("Terminating server...");

        } catch (IOException ioException) {
            ioException.printStackTrace();

        } finally {
            try {
                for (int i = 0; i < WORKERS; i++) {
                    in[i].close();
                    out[i].close();
                    workerSockets[i].close();
                }
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void calculateFile_R_MatrixDimensions(String path) {
        /* Open the com.example.user.myapplicationrecopoi.Data file */
        Scanner scanner1 = null;
        try {
            /* Test data */
            scanner1 = new Scanner(new File(path));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<Integer> user_id = new ArrayList<Integer>();
        ArrayList<Integer> poi_id = new ArrayList<Integer>();

        /* Scan the data file */
        while (scanner1.hasNextLine()) {
            String line = scanner1.nextLine();
            String array1[] = line.split(", ");
            String a = array1[0];             //prwto stoixeio kathe grammis
            String b = array1[1];             //deutero
            String q = array1[2];             //trito

            int f = Integer.parseInt(a.trim());
            int k = Integer.parseInt(b.trim());
            double g = Double.parseDouble(q.trim());
            user_id.add(f);
            poi_id.add(k);
        }
        USERS = user_id.get(user_id.size() - 1) + 1;
        Collections.sort(poi_id);
        POIS = poi_id.get(poi_id.size() - 1) + 1;
        scanner1.close();
    }

    public void ReadFileRMatrix(String path) {
        /* Open the com.example.user.myapplicationrecopoi.Data file */
        Scanner scanner2 = null;
        try {
            scanner2 = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        /* Scan the data file */
        while (scanner2.hasNextLine()) {
            String line = scanner2.nextLine();
            String array1[] = line.split(", ");
            String a = array1[0];             //prwto stoixeio kathe grammis
            String b = array1[1];             //deutero
            String q = array1[2];             //trito

            int f = Integer.parseInt(a.trim());
            int k = Integer.parseInt(b.trim());
            double g = Double.parseDouble(q.trim());
            R_matrix.setEntry(f, k, g);
        }
        scanner2.close();
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.initialize();
    }
}
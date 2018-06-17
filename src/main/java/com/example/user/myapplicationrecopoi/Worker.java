package com.example.user.myapplicationrecopoi;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Worker extends Thread {

    /*----------SOCKET VARIABLES-----------*/
    final String IP = "localhost";

    /*----------WORKER specs-----------*/
    int cores = 0;
    long ram = 0;

    /* ---- SOCKET VARIABLES ---- */
    private final int port = 60000;
    private Socket workerSocket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    Data received;

    /* ---- MATRICES ---- */
    private OpenMapRealMatrix R_matrix;
    private OpenMapRealMatrix X;
    private OpenMapRealMatrix Y;
    private OpenMapRealMatrix C;
    private OpenMapRealMatrix P;
    private RealMatrix X_I;
    private RealMatrix Y_I;
    private RealMatrix lI;
    private boolean initialized_worker = false;

    /* ---- IMPLICIT CALCULATION VARIABLES ---- */
    private double l = 0.1;
    private int features = 10; // todo NEEDS TO BE CHANGED!

    public Worker() {

        try {
            /* Create socket for contacting the server on port 4321 */
            workerSocket = new Socket(IP, port);

            /* Create the streams to send and receive data from server */
            out = new ObjectOutputStream(workerSocket.getOutputStream());
            in = new ObjectInputStream(workerSocket.getInputStream());

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /* com.example.user.myapplicationrecopoi.Worker simulation */
    public static void main(String args[]) {
        /* Lets create 3 Slave Computers and try a test... */
        Worker pc1 = new Worker();
        pc1.setName("PC1");

        Worker pc2 = new Worker();
        pc2.setName("PC2");

        Worker pc3 = new Worker();
        pc3.setName("PC3");

        /* Set custom cores to test fair distribution */
//        pc1.cores = 1;
//        pc2.cores = 1;
//        pc3.cores = 2;

        pc1.start();
        pc2.start();
        pc3.start();
    }

    /* ---- MATRIX CALCULATION FUNCTIONS ---- */

    public void initialize() {
        R_matrix = received.r_matrix;
        calculateCMatrix(40, R_matrix);
        calculatePMatrix(R_matrix);
        calculateUnitaryMatrices();
        X = received.x_matrix;
        Y = received.y_matrix;
        initialized_worker = true;
    }

    /* Initialize the C matrix */
    public void calculateCMatrix(int a, RealMatrix matrix) {
        C = new OpenMapRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                C.setEntry(i, j, (1 + a * matrix.getEntry(i, j)));
            }
        }
    }

    /* Initialise the P matrix (do this here or in master ?)*/
    private void calculatePMatrix(RealMatrix matrix) {
        P = new OpenMapRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                P.setEntry(i, j, matrix.getEntry(i, j) > 0 ? 1 : 0);
            }
        }
    }

    public RealMatrix preCalculateXX_YY(RealMatrix matrix) {
        return matrix.transpose().multiply(matrix);
    }

    public void calculateUnitaryMatrices() {
        //Dimiourgia pinaka X_I
        int user_size = R_matrix.getRowDimension();
        int pois_size = R_matrix.getColumnDimension();
        double[] diag1 = new double[user_size];
        for (int fill = 0; fill < user_size; fill++) {
            diag1[fill] = 1;
        }
        X_I = MatrixUtils.createRealDiagonalMatrix(diag1);

        //Dimiourgia pinaka Y_I
        double[] diag2 = new double[pois_size];
        for (int fill = 0; fill < pois_size; fill++) {
            diag2[fill] = 1;
        }
        Y_I = MatrixUtils.createRealDiagonalMatrix(diag2);

        //Dimiourgia  I
        RealMatrix I;
        double[] diag3 = new double[features];
        for (int fill = 0; fill < features; fill++) {
            diag3[fill] = 1;
        }
        I = MatrixUtils.createRealDiagonalMatrix(diag3);
        //Upologismos lI ,estw l=0.1
        lI = I.scalarMultiply(l);
    }

    public OpenMapRealMatrix calculate_x_u(RealMatrix c, RealMatrix Y) {

        ArrayList<Integer> x_split = received.indexes;
        int user_size = X.getRowDimension();
        RealMatrix user_row;
        RealMatrix p_u;

        RealMatrix yTy = Y.transpose().multiply(Y);

        for (int j : x_split) {

            // gia kathe user ,pairnoume kathe grammi
            user_row = c.getRowMatrix(j);

            // for each user u define diagonal n x n matrix Cu
            double[] diag = user_row.getRow(0);
            RealMatrix Cu = MatrixUtils.createRealDiagonalMatrix(diag);
            // and also vector p(u)
            p_u = P.getRowMatrix(j);


            //OLOS O TUPOS
            RealMatrix CuI = Cu.subtract(Y_I);           // (Cu - I)
            RealMatrix yT_CuI_y = Y.transpose().multiply(CuI).multiply(Y);  //Y^T*(C^u-I)*Y
            RealMatrix yTCuY = yTy.add(yT_CuI_y);     //Y^T*C^u*Y = Y^T*Y + Y^T*(C^u-I)*Y
            RealMatrix yTCuY_lI = yTCuY.add(lI);      //Y^T*C^u*Y+l*I
            RealMatrix Inverse_part = new LUDecomposition(yTCuY_lI).getSolver().getInverse();//(Y^T*C^u*Y+l*I)^(-1)

            RealMatrix yT_Cu_pu = Y.transpose().multiply(Cu).multiply(p_u.transpose()); //Y^T*C^u*p(u)^T
            X.setRowMatrix(j, (Inverse_part.multiply(yT_Cu_pu)).transpose());
        }

        System.out.println("X after calculateX in worker:\n " + X);
        return X.copy();
    }

    public OpenMapRealMatrix calculate_y_i(RealMatrix c, RealMatrix X) {

        ArrayList<Integer> y_split = received.indexes;
        RealMatrix pois_row;
        RealMatrix p_i;

        /* ---- new code ----- */

        RealMatrix xTx = preCalculateXX_YY(X);
        for (int poi : y_split) {

            pois_row = c.getColumnMatrix(poi).transpose();      //gia kathe poi,pairnoume kathe stili kai kanoume transpose

            p_i = P.getColumnMatrix(poi).transpose();

            //ftiaxnw ton CiI
            double[] diag4 = pois_row.getRow(0);

            RealMatrix Ci = MatrixUtils.createRealDiagonalMatrix(diag4);
            RealMatrix CiI = Ci.subtract(X_I);
            //OLOS O TUPOS

            RealMatrix xT_CiI_x = X.transpose().multiply(CiI).multiply(X);  //X^T*(C^i-I)*X
            RealMatrix xTCiX = xTx.add(xT_CiI_x);     //X^T*C^i*X = X^T*X + X^T*(C^i-I)*X
            RealMatrix xTCiX_lI = xTCiX.add(lI);      //X^T*C^i*X+l*I
            RealMatrix Inverse_part2 = new LUDecomposition(xTCiX_lI).getSolver().getInverse();//(X^T*C^i*X+l*I)^(-1)

            RealMatrix xT_Ci_pi = X.transpose().multiply(Ci).multiply(p_i.transpose()); //X^T*C^i*p(i)^T

            Y.setRowMatrix(poi, (Inverse_part2.multiply(xT_Ci_pi)).transpose());

        }
        System.out.println("Y after calculateY in worker:\n " + Y);
        return Y.copy();
    }

    /* ---- SIMULATION FUNCTIONS ---- */
    public synchronized void receive() {
        try {
            received = (Data) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized void send(Data send) {
        try {
            out.reset();
            out.writeObject(send);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void terminateSocket() {
        try {
            in.close();
            out.close();
            workerSocket.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /* These will be the actions performed */
    public void run() {
        boolean exit = false;
        String name = this.getName();

        System.out.println("Hello from Worker : " + name);

        while (!exit) {

            receive();

            if (received.to_calculate.equals("specs")) {
                if (cores == 0) {
                    cores = Runtime.getRuntime().availableProcessors();
                }
                ram = Runtime.getRuntime().freeMemory();
                send(new Data(cores, ram));
            } else if (received.to_calculate.equals("init")) {
                initialize();
                System.out.println("Ekana to initialize mou!");
                send(new Data(name + " : initialized! "));
            } else if (received.to_calculate.equals("X")) {
                System.out.println(name + ": working on x_split...");
                send(new Data(received.indexes, calculate_x_u(C, Y), "x_part_calculated!"));
            } else if (received.to_calculate.equals("Y")) {
                System.out.println(name + ": working on y_split...");
                send(new Data(received.indexes, calculate_y_i(C, X), "x_part_calculated!"));
            } else if (received.to_calculate.equals("done")) {
                System.out.println(name + ": Molis teleiwsa!");
                exit = true;
                terminateSocket();
            } else if (received.to_calculate.equals("update X")) {
                X = received.matrix;
                send(new Data("updated X"));
            } else if (received.to_calculate.equals("update Y")) {
                Y = received.matrix;
                send(new Data("updated Y"));
            }
        }
    }
}
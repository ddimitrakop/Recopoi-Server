//	DIMITRAKOPOULOS DIMITRIOS_3130053
//	KOURLI DIMITRA_3150081
//	KOUTSOMIXOU EUAGGELIA_3130103
//	VASILOU PARASKEVI_3150008

package com.example.user.myapplicationrecopoi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class HandleClient extends Thread {
    Socket connection;
    ObjectInputStream in = null;
    ObjectOutputStream out = null;

    Data received = null;

    HandleClient(Socket connection) {
        this.connection = connection;

        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error parsing OutputStream and InputStream!");
        }
    }

    private void readDataPacket() {
        try {
            received = (Data) in.readObject();
            System.out.println("To diavase o HandleClient!!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDataPacket(Data packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void terminateHandler() {
        if (!connection.isClosed()) {
            try {
                in.close();
                out.close();
                connection.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("COULDNT CLOSE IN/OUT/CONNECTION IN HANDLER!!");
            }
            System.out.println("COULDNT CLOSE HANDLER SOCKET");
        }
    }

    public void run() {

        int[] pred;
        readDataPacket();

        while (!received.to_calculate.equals("exit")) {
            pred= Master.calculateBestLocalPoisForUser(received.user, received.k,received.lati,received.longi,received.category,received.dist);

            sendDataPacket(new Data(pred));
            /* Read the next request */
            readDataPacket();
            received.to_calculate = "exit";
        }
        /* Signal com.example.user.myapplicationrecopoi.Master that the com.example.user.myapplicationrecopoi.Client phase has ended */
        Master.client_phase = true;
        /* If com.example.user.myapplicationrecopoi.Client wasnt to exit Terminate the Connection */
        terminateHandler();
    }
}
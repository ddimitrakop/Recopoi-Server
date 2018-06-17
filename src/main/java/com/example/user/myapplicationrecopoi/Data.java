package com.example.user.myapplicationrecopoi;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.Serializable;
import java.util.ArrayList;

public class Data implements Serializable {
    /* Contains the matrix (X , Y or predictions) with the calculated results */
    /* What the com.example.user.myapplicationrecopoi.Worker needs to calculate with the data */
    String to_calculate = "null";
    int cores;
    long ram;

    int user;
    int k;

    double lati;
    double longi;
    String category;
    double dist;

    RealMatrix predictions;
    ArrayList<Integer> indexes;
    OpenMapRealMatrix matrix_part;
    OpenMapRealMatrix matrix;
    OpenMapRealMatrix r_matrix;
    OpenMapRealMatrix x_matrix;
    OpenMapRealMatrix y_matrix;

    public Data(String to_calculate) {
        this.to_calculate = to_calculate;
    }

    public Data(int cores, long ram) {
        this.cores = cores;
        this.ram = ram;
    }

    /* Used from HandleWorker to terminate the com.example.user.myapplicationrecopoi.Worker */
    public Data(RealMatrix predictions) {
        this.predictions = predictions;
    }

    public Data(ArrayList<Integer> indexes, String to_calculate) {
        this.indexes = indexes;
        this.to_calculate = to_calculate;
    }

    public Data(ArrayList<Integer> indexes, OpenMapRealMatrix matrix_part, String to_calculate) {
        this.indexes = indexes;
        this.matrix_part = matrix_part;
        this.to_calculate = to_calculate;
    }

    public Data(OpenMapRealMatrix matrix, String to_calculate) {
        this.matrix = matrix;
        this.to_calculate = to_calculate;
    }

    public Data(OpenMapRealMatrix r_matrix, OpenMapRealMatrix x_matrix, OpenMapRealMatrix y_matrix, String to_calculate) {
        this.r_matrix = r_matrix;
        this.x_matrix = x_matrix;
        this.y_matrix = y_matrix;
        this.to_calculate = to_calculate;
    }

    /* This is used by com.example.user.myapplicationrecopoi.Client to ask for recommendations*/
    public Data(int user, int k) {
        this.user = user;
        this.k = k;
    }

    /*This is used by Client to ask for recommendations*/
    public Data(int user, int k, double lati, double longi, String category, double dist) {
        this.user = user;
        this.k = k;
        this.lati=lati;
        this.longi=longi;
        this.category=category;
        this.dist=dist;
    }

    int[] pred;
    /* This is used by com.example.user.myapplicationrecopoi.Client to ask for recommendations*/
    public Data(int [] pred) {
        this.pred = pred;
    }

    @Override
    public String toString() {
        return "DATA_PACKET: calculate =" + to_calculate + ", cores = " + cores + ", ram = " + ram + "\n";
    }
}
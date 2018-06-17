//	DIMITRAKOPOULOS DIMITRIOS_3130053
//	KOURLI DIMITRA_3150081
//	KOUTSOMIXOU EUAGGELIA_3130103
//	VASILOU PARASKEVI_3150008

package com.example.user.myapplicationrecopoi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Map;

public class JSONParser {

    /* Testing how to parse the json file using the Google GSON API */

    public static Map<Integer, Poi> parsePois(){
        String path = "src/main/datasets/POIs.json";
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            System.out.println("Couldnt read the file!");
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<Map<Integer, Poi>>() {
        }.getType();
        Map<Integer, Poi> poiList = gson.fromJson(bufferedReader, type);


        return poiList;
    }
}


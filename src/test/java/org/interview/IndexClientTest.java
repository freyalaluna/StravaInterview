package org.interview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexClientTest {
    IndexClient client = new IndexClient("https://flaluna-interview.free.beeceptor.com/_cat/indicies/*2025*4*19");
    Gson gson = new GsonBuilder().create();

    @Test
    void getIndices() throws IOException, InterruptedException {
        String indicesString = client.getIndices();
        assertNotNull(indicesString);

        IndexInfo[] infoArray = gson.fromJson(indicesString, IndexInfo[].class);
        List<IndexInfo> infoList = new ArrayList<>(Arrays.asList(infoArray));

        for(IndexInfo info : infoList) {
            System.out.println(info.getIndexName());
            System.out.println(info.getPri());
            System.out.println(info.getPriStoreSize());
        }
    }

    @Test
    void getIndicesMultipleDays() throws IOException, InterruptedException {
        List<IndexInfo> infoList = new ArrayList<>();
        for(int i = 19; i > 16; i--){
            client.setUrl("https://flaluna-interview.free.beeceptor.com/_cat/indicies/*2025*4*" + i);
            String indicesString = client.getIndices();
            IndexInfo[] infoArray = gson.fromJson(indicesString, IndexInfo[].class);
            infoList.addAll(Arrays.asList(infoArray));
        }

        assertEquals(10, infoList.size());
        for(IndexInfo info : infoList) {
            System.out.println(info.getIndexName());
            System.out.println(info.getPri());
            System.out.println(info.getPriStoreSize());
        }
    }
}
package org.interview;

import com.google.gson.annotations.SerializedName;

public class IndexInfo {
    IndexInfo(String aIndexName, long aPriStoreSize, int aPri){
        indexName = aIndexName;
        priStoreSize = aPriStoreSize;
        pri = aPri;
    }

    //Used for easy conversion between data sizes
    //Uses SI standard for byte size (1000 bytes per kilobyte)
    public double convertBytesToGB(){
        return (double)(priStoreSize / Math.pow(1000, 3));
    }

    //Getters
    public String getIndexName() {
        return indexName;
    }
    public long getPriStoreSize() {
        return priStoreSize;
    }
    public int getPri() {
        return pri;
    }

    //Setters
    public void setIndexName(String aIndexName) {
        indexName = aIndexName;
    }
    public void setPriStoreSize(long aPriStoreSize) {
        priStoreSize = aPriStoreSize;
    }
    public void setPri(int aPri) {
        pri = aPri;
    }

    @SerializedName("index")
    String indexName;
    @SerializedName("pri.store.size")
    long priStoreSize;
    int pri;
}
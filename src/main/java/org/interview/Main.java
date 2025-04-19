package org.interview;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {
    /*
        display the top 5 largest indexes by size with their name and size in human readable format (GBs, not bytes)
        display the top 5 largest indexes by shard count with their name
        An index should have 1 shard for every 30 GB of data it stores.
        Some indexes are storing WAY too much data for their shard count.
        Find the top 5 biggest offenders and offer a new shard count recommendation.
        (For example, an index with 2,000 GB and 20 shards has a “ratio” of 100. Ideally, this index should have at least 67 shards allocated.)
     */
    public static void main(String[] args) {
        String dataFile = "src/main/resources/input.json";

        // Default flag values.
        String endpoint = "";
        boolean debug = false;
        int days = 7;

        for(int i = 0; i < args.length; i++) {
            if (args[i].equals("--debug")) {
                debug = true;
            } else if (args[i].equals("--endpoint")) {
                if (i+1 < args.length) {
                    endpoint = args[++i];
                }
            } else if (args[i].equals("--days")){
                if (i+1 < args.length) {
                    try {
                        days = Integer.parseInt(args[i++]);
                    } catch (NumberFormatException e) {
                        System.err.println(e);
                        System.exit(1);
                    }
                }
            } else {
                System.err.println("Error: unrecognized argument: " + args[i]);
            }
        }

        /*
            You'll need to build a query string containing year, month, day. Each API call should request 1 day worth of data (e.g. if today is April 15, 2025, the parameters would be year 2025, month 04, day 14).
            Query string is in the form of "https://<ENDPOINT>/_cat/indices/*<YEAR>*<MONTH>*<DAY>?v&h=index,pri.store.size,pri&format=json&bytes=b"
        */

        List<IndexInfo> data = null;
        try {
            if (debug) {
                data = getDataFromFile(dataFile);
            } else {
                data = getDataFromServer(endpoint, days);
            }
        } catch (Exception e) {
            System.err.println("Error reading data: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

//        for(IndexInfo i : data) {
//            System.out.println("Index: " + i.indexName);
//            System.out.println("priStoreSize: " + i.priStoreSize);
//            System.out.println("pri: " + i.pri);
//        }

        printLargestIndexes(data);
        printMostShards(data);
        printLeastBalanced(data);
    }

    public static List<IndexInfo> getDataFromFile(String inputFile) throws Exception {
        String fileJson = Files.readString(Paths.get(inputFile).toAbsolutePath());
        return parseIndexList(fileJson);
    }

    public static List<IndexInfo> getDataFromServer(String endpoint, int days) {
        List<IndexInfo> indexList = null;
        return indexList;
    }

    public static List<IndexInfo> parseIndexList(String jsonSource) {
        Gson gson = new GsonBuilder().create();
        IndexInfo[] indexArray = gson.fromJson(jsonSource, IndexInfo[].class);
        return new ArrayList<>(Arrays.asList(indexArray));
    }

    public static void printLargestIndexes(List<IndexInfo> data) {
        System.out.println("Printing largest indexes by storage size");
        int largestIndexesToPrint = 5;

        PriorityQueue<IndexInfo> largestIndexes = new PriorityQueue<IndexInfo>(
                Comparator.comparingLong(index -> index.priStoreSize));

        Stack<IndexInfo> stack = getLargest(largestIndexes, data, largestIndexesToPrint);
        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            double sizeInGB = topIndex.convertBytesToGB();

            System.out.println("Index: " + topIndex.indexName);
            System.out.printf("Size: %.2f GB\n", sizeInGB);
        }
        System.out.println();
    }

    public static void printMostShards(List<IndexInfo> data) {
        System.out.println("Printing largest indexes by shard count");
        int mostShardsToPrint = 5;

        PriorityQueue<IndexInfo> largestIndexes = new PriorityQueue<IndexInfo>(
                Comparator.comparingInt(index -> index.pri));

        Stack<IndexInfo> stack = getLargest(largestIndexes, data, mostShardsToPrint);
        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            System.out.println("Index: " + topIndex.indexName);
            System.out.println("Shards: " + topIndex.pri);
        }
        System.out.println();
    }

    public static void printLeastBalanced(List<IndexInfo> data) {
        int leastBalancedToPrint = 5;

        PriorityQueue<IndexInfo> largestIndexes = new PriorityQueue<IndexInfo>(
                Comparator.comparingDouble(index -> index.convertBytesToGB()/index.pri));

        Stack<IndexInfo> stack = getLargest(largestIndexes, data, leastBalancedToPrint);

        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            //TODO: Find a more elegant way to handle ratio rounding
            double sizeInGB = topIndex.convertBytesToGB();
            int currentShardRatio = (int) Math.ceil(sizeInGB/topIndex.pri);
            int recommendedShardRatio = (int) Math.ceil(sizeInGB / 30);

            System.out.println("Index: " + topIndex.indexName);
            System.out.printf("Size: %.2f GB\n", sizeInGB);
            System.out.println("Shards: " + topIndex.pri);
            System.out.println("Balance Ratio: " + currentShardRatio);
            System.out.println("Recommended Shard count is " + recommendedShardRatio);

        }
        System.out.println();
    }

    public static Stack<IndexInfo> getLargest(PriorityQueue<IndexInfo> queue, List<IndexInfo> data, int size) {
        for(IndexInfo i : data) {
            queue.add(i);

            if(queue.size() > size) {
                queue.poll();
            }
        }

        Stack<IndexInfo> stack = new Stack<>();
        while(!queue.isEmpty()) {
            IndexInfo topIndex = queue.poll();
            stack.push(topIndex);
        }

        return stack;

    }
}
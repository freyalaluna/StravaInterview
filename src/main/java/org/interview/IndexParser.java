package org.interview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IndexParser {

    public static void main(String[] args) {
        String dataFile = "src/main/resources/input.json";

        // Default flag values.
        String endpoint = "";
        boolean debug = false;
        int days = 7;

        // Flag handling
        for(int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--debug" -> debug = true;
                case "--endpoint" -> {
                    if (i + 1 < args.length) {
                        endpoint = args[++i];
                    } else {
                        System.err.println("--endpoint must be supplied with a URI");
                        System.exit(1);
                    }
                }
                case "--days" -> {
                    if (i + 1 < args.length) {
                        try {
                            days = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println(e);
                            System.exit(1);
                        }
                    } else {
                        System.err.println("--days must be supplied with an integer");
                        System.exit(1);
                    }
                }
                default -> System.err.println("Error: unrecognized argument: " + args[i]);
            }
        }

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

        printLargestIndexes(data);
        printMostShards(data);
        printLeastBalanced(data);
    }

    // Extracts the JSON contents of the test file as a String, and converts it to a List<IndexInfo>
    public static List<IndexInfo> getDataFromFile(String inputFile) throws Exception {
        String fileJson = Files.readString(Paths.get(inputFile).toAbsolutePath());
        return parseIndexList(fileJson);
    }

    // Queries the given endpoint for the past n days, where n = the days argument, and returns the list of all indices as IndexInfo objects.
    // Each API call should request 1 day worth of data (e.g. if today is April 15, 2025, the parameters would be year 2025, month 04, day 14).
    // Query string is in the form of "https://<ENDPOINT>/_cat/indices/*<YEAR>*<MONTH>*<DAY>?v&h=index,pri.store.size,pri&format=json&bytes=b"
    public static List<IndexInfo> getDataFromServer(String endpoint, int days) throws IOException, InterruptedException {
        String urlBase = "https://" + endpoint + "/_cat/indicies/";
        String urlQueryParams = "?v&h=index,pri.store.size,pri&format=json&bytes=b";

        // A GregorianCalendar is used to keep track of the current year/month/day being requested,
        // and handles edge cases like months/years rolling over during repeated requests
        Calendar calendar = Calendar.getInstance();
        GregorianCalendar gc = (GregorianCalendar) calendar;

        IndexClient client = new IndexClient(urlBase);
        List<IndexInfo> serverInfoIndices = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            int year = gc.get(Calendar.YEAR);
            int month = gc.get(Calendar.MONTH)+1;
            int day = gc.get(Calendar.DAY_OF_MONTH)-1;

            String urlString = urlBase + "*" + year + "*" + month + "*" + day + urlQueryParams;
            client.setUrl(urlString);
            String endpointIndices = client.getIndices();

            // Converts the JSON string into a List<InfoIndex>,
            // which is concatenated to our running collection
            serverInfoIndices.addAll(parseIndexList(endpointIndices));

            gc.add(Calendar.DATE, -1);
        }

        return serverInfoIndices;
    }


    // A helper function that uses the Gson library to convert the incoming JSON array string into a List of IndexInfo objects
    public static List<IndexInfo> parseIndexList(String jsonSource) {
        Gson gson = new GsonBuilder().create();
        IndexInfo[] indexArray = gson.fromJson(jsonSource, IndexInfo[].class);
        return new ArrayList<>(Arrays.asList(indexArray));
    }

    //Collects and prints the five indexes holding the largest amounts of data
    public static void printLargestIndexes(List<IndexInfo> data) {
        System.out.println("Printing largest indexes by storage size");
        int largestIndexesToPrint = 5;

        PriorityQueue<IndexInfo> largestIndexes = new PriorityQueue<IndexInfo>(
                Comparator.comparingLong(IndexInfo::getPriStoreSize));

        Stack<IndexInfo> stack = getLargest(largestIndexes, data, largestIndexesToPrint);
        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            double sizeInGB = topIndex.convertBytesToGB();

            System.out.println("Index: " + topIndex.getIndexName());
            System.out.printf("Size: %.2f GB\n", sizeInGB);
        }
        System.out.println();
    }

    // Collects and prints the five indexes with the most shards
    public static void printMostShards(List<IndexInfo> data) {
        System.out.println("Printing largest indexes by shard count");
        int mostShardsToPrint = 5;

        PriorityQueue<IndexInfo> mostShards = new PriorityQueue<IndexInfo>(
                Comparator.comparingInt(IndexInfo::getPri));

        Stack<IndexInfo> stack = getLargest(mostShards, data, mostShardsToPrint);
        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            System.out.println("Index: " + topIndex.getIndexName());
            System.out.println("Shards: " + topIndex.getPri());
        }
        System.out.println();
    }

    // Collects and prints the five indexes with the highest Data/Shard ratio
    public static void printLeastBalanced(List<IndexInfo> data) {
        System.out.println("Printing least balanced indexes");
        int leastBalancedToPrint = 5;

        PriorityQueue<IndexInfo> highestRatio = new PriorityQueue<IndexInfo>(
                Comparator.comparingDouble(index -> index.convertBytesToGB()/index.getPri()));

        Stack<IndexInfo> stack = getLargest(highestRatio, data, leastBalancedToPrint);

        int recommendedShardSize = 30;
        while(!stack.isEmpty()) {
            IndexInfo topIndex = stack.pop();

            // Ratios are handled by ignoring the decimal part of the resulting value
            double sizeInGB = topIndex.convertBytesToGB();
            int currentShardRatio = (int) (sizeInGB/topIndex.getPri());
            int recommendedShardRatio = (int) (sizeInGB / recommendedShardSize);

            // If an index is smaller than the recommended shard size
            // it should still be afforded 1 shard
            recommendedShardRatio = Math.max(recommendedShardRatio, 1);

            System.out.println("Index: " + topIndex.getIndexName());
            System.out.printf("Size: %.2f GB\n", sizeInGB);
            System.out.println("Shards: " + topIndex.getPri());
            System.out.println("Balance Ratio: " + currentShardRatio);
            System.out.println("Recommended Shard count is " + recommendedShardRatio);

        }
        System.out.println();
    }

    // A helper function to handle sorting using a Priority Queue
    // Returns the top elements in a stack to ensure proper ordering
    // from largest to smallest
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
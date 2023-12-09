import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class Tuple<A, B, C> {
    A item1;
    B item2;
    C item3;

    Tuple(A item1, B item2, C item3) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }
}

class PD_IMFI {
    String item;
    int cur_Sup;
    int incr_Sup;
    int incr_Minsup;
    List<List<String>> list_IMFIs;

    PD_IMFI(String item, int cur_Sup, int incr_Sup, int incr_Minsup, List<List<String>> list_IMFIs) {
        this.item = item;
        this.cur_Sup = cur_Sup;
        this.incr_Sup = incr_Sup;
        this.incr_Minsup = incr_Minsup;
        this.list_IMFIs = list_IMFIs != null ? list_IMFIs : new ArrayList<>();
    }
}

class FPNode {
    String item;
    int count;
    FPNode parent;
    Map<String, FPNode> children;

    FPNode(String item, int count) {
        this.item = item;
        this.count = count;
        this.children = new HashMap<>();
        this.parent = null;
    }

    // Add this method to insert a transaction into the tree
    void addTransaction(List<String> transaction, int count) {
        FPNode currentNode = this;

        for (String item : transaction) {
            FPNode childNode;
            if (currentNode.children.containsKey(item)) {
                childNode = currentNode.children.get(item);
                childNode.count += count;
            } else {
                childNode = new FPNode(item, count);
                childNode.parent = currentNode;
                currentNode.children.put(item, childNode);
            }
            currentNode = childNode;
        }
    }
}

class FPTree {

    FPNode root;
    int minSupport;
    int totalTransactions;

    FPTree(List<List<String>> Di_plus_1, int minSupport) {
        this.minSupport = minSupport;
        this.root = new FPNode(null, 0);
        this.totalTransactions = Di_plus_1.size();
        buildFPTree(Di_plus_1); }

    private void buildFPTree(List<List<String>> Di_plus_1) {
        Map<String, Integer> itemFrequency = calculateItemFrequency(Di_plus_1);
        while (!itemFrequency.isEmpty()) {
            String maxItem = selectMaxFrequencyItem(itemFrequency.keySet(), itemFrequency);
            Set<String> filteredItems = filterItems(Collections.singletonList(maxItem), itemFrequency, minSupport)
                    .stream()
                    .sorted(Comparator.comparingInt(itemFrequency::get).reversed())
                    .collect(Collectors.toSet());
            String rootItem = selectMaxFrequencyItem(filteredItems, itemFrequency);
            if (rootItem != null) {
                root.addTransaction(new ArrayList<>(filteredItems), 1);
                itemFrequency.remove(rootItem); } } }

    private String selectMaxFrequencyItem(Set<String> items, Map<String, Integer> itemFrequency) {
        String maxItem = null;
        int maxFrequency = 0;

        for (String item : items) {
            int frequency = itemFrequency.getOrDefault(item, 0);
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
                maxItem = item;
            }
        }

        return maxItem;
    }

    private Map<String, Integer> calculateItemFrequency(List<List<String>> transactions) {
        Map<String, Integer> itemFrequency = new HashMap<>();
        for (List<String> transaction : transactions) {
            for (String item : transaction) {
                itemFrequency.put(item, itemFrequency.getOrDefault(item, 0) + 1);
            }
        }
        return itemFrequency;
    }

    // Add this method to filter items based on minSupport
    private Set<String> filterItems(List<String> transaction, Map<String, Integer> itemFrequency, int minSupport) {
        return transaction.stream()
                .filter(item -> itemFrequency.get(item) >= minSupport)
                .collect(Collectors.toSet());
    }

    // Add this method to traverse and print the tree
    private void inTree(FPNode node, String indent) {
        if (node != null) {
            System.out.println(indent + "├── " + (node.item != null ? node.item : "null") + " [count: " + node.count + "]");

            // Sort children by count in descending order and then by item name
            List<Map.Entry<String, FPNode>> sortedChildren = new ArrayList<>(node.children.entrySet());
            sortedChildren.sort((entry1, entry2) -> {
                int countComparison = Integer.compare(entry2.getValue().count, entry1.getValue().count);
                return countComparison != 0 ? countComparison : entry1.getKey().compareTo(entry2.getKey());
            });

            for (Map.Entry<String, FPNode> entry : sortedChildren) {
                String childItem = entry.getKey();
                FPNode childNode = entry.getValue();
                String childIndent = indent + "    ";
                inTree(childNode, childIndent);
            }
        }
    }
    // Add this method to find frequent itemsets
    Set<String> findMFIsSet(int minSupport) {
        Set<String> mfis = new HashSet<>();
        findMFIs(root, new ArrayList<>(), minSupport, mfis);
        return mfis;
    }
    
    private void findMFIs(FPNode node, List<String> currentPath, int minSupport, Set<String> mfis) {
        if (node != null) {
            if (node.count >= minSupport) {
                // Add the current path to the set of maximal frequent itemsets
                mfis.add(String.join(", ", currentPath));
            }
    
            // Continue traversing the tree recursively
            for (Map.Entry<String, FPNode> entry : node.children.entrySet()) {
                String childItem = entry.getKey();
                FPNode childNode = entry.getValue();
    
                // Create a new path with the current item
                List<String> newPath = new ArrayList<>(currentPath);
                newPath.add(childItem);
    
                // Recursive call to findMFIs for the child node
                findMFIs(childNode, newPath, minSupport, mfis);
            }
        }
    }

    // Add this method to add a transaction to the tree
    void addTransaction(List<String> transaction) {
        root.addTransaction(transaction, 1); // Adjust count as needed
    }
    
    void printTree() {
        inTree(root, "");
    }
}

public class Main {

    private static long startTime;

    public static List<List<String>> readDataFromCSV(String filePath) {
        List<List<String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Remove double quotes around each item
                List<String> items = Arrays.asList(line.split(",")).stream()
                        .map(item -> item.replaceAll("\"", ""))
                        .collect(Collectors.toList());
                data.add(items);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static double calculateNovelty(List<String> itemset, List<List<String>> Di_plus_1) {
        // Create a set to store unique items in the itemset
        Set<String> uniqueItemsInItemset = new HashSet<>(itemset);
    
        // Create a set to store unique items in Di_plus_1
        Set<String> uniqueItemsInDi_plus_1 = new HashSet<>();
        for (List<String> transaction : Di_plus_1) {
            uniqueItemsInDi_plus_1.addAll(transaction);
        }
    
        // Calculate novelty based on the ratio of unique items in the itemset to total unique items in Di_plus_1
        double novelty = (double) uniqueItemsInItemset.size() / uniqueItemsInDi_plus_1.size();
    
        return novelty;
    }

    public static List<PD_IMFI> createPDIMFIAlgorithm(List<List<String>> Di_plus_1, List<PD_IMFI> Pi, double minSup) {
        Set<String> uniqueItems = new HashSet<>();
        for (List<String> transaction : Di_plus_1) {
            uniqueItems.addAll(transaction); }

        List<PD_IMFI> Pi_plus_1 = new ArrayList<>();
        for (String item : uniqueItems) {
            Pi_plus_1.add(new PD_IMFI(item, 0, 0, 0, null));}

        for (List<String> T : Di_plus_1) {
            for (String I : T) {
                boolean found = false;
                for (PD_IMFI P : Pi_plus_1) {
                    if (I.equals(P.item)) {
                        P.cur_Sup++;
                        P.incr_Sup++;
                        found = true;
                        break; } }
                if (!found) {
                    PD_IMFI Pnew = new PD_IMFI(I, 1, 1, 0, null);
                    Pi_plus_1.add(Pnew); } } }
        // Calculate incremental min support
        for (PD_IMFI P : Pi_plus_1) {
            P.incr_Minsup = (int) (minSup * P.incr_Sup); }

        Pi_plus_1.sort(Comparator.comparingInt(x -> x.incr_Sup));
        Collections.reverse(Pi_plus_1);
        return Pi_plus_1;
    }

    public static FPTree findMFIsFromPDIMFIs(List<PD_IMFI> PD_IMFIs, List<List<String>> Di_plus_1) {
        FPTree tree = new FPTree(new ArrayList<>(), 0);  

        // Add transactions to the FP-Tree
        for (PD_IMFI P : PD_IMFIs) {
            if (P.list_IMFIs != null && (P.cur_Sup >= P.incr_Minsup || P.incr_Sup >= P.incr_Minsup)) {
                for (List<String> IMFIs : P.list_IMFIs) {
                    tree.addTransaction(new ArrayList<>(IMFIs));
                }
                P.list_IMFIs = null;
            }
        }

        // Add transactions from Di_plus_1
        for (List<String> T : Di_plus_1) {
            List<String> arrayTrns = new ArrayList<>();
            for (String item : T) {
                for (PD_IMFI P : PD_IMFIs) {
                    if (item.equals(P.item) && (P.cur_Sup >= P.incr_Minsup || P.incr_Sup >= P.incr_Minsup)) {
                        arrayTrns.add(item);
                        break;
                    }
                }
            }
            arrayTrns.sort(Comparator.comparingInt(x -> {
                int index = PD_IMFIs.indexOf(new PD_IMFI(x, 0, 0, 0, null));
                return index != -1 ? index : Integer.MAX_VALUE;
            }));
            tree.addTransaction(arrayTrns);
        }

        return tree;
    }

    public static FPTree FP_Max_Algorithm(FPTree tree) {
        int minSupport = 1;
        FPTree mfiTree = new FPTree(new ArrayList<>(), minSupport);

        Set<String> mfisNew = tree.findMFIsSet(minSupport);

        for (String mfi : mfisNew) {
            mfiTree.addTransaction(Collections.singletonList(mfi));
        }

        return mfiTree;
    }

    public static double confidence(List<String> antecedent, List<String> consequent, List<List<String>> Di_plus_1) {
        int supportConsequent = support(consequent, Di_plus_1);
        int supportAntecedentConsequent = support(concatenateLists(antecedent, consequent), Di_plus_1);

        return (double) supportAntecedentConsequent / supportConsequent;
    }

    public static List<String> concatenateLists(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }

    public static int support(List<String> itemset, List<List<String>> Di_plus_1) {
        int count = 0;
        if (itemset != null) {
            for (List<String> transaction : Di_plus_1) {
                if (transaction != null) {
                    boolean containsAllConsequent = transaction.containsAll(itemset);

                    // Check if at least one item from the antecedent is present in the transaction
                    boolean containsAtLeastOneAntecedent = itemset.stream().anyMatch(transaction::contains);

                    if (containsAllConsequent && containsAtLeastOneAntecedent) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static List<Map<String, Object>> generateAssociationRules(Set<String> MFIs, double minConf, List<List<String>> Di_plus_1) {
        List<Map<String, Object>> associationRules = new ArrayList<>();
    
        for (String mfi : MFIs) {
            Set<String> mfiSet = new HashSet<>(Arrays.asList(mfi.split(", ")));
            if (mfiSet.size() > 1) {
                List<String> mfiList = new ArrayList<>(mfiSet);
    
                for (int i = 1; i < mfiList.size(); i++) {
                    List<String> antecedent = mfiList.subList(0, i);
                    List<String> consequent = mfiList.subList(i, mfiList.size());
    
                    int supportAntecedentConsequent = support(concatenateLists(antecedent, consequent), Di_plus_1);
                    int supportConsequent = support(consequent, Di_plus_1);
    
                    double supportXUY = (double) supportAntecedentConsequent / Di_plus_1.size();
                    double supportX = (double) support(antecedent, Di_plus_1) / Di_plus_1.size();
                    double confidence = supportXUY / supportX;
    
                    if (confidence >= minConf) {
                        Map<String, Object> associationRule = new HashMap<>();
                        associationRule.put("antecedent", antecedent);
                        associationRule.put("consequent", consequent);
                        associationRule.put("confidence", confidence);
                        associationRules.add(associationRule);
                    }
                }
            }
        }
    
        return associationRules;
    }


    public static void printAssociationRules(List<Map<String, Object>> associationRules) {
        System.out.println("\nAssociation Rules:");
        for (Map<String, Object> rule : associationRules) {
            List<String> antecedent = (List<String>) rule.get("antecedent");
            List<String> consequent = (List<String>) rule.get("consequent");
            double confidence = (double) rule.get("confidence");
    
            System.out.println("Antecedent: " + antecedent + " => Consequent: " + consequent + " (Confidence: " + confidence + ")");
        }
    }
    public static void myCode() {
        String filePath = "transactional_T10I4D100K.csv";
        List<List<String>> Di_plus_1 = readDataFromCSV(filePath);
    
        // Print Di_plus_1
        System.out.println("Di_plus_1 at the beginning:");
        for (List<String> transaction : Di_plus_1) {
            System.out.println(transaction);
        }
    
        List<PD_IMFI> Pi = createPDIMFIAlgorithm(Di_plus_1, new ArrayList<>(), 0.2);
        Tuple<List<PD_IMFI>, FPTree, FPTree> result = mainIIMFIAlgorithm(Di_plus_1, Pi, false, 0.0, 0.1, 0.9);
    
        List<PD_IMFI> PD_IMFIs = result.item1;
        FPTree tree = result.item2;  // Retrieve the FP-Tree from the result
        FPTree mfiTree = result.item3;  // Retrieve the mfiTree from the result
    
        // Print FP-Tree
        System.out.println("\nFP-Tree:");
        tree.printTree();
    
        // Additional processing with PD_IMFIs if needed
    
        System.out.println("Printing PD_IMFIs:");
        for (PD_IMFI pd_imfi : PD_IMFIs) {
            System.out.println("Item: " + pd_imfi.item + ", Cur_Sup: " + pd_imfi.cur_Sup +
                    ", Incr_Sup: " + pd_imfi.incr_Sup + ", Incr_Minsup: " + pd_imfi.incr_Minsup);
        }
    
        // Additional prints
        int transactionsCount = Di_plus_1.size();
        double maxMemoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0);
        int maximalFrequentItemsetCount = mfiTree.findMFIsSet((int) (0.1 * Di_plus_1.size())).size();
        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime);
    
        System.out.println("\nTransactions count from database: " + transactionsCount);
        System.out.println("Max memory usage: " + maxMemoryUsage + " mb");
        System.out.println("Maximal frequent itemset count: " + maximalFrequentItemsetCount);
        System.out.println("Total time ~ " + totalTime + " ms");
    }

    public static Tuple<List<PD_IMFI>, FPTree, FPTree> mainIIMFIAlgorithm(List<List<String>> Di, List<PD_IMFI> Pi, boolean useNM, double minNovelty, double minSup, double minConf) {
        startTime = System.currentTimeMillis();  // Start time capture
        List<PD_IMFI> PD_IMFIs = createPDIMFIAlgorithm(Di, Pi, minSup);
        FPTree tree = findMFIsFromPDIMFIs(PD_IMFIs, Di);
        FPTree mfiTree = FP_Max_Algorithm(tree);
        mfiTree.minSupport = (int) (minSup * Di.size());
    
        // Tính toán độ mới cho các itemset và lọc theo minNovelty
        calculateNoveltyForPD_IMFIs(PD_IMFIs, Di, minNovelty);
    
        Set<String> mfiSet = mfiTree.findMFIsSet((int) (minSup * Di.size()));
    
        List<Map<String, Object>> associationRules = generateAssociationRules(
                mfiSet,
                minConf, Di);
    
        printAssociationRules(associationRules);
    
        return new Tuple<>(PD_IMFIs.stream().filter(item -> item.item != null).collect(Collectors.toList()), tree, mfiTree);
    }

    private static void calculateNoveltyForPD_IMFIs(List<PD_IMFI> PD_IMFIs, List<List<String>> Di_plus_1, double minNovelty) {
        for (PD_IMFI pd_imfi : PD_IMFIs) {
            if (pd_imfi.list_IMFIs != null && (pd_imfi.cur_Sup >= pd_imfi.incr_Minsup || pd_imfi.incr_Sup >= pd_imfi.incr_Minsup)) {
                for (List<String> IMFIs : pd_imfi.list_IMFIs) {
                    double novelty = calculateNovelty(IMFIs, Di_plus_1);
                    if (novelty >= minNovelty) {
                        System.out.println("Novelty for itemset " + IMFIs + ": " + novelty);
                    }
                }
                pd_imfi.list_IMFIs = null;
            }
        }
    }

    public static void main(String[] args) {
        myCode();
    }
}
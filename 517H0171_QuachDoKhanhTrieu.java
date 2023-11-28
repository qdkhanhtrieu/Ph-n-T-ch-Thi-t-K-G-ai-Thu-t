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

class CreatedNode {
    String item;
    int support;
    int novelty;
    int confidence;
    CreatedNode parent;
    Map<String, CreatedNode> children;

    CreatedNode(String item, int support, int novelty, int confidence, CreatedNode parent) {
        this.item = item;
        this.support = support;
        this.novelty = novelty;
        this.confidence = confidence;
        this.parent = parent;
        this.children = new HashMap<>();
    }
}

class FPTree {
    FPNode root;

    FPTree() {
        this.root = new FPNode(null, 0);
    }

    void addTransaction(List<String> transaction) {
        FPNode currentNode = this.root;
        for (String item : transaction) {
            if (item != null) {
                if (currentNode.children.containsKey(item)) {
                    currentNode.children.get(item).count++;
                    currentNode = currentNode.children.get(item);
                } else {
                    FPNode newNode = new FPNode(item, 1);
                    currentNode.children.put(item, newNode);
                    currentNode = newNode;
                }
            }
        }
    }

    void inTree(FPNode node, String indent) {
        if (node != null) {
            if (node.item != null) {
                System.out.println(indent + "├── " + node.item + " [count: " + node.count + "]");
            }
            for (Map.Entry<String, FPNode> entry : node.children.entrySet()) {
                String childItem = entry.getKey();
                FPNode childNode = entry.getValue();
                String childIndent = indent + (childItem != null ? "│   " : "    ");
                inTree(childNode, childIndent);
            }
        }
    }

    List<Set<String>> findMFIs(int minSupport) {
        List<Set<String>> mfis = new ArrayList<>();
        _findMFIs(this.root, new ArrayList<>(), minSupport, mfis);
        mfis.removeIf(mfi -> mfi.contains(null));
        return mfis;
    }

    private void _findMFIs(FPNode node, List<String> currentPath, int minSupport, List<Set<String>> mfis) {
        if (node != null) {
            if (node.count >= minSupport) {
                currentPath.removeIf(item -> item == null);
                mfis.add(new HashSet<>(currentPath));
            }
            for (Map.Entry<String, FPNode> entry : node.children.entrySet()) {
                String childItem = entry.getKey();
                FPNode childNode = entry.getValue();
                _findMFIs(childNode, new ArrayList<>(currentPath) {{
                    add(node.item);
                }}, minSupport, mfis);
            }
        }
    }
}

class FPNode {
    String item;
    int count;
    Map<String, FPNode> children;

    FPNode(String item, int count) {
        this.item = item;
        this.count = count;
        this.children = new HashMap<>();
    }
}

public class Main {
    public static List<List<String>> readDataFromCSV(String filePath) {
        List<List<String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                data.add(Arrays.asList(line.split("\t")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static List<PD_IMFI> createdPDIMFIAlgorithm(List<List<String>> Di_plus_1, List<PD_IMFI> Pi) {
        Set<String> uniqueItems = new HashSet<>();
        for (List<String> transaction : Di_plus_1) {
            uniqueItems.addAll(transaction);
        }

        List<PD_IMFI> Pi_plus_1 = new ArrayList<>();
        for (String item : uniqueItems) {
            Pi_plus_1.add(new PD_IMFI(item, 0, 0, 0, null));
        }

        for (List<String> T : Di_plus_1) {
            for (String I : T) {
                boolean found = false;
                for (PD_IMFI P : Pi_plus_1) {
                    if (I.equals(P.item)) {
                        P.cur_Sup++;
                        P.incr_Sup++;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    PD_IMFI Pnew = new PD_IMFI(I, 1, 1, 0, null);
                    Pi_plus_1.add(Pnew);
                }
            }
        }

        Pi_plus_1.sort(Comparator.comparingInt(x -> x.incr_Sup));
        Collections.reverse(Pi_plus_1);

        return Pi_plus_1;
    }

    public static void FP_Growth(FPTree tree, List<String> itemset, int support) {
        if (itemset.isEmpty()) {
            return;
        }

        FPNode currentNode = tree.root;

        for (String item : itemset) {
            if (currentNode.children.containsKey(item)) {
                currentNode = currentNode.children.get(item);
            } else {
                FPNode newNode = new FPNode(item, support);
                currentNode.children.put(item, newNode);
                currentNode = newNode;
            }
        }
    }

    public static FPTree createdTreeAlgorithm(List<PD_IMFI> Pi_plus_1, List<List<String>> Di_plus_1, int cur_Minsup) {
        FPTree tree = new FPTree();

        for (PD_IMFI P : Pi_plus_1) {
            if (P.list_IMFIs != null && (P.cur_Sup >= cur_Minsup || P.incr_Sup >= P.incr_Minsup)) {
                for (List<String> IMFIs : P.list_IMFIs) {
                    List<String> arrayTrns = new ArrayList<>();
                    for (List<String> itemset : IMFIs) {
                        arrayTrns.addAll(itemset);
                    }
                    arrayTrns.sort(Comparator.comparingInt(x -> Pi_plus_1.indexOf(x) != -1 ? Pi_plus_1.indexOf(x) : Integer.MAX_VALUE));
                    tree.addTransaction(arrayTrns);
                }
                P.list_IMFIs = null;
            }
        }

        for (List<String> T : Di_plus_1) {
            List<String> arrayTrns = new ArrayList<>();
            for (String item : T) {
                for (PD_IMFI P : Pi_plus_1) {
                    if (item.equals(P.item) && (P.cur_Sup >= cur_Minsup || P.incr_Sup >= P.incr_Minsup)) {
                        arrayTrns.add(item);
                        break;
                    }
                }
            }
            arrayTrns.sort(Comparator.comparingInt(x -> Pi_plus_1.indexOf(x) != -1 ? Pi_plus_1.indexOf(x) : Integer.MAX_VALUE));
            tree.addTransaction(arrayTrns);
        }

        return tree;
    }

    public static FPTree FP_Max_Algorithm(FPTree tree) {
        int minSupport = 1;
        FPTree mfiTree = new FPTree();
        mfiTree.root.children = tree.findMFIs(minSupport);
        return mfiTree;
    }

    public static double interestingMeasureAlgorithm(List<PD_IMFI> Pi_plus_1, FPTree mfiTree) {
        double minNM = 1;

        for (PD_IMFI P : Pi_plus_1) {
            if (P.list_IMFIs != null) {
                for (List<String> IMFIs : P.list_IMFIs) {
                    int S1 = IMFIs.size();
                    int S2 = mfiTree.findMFIs(S1).size();
                    int k = new HashSet<>(IMFIs).retainAll(mfiTree.findMFIs(S1).get(0)) ? 1 : 0;
                    double NMtemp = (S1 + S2 - 2 * k) / (double) (S1 + S2);
                    if (NMtemp < minNM) {
                        minNM = NMtemp;
                    }
                }
            }
        }

        return minNM;
    }

    public static int support(List<String> itemset, List<List<String>> Di_plus_1) {
        int count = 0;
        for (List<String> transaction : Di_plus_1) {
            if (transaction.containsAll(itemset)) {
                count++;
            }
        }
        return count;
    }

    public static List<Map<String, Object>> generateAssociationRules(List<Set<String>> MFIs, double minConf, List<List<String>> Di_plus_1) {
        List<Map<String, Object>> associationRules = new ArrayList<>();

        for (Set<String> mfi : MFIs) {
            if (mfi.size() > 1) {
                List<String> mfiList = new ArrayList<>(mfi);

                for (int i = 1; i < mfiList.size(); i++) {
                    List<String> antecedent = mfiList.subList(0, i);
                    List<String> consequent = mfiList.subList(i, mfiList.size());

                    double confidence = (double) support(mfiList, Di_plus_1) / support(antecedent, Di_plus_1);

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

    public static Tuple<List<PD_IMFI>, FPTree, FPTree> mainIIMFIAlgorithm(List<List<String>> Di, List<PD_IMFI> Pi, boolean useNM, double minNovelty, double minSup, double minConf) {
        List<PD_IMFI> Pi_plus_1 = createdPDIMFIAlgorithm(Di, Pi);
        int cur_Minsup = (int) (minSup * Di.size());
        FPTree tree = createdTreeAlgorithm(Pi_plus_1, Di, cur_Minsup);
        System.out.println("FPTree:");
        tree.inTree(tree.root, "");
        FPTree mfiTree = FP_Max_Algorithm(tree);
        double min_novelty = interestingMeasureAlgorithm(Pi_plus_1, mfiTree);

        if (min_novelty >= minNovelty) {
            for (PD_IMFI P : Pi_plus_1) {
                if (P.item == null) {
                    P.list_IMFIs = Collections.singletonList((List<String>) mfiTree.findMFIs(Di.size()).get(0));
                    break;
                }
            }
        }

        System.out.println("\nMFIs:");
        for (Set<String> mfi : mfiTree.findMFIs(cur_Minsup)) {
            System.out.println(mfi);
        }

        List<Map<String, Object>> associationRules = generateAssociationRules(mfiTree.findMFIs(cur_Minsup), minConf, Di);

        System.out.println("\nAssociation Rules:");
        for (Map<String, Object> rule : associationRules) {
            System.out.println("Antecedent: " + rule.get("antecedent") + ", Consequent: " + rule.get("consequent") + ", Confidence: " + rule.get("confidence"));
        }

        return new Tuple<>(Pi_plus_1.stream().filter(item -> item.item != null).collect(Collectors.toList()), tree, mfiTree);
    }

    public static void myCode() {
        String filePath = "transactional_T10I4D100K.csv";
        List<List<String>> Di_plus_1 = readDataFromCSV(filePath);
        List<PD_IMFI> Pi = createdPDIMFIAlgorithm(Di_plus_1, new ArrayList<>());
        Tuple<List<PD_IMFI>, FPTree, FPTree> result = mainIIMFIAlgorithm(Di_plus_1, Pi, true, 0.5, 0.2, 0.5);
        for (PD_IMFI item : result.item1) {
            System.out.println("Item " + item.item + ": cur_Sup=" + item.cur_Sup + ", incr_Sup=" + item.incr_Sup + ", incr_Minsup=" + item.incr_Minsup + ", list_IMFIs=" + item.list_IMFIs);
        }
    }

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        myCode();
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1e9;
        System.out.println("Running time: " + executionTime + " seconds");
    }
}

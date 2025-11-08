import java.util.*;

public class BankAlgorithmsMain {
    static class Interval {
        int start, end;
        Interval(int s, int e) { this.start = s; this.end = e; }
        public String toString() { return "(" + start + "," + end + ")"; }
    }
    public static int maxNonOverlappingAlerts(List<Interval> alerts) {
        alerts.sort(Comparator.comparingInt(a -> a.end));
        int count = 0, lastEnd = -1;
        for (Interval alert : alerts) {
            if (alert.start >= lastEnd) {
                count++; lastEnd = alert.end;
            }
        }
        return count;
    }

    static class Transaction implements Comparable<Transaction> {
        int timestamp, priority, branchId, txnId;
        String priorityLabel;
        Transaction(int t, String p, int b, int id) {
            timestamp = t;
            priorityLabel = p;
            priority = switch (p) { case "High" -> 3; case "Medium" -> 2; default -> 1; };
            branchId = b; txnId = id;
        }
        public int compareTo(Transaction o) {
            if (timestamp != o.timestamp) return Integer.compare(timestamp, o.timestamp);
            if (priority != o.priority) return -Integer.compare(priority, o.priority);
            if (branchId != o.branchId) return Integer.compare(branchId, o.branchId);
            return Integer.compare(txnId, o.txnId);
        }
        public String toString() { return String.format("(%d, %d, %d, %d)", timestamp, priority, branchId, txnId); }
    }
    static class MergeNode {
        List<Transaction> data; MergeNode left, right;
        MergeNode(List<Transaction> data) { this.data = data; }
    }
    public static MergeNode mergeK(List<List<Transaction>> logs) {
        return mergeKHelper(logs, 0, logs.size()-1);
    }
    private static MergeNode mergeKHelper(List<List<Transaction>> logs, int left, int right) {
        if (left == right) return new MergeNode(logs.get(left));
        int mid = (left + right) / 2;
        MergeNode l = mergeKHelper(logs, left, mid);
        MergeNode r = mergeKHelper(logs, mid+1, right);
        return new MergeNode(mergeTwo(l.data, r.data));
    }
    private static List<Transaction> mergeTwo(List<Transaction> A, List<Transaction> B) {
        List<Transaction> out = new ArrayList<>(A.size() + B.size());
        int i=0, j=0;
        while(i<A.size() && j<B.size()) {
            if(A.get(i).compareTo(B.get(j)) <= 0)
                out.add(A.get(i++));
            else
                out.add(B.get(j++));
        }
        while(i<A.size()) out.add(A.get(i++));
        while(j<B.size()) out.add(B.get(j++));
        return out;
    }
    static boolean validate(List<Transaction> merged) {
        for (int i=1; i<merged.size(); ++i)
            if (merged.get(i-1).compareTo(merged.get(i)) > 0)
                return false;
        return true;
    }

    public static void main(String[] args) {
        Random rand = new Random(777);
        int k = 8, txnsPerBranch = 2000;
        String[] priorities = {"Low", "Medium", "High"};

        List<List<Transaction>> logs = new ArrayList<>();
        for (int b=0; b<k; b++) {
            List<Transaction> branchLog = new ArrayList<>();
            int t = 0;
            for (int i=0; i<txnsPerBranch; i++) {
                t += rand.nextInt(5);
                String p = priorities[rand.nextInt(3)];
                branchLog.add(new Transaction(t, p, b, i));
            }
            branchLog.sort(Comparator.naturalOrder());
            logs.add(branchLog);
        }
        long start = System.currentTimeMillis();
        MergeNode root = mergeK(logs);
        long end = System.currentTimeMillis();
        System.out.println("=== Simulated Test: Transaction Merge (Divide & Conquer) ===");
        System.out.printf("%-38s %d\n", "Merged transaction count:", root.data.size());
        System.out.printf("%-38s %d\n", "Runtime (ms):", (end-start));
        System.out.println("Validation: " + (validate(root.data) ? "PASS" : "FAIL"));

        List<List<Transaction>> logs2 = new ArrayList<>();
        logs2.add(Arrays.asList(
            new Transaction(10, "High", 0, 1),
            new Transaction(10, "Medium", 0, 2)
        ));
        logs2.add(Arrays.asList(
            new Transaction(10, "High", 1, 1),
            new Transaction(10, "Low", 1, 2)
        ));
        logs2.forEach(l -> l.sort(Comparator.naturalOrder()));
        MergeNode advMerge = mergeK(logs2);
        System.out.println("=== Edge Case: Duplicate Timestamps, Priorities ===");
        System.out.println("Merged Output:");
        for (Transaction t : advMerge.data) System.out.println(t);
        System.out.println("Validation: " + (validate(advMerge.data) ? "PASS" : "FAIL"));

        List<List<Transaction>> logs3 = new ArrayList<>();
        logs3.add(Arrays.asList());
        logs3.add(Arrays.asList());
        logs3.add(logs2.get(0));
        MergeNode empMerge = mergeK(logs3);
        System.out.println("=== Edge Case: Empty Branch Logs ===");
        System.out.println("Merged Output:");
        for (Transaction t : empMerge.data) System.out.println(t);
        System.out.println("Validation: " + (validate(empMerge.data) ? "PASS" : "FAIL"));

        int bigN = 16000; List<List<Transaction>> lsts = new ArrayList<>();
        for (int b = 0; b < 8; b++) {
            List<Transaction> L = new ArrayList<>();
            int t = rand.nextInt(10);
            for (int j = 0; j < bigN; j++) {
                L.add(new Transaction(t + j, "High", b, j));
            }
            L.sort(Comparator.naturalOrder());
            lsts.add(L);
        }
        long startStress = System.currentTimeMillis();
        MergeNode bigTest = mergeK(lsts);
        long endStress = System.currentTimeMillis();
        System.out.println("=== Stress Test: Same Priority, Large N ===");
        System.out.printf("%-38s %d\n", "Merged transaction count:", bigTest.data.size());
        System.out.printf("%-38s %d\n", "Runtime (ms):", (endStress-startStress));
        System.out.println("Validation: " + (validate(bigTest.data) ? "PASS" : "FAIL"));

        System.out.println("\n50E Example Test: Transaction Merge");
        List<Transaction> branch0 = Arrays.asList(
            new Transaction(1, "High", 0, 1),
            new Transaction(5, "Low", 0, 2)
        );
        List<Transaction> branch1 = Arrays.asList(
            new Transaction(3, "Medium", 1, 1),
            new Transaction(5, "High", 1, 2)
        );
        branch0.sort(Comparator.naturalOrder());
        branch1.sort(Comparator.naturalOrder());
        System.out.println("Branch 0: " + branch0);
        System.out.println("Branch 1: " + branch1);
        MergeNode exampleMerge = mergeK(List.of(branch0, branch1));
        System.out.println("Merged Output:");
        for (Transaction t : exampleMerge.data) System.out.println(t);
    }
}


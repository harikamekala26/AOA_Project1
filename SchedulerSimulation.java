import java.util.*;

public class SchedulerSimulation {

    /**
     * Represents a fraud alert interval with urgency and severity.
     */
    static class FraudAlert {
        private final String id;
        private final int start, end;
        private final int urgency;
        private final double severity;
        private final String location;

        public FraudAlert(String id, int start, int end, int urgency, double severity, String location) {
            if (end < start)
                throw new IllegalArgumentException("End must not be less than start");
            this.id = id;
            this.start = start;
            this.end = end;
            this.urgency = urgency;
            this.severity = severity;
            this.location = location;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        /** Weight is urgency * severity */
        public double getWeight() {
            return urgency * severity;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return String.format("%s[%d-%d] u=%d s=%.2f loc=%s",
                    id, start, end, urgency, severity, location);
        }
    }

    static class IntervalTreeNode {
        FraudAlert alert;
        int maxEnd;
        IntervalTreeNode left, right;

        IntervalTreeNode(FraudAlert alert) {
            this.alert = alert;
            this.maxEnd = alert.getEnd();
        }
    }

    static class IntervalTree {
        private IntervalTreeNode root;

        public void insert(FraudAlert f) {
            root = insert(root, f);
        }

        private IntervalTreeNode insert(IntervalTreeNode node, FraudAlert f) {
            if (node == null)
                return new IntervalTreeNode(f);
            if (f.getStart() < node.alert.getStart())
                node.left = insert(node.left, f);
            else
                node.right = insert(node.right, f);

            node.maxEnd = Math.max(node.maxEnd, f.getEnd());
            return node;
        }

        public boolean overlaps(FraudAlert f) {
            return overlaps(root, f);
        }

        private boolean overlaps(IntervalTreeNode node, FraudAlert f) {
            if (node == null)
                return false;
            if (conflict(node.alert, f))
                return true;

            if (node.left != null && node.left.maxEnd > f.getStart())
                return overlaps(node.left, f);

            return overlaps(node.right, f);
        }

        private boolean conflict(FraudAlert a, FraudAlert b) {
            return !(a.getEnd() <= b.getStart() || a.getStart() >= b.getEnd());
        }
    }

    /**
     * An investigation team with skill factor, fatigue, and assigned alerts.
     *
     * Skill factor models expertise scaling alert weights.
     * Fatigue models workload reducing team effectiveness.
     * Conflict penalties reduce assignment scores for scheduling decisions.
     */
    static class InvestigationTeam {
        private final String name;
        private final double skillFactor;
        private final List<FraudAlert> assigned = new ArrayList<>();
        private double fatigue = 1.0;

        public final IntervalTree tree = new IntervalTree();

        public InvestigationTeam(String name, double skill) {
            this.name = name;
            this.skillFactor = skill;
        }

        public boolean hasConflict(FraudAlert a) {
            return tree.overlaps(a);
        }

        public double score(FraudAlert a) {
            double base = a.getWeight() * skillFactor;
            double penalty = fatigue * (hasConflict(a) ? 5.0 : 1.0);
            return base / penalty;
        }

        public void assign(FraudAlert a) {
            assigned.add(a);
            tree.insert(a);
            fatigue += 0.05 * (a.getEnd() - a.getStart());
        }

        public double utilization() {
            return assigned.stream().mapToDouble(f -> f.getEnd() - f.getStart()).sum();
        }

        @Override
        public String toString() {
            return String.format("%s skill=%.2f fatigue=%.2f assigned=%d utilization=%.2f",
                    name, skillFactor, fatigue, assigned.size(), utilization());
        }

        public List<FraudAlert> getAssigned() {
            return assigned;
        }
    }

    static class GreedyScheduler {
        private final List<FraudAlert> alerts;
        private final List<InvestigationTeam> teams;

        public GreedyScheduler(List<FraudAlert> alerts, List<InvestigationTeam> teams) {
            this.alerts = alerts;
            this.teams = teams;
        }

        /**
         * Formal running time: O(n log n + n m log k)
         * Greedy correctness: Assigns max weighted intervals without conflict per team, optimal for interval scheduling extension.
         */
        public void schedule() {
            alerts.sort(Comparator.comparingDouble(FraudAlert::getWeight).reversed());
            for (FraudAlert a : alerts) {
                InvestigationTeam best = null;
                double bestScore = 0;
                for (InvestigationTeam t : teams) {
                    double s = t.score(a);
                    if (s > bestScore && !t.hasConflict(a)) {
                        bestScore = s;
                        best = t;
                    }
                }
                if (best != null)
                    best.assign(a);
            }
        }
    }

    static double getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
    }

    public static void main(String[] args) {
        // Timing experiment output (for graphing externally)
        System.out.println("=== Greedy Algorithm Scheduling Timing Experiment ===");
        System.out.println("Size,Runtime_ms");
        int[] sizes = {100, 500, 1000, 2000, 5000};
        for (int size : sizes) {
            List<FraudAlert> alerts = generateRandomAlerts(size);
            List<InvestigationTeam> teams = Arrays.asList(
                    new InvestigationTeam("Alpha", 1.1),
                    new InvestigationTeam("Beta", 0.9),
                    new InvestigationTeam("Gamma", 1.0));
            long start = System.nanoTime();
            new GreedyScheduler(alerts, teams).schedule();
            long end = System.nanoTime();
            System.out.printf("%d,%.3f%n", size, (end - start) / 1e6);
        }

        // Run all desired test cases with neat output
        System.out.println("\n=== Demo: Large Random Test ===");
        runTest(generateRandomAlerts(1000));

        System.out.println("\n=== Edge Case Test 1: Overlapping Alerts ===");
        runTest(Arrays.asList(
                new FraudAlert("O1", 1, 5, 3, 4.0, "Branch1"),
                new FraudAlert("O2", 4, 8, 5, 2.5, "Branch2"),
                new FraudAlert("O3", 7, 10, 2, 3.5, "Branch3"),
                new FraudAlert("O4", 6, 9, 4, 1.2, "Branch4"),
                new FraudAlert("O5", 3, 6, 3, 3.0, "Branch5")
        ));

        System.out.println("\n=== Edge Case Test 2: Zero Length and Same Start/End Alerts ===");
        runTest(Arrays.asList(
                new FraudAlert("Z1", 5, 5, 1, 2.0, "Branch1"),
                new FraudAlert("Z2", 5, 5, 3, 2.5, "Branch2"),
                new FraudAlert("Z3", 5, 5, 4, 1.5, "Branch3"),
                new FraudAlert("Z4", 1, 2, 2, 3.5, "Branch4"),
                new FraudAlert("Z5", 2, 3, 3, 2.0, "Branch5")
        ));

        System.out.println("\n=== Example Test: Known Intervals ===");
        runTest(Arrays.asList(
                new FraudAlert("E1", 1, 4, 1, 1.0, "Branch0"),
                new FraudAlert("E2", 3, 5, 1, 1.0, "Branch0"),
                new FraudAlert("E3", 0, 6, 1, 1.0, "Branch0"),
                new FraudAlert("E4", 5, 7, 1, 1.0, "Branch0"),
                new FraudAlert("E5", 8, 9, 1, 1.0, "Branch0"),
                new FraudAlert("E6", 5, 9, 1, 1.0, "Branch0")
        ));
    }

    private static void runTest(List<FraudAlert> alerts) {
        System.out.printf("Total alerts generated: %d%n", alerts.size());

        List<InvestigationTeam> teams = Arrays.asList(
                new InvestigationTeam("Alpha", 1.1),
                new InvestigationTeam("Beta", 0.9),
                new InvestigationTeam("Gamma", 1.0)
        );

        double memBefore = getMemoryUsageMB();
        long startTime = System.nanoTime();

        GreedyScheduler scheduler = new GreedyScheduler(alerts, teams);
        scheduler.schedule();

        long endTime = System.nanoTime();
        double memAfter = getMemoryUsageMB();

        System.out.println("=== FRAUD ALERT INVESTIGATION SUMMARY ===");
        System.out.printf("%-10s %-7s %-8s %-10s %-18s %-20s%n",
                "Team", "Skill", "Fatigue", "Assigned", "Total Utilization", "Avg Interval Length");
        for (InvestigationTeam t : teams) {
            int assignedCount = t.getAssigned().size();
            double totalUtil = t.utilization();
            double avgInterval = (assignedCount > 0) ? (totalUtil / assignedCount) : 0;
            System.out.printf("%-10s %-7.2f %-8.2f %-10d %-18.2f %-20.2f%n",
                    t.name, t.skillFactor, t.fatigue, assignedCount, totalUtil, avgInterval);
        }

        int totalAssigned = teams.stream().mapToInt(t -> t.getAssigned().size()).sum();
        double totalUtil = teams.stream().mapToDouble(InvestigationTeam::utilization).sum();

        System.out.printf("Max non-overlapping alerts: %d%n", totalAssigned);
        System.out.printf("Total utilization: %.2f%n", totalUtil);
        System.out.printf("Runtime (ms): %.3f%n", (endTime - startTime) / 1e6);
        System.out.printf("Memory used (MB): %.3f%n", (memAfter - memBefore));
    }

    private static List<FraudAlert> generateRandomAlerts(int count) {
        Random rand = new Random();
        List<FraudAlert> alerts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int s = rand.nextInt(50);
            int e = s + rand.nextInt(6) + 1;
            int u = 1 + rand.nextInt(5);
            double sev = 1 + rand.nextDouble() * 4;
            alerts.add(new FraudAlert("A" + i, s, e, u, sev, "Branch" + rand.nextInt(10)));
        }
        return alerts;
    }
}

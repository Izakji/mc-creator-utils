package com.izakji.mccreatorutils.locate.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.biome.Climate;

/**
 * Spatial index of parameter-point boxes used to decide whether a sampled climate
 * point is covered by at least one parameter point.
 *
 * <p>This is the soundness gate for envelope pruning. A nearest-neighbour biome
 * lookup can return the target even when the sample lies outside the target box.
 * Pruning is only safe when a zero-offset non-target parameter point contains the
 * sample, because that point has fitness zero and therefore cannot lose to the
 * target. Non-zero offsets are ignored here so the gate stays conservative.</p>
 *
 * @author Izakji
 */
public final class CoverageIndex {
    private static final int LEAF_SIZE = 12;
    private static final int AXES = 6;

    private final Node root;

    private CoverageIndex(Node root) {
        this.root = root;
    }

    public static CoverageIndex fromPoints(List<Climate.ParameterPoint> points) {
        if (points.isEmpty()) {
            return new CoverageIndex(null);
        }
        List<Box> boxes = new ArrayList<>(points.size());
        for (Climate.ParameterPoint point : points) {
            boxes.add(new Box(point));
        }
        return new CoverageIndex(build(boxes));
    }

    public boolean covers(Climate.TargetPoint point) {
        return root != null
                && root.covers(new long[] {
                    point.temperature(),
                    point.humidity(),
                    point.continentalness(),
                    point.erosion(),
                    point.depth(),
                    point.weirdness()
                });
    }

    public boolean coversFive(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        return root != null && root.coversFive(new long[] {temperature, humidity, continentalness, erosion, weirdness});
    }

    private static Node build(List<Box> boxes) {
        if (boxes.size() <= LEAF_SIZE) {
            return new Node(boxes, null, null);
        }

        int splitAxis = 0;
        long bestSpread = -1L;
        for (int axis = 0; axis < AXES; axis++) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (Box box : boxes) {
                min = Math.min(min, box.min[axis]);
                max = Math.max(max, box.max[axis]);
            }
            long spread = max - min;
            if (spread > bestSpread) {
                bestSpread = spread;
                splitAxis = axis;
            }
        }

        int axis = splitAxis;
        boxes.sort(Comparator.comparingLong(box -> midpoint(box, axis)));
        int middle = boxes.size() / 2;
        List<Box> left = new ArrayList<>(boxes.subList(0, middle));
        List<Box> right = new ArrayList<>(boxes.subList(middle, boxes.size()));
        return new Node(null, build(left), build(right));
    }

    private static long midpoint(Box box, int axis) {
        return box.min[axis] + (box.max[axis] - box.min[axis]) / 2L;
    }

    private static final class Box {
        private final long[] min;
        private final long[] max;
        private final long offset;

        private Box(Climate.ParameterPoint point) {
            offset = point.offset();
            min = new long[] {
                point.temperature().min(),
                point.humidity().min(),
                point.continentalness().min(),
                point.erosion().min(),
                point.depth().min(),
                point.weirdness().min()
            };
            max = new long[] {
                point.temperature().max(),
                point.humidity().max(),
                point.continentalness().max(),
                point.erosion().max(),
                point.depth().max(),
                point.weirdness().max()
            };
        }
    }

    private static final class Node {
        private final long[] min;
        private final long[] max;
        private final List<Box> boxes;
        private final Node left;
        private final Node right;
        private final boolean hasZeroOffset;

        private Node(List<Box> boxes, Node left, Node right) {
            this.boxes = boxes;
            this.left = left;
            this.right = right;
            this.min = new long[AXES];
            this.max = new long[AXES];
            for (int axis = 0; axis < AXES; axis++) {
                min[axis] = Long.MAX_VALUE;
                max[axis] = Long.MIN_VALUE;
            }
            boolean zeroOffset = false;
            if (boxes != null) {
                for (Box box : boxes) {
                    zeroOffset |= box.offset == 0;
                    for (int axis = 0; axis < AXES; axis++) {
                        min[axis] = Math.min(min[axis], box.min[axis]);
                        max[axis] = Math.max(max[axis], box.max[axis]);
                    }
                }
            } else {
                for (Node child : List.of(left, right)) {
                    zeroOffset |= child.hasZeroOffset;
                    for (int axis = 0; axis < AXES; axis++) {
                        min[axis] = Math.min(min[axis], child.min[axis]);
                        max[axis] = Math.max(max[axis], child.max[axis]);
                    }
                }
            }
            this.hasZeroOffset = zeroOffset;
        }

        private boolean covers(long[] point) {
            if (!hasZeroOffset || !inside(min, max, point)) {
                return false;
            }
            if (boxes != null) {
                for (Box box : boxes) {
                    if (box.offset == 0 && inside(box.min, box.max, point)) {
                        return true;
                    }
                }
                return false;
            }
            return left.covers(point) || right.covers(point);
        }

        private boolean coversFive(long[] point) {
            if (!hasZeroOffset || !insideFive(min, max, point)) {
                return false;
            }
            if (boxes != null) {
                for (Box box : boxes) {
                    if (box.offset == 0 && insideFive(box.min, box.max, point)) {
                        return true;
                    }
                }
                return false;
            }
            return left.coversFive(point) || right.coversFive(point);
        }
    }

    private static boolean inside(long[] min, long[] max, long[] point) {
        for (int axis = 0; axis < AXES; axis++) {
            if (point[axis] < min[axis] || point[axis] > max[axis]) {
                return false;
            }
        }
        return true;
    }

    private static boolean insideFive(long[] min, long[] max, long[] point) {
        for (int axis = 0; axis < 5; axis++) {
            if (point[axis] < min[axis] || point[axis] > max[axis]) {
                return false;
            }
        }
        return true;
    }
}

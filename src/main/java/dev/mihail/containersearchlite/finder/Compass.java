package dev.mihail.containersearchlite.finder;

/**
 * Eight-way compass bearing for a horizontal offset, using Minecraft's axes: north is -Z, east is +X.
 */
public enum Compass {
    NORTH("n"), NORTH_EAST("ne"), EAST("e"), SOUTH_EAST("se"), SOUTH("s"), SOUTH_WEST("sw"), WEST("w"), NORTH_WEST("nw");

    private final String key;

    Compass(String key) {
        this.key = key;
    }

    /** Short lower-case key used to build a translation key such as {@code containersearchlite.compass.ne}. */
    public String key() {
        return key;
    }

    /**
     * @param dx offset along the X axis (positive is east)
     * @param dz offset along the Z axis (positive is south)
     */
    public static Compass fromOffset(double dx, double dz) {
        if (dx == 0 && dz == 0) {
            return NORTH;
        }
        // 0 degrees = north, increasing clockwise (east = 90).
        double bearing = Math.toDegrees(Math.atan2(dx, -dz));
        if (bearing < 0) {
            bearing += 360.0;
        }
        int sector = (int) Math.round(bearing / 45.0) % 8;
        return values()[sector];
    }
}

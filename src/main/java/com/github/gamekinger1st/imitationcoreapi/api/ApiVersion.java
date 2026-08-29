package com.github.gamekinger1st.imitationcoreapi.api;

public record ApiVersion(int major, int minor, int patch) implements Comparable<ApiVersion> {
    public static final ApiVersion CURRENT = new ApiVersion(0, 5, 0);

    public ApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("API version values cannot be negative");
        }
    }

    public boolean isBinaryCompatibleWith(ApiVersion other) {
        if (major == 0 || other.major == 0) {
            return major == other.major && minor == other.minor;
        }
        return major == other.major;
    }

    @Override
    public int compareTo(ApiVersion other) {
        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }
        int minorComparison = Integer.compare(minor, other.minor);
        return minorComparison != 0 ? minorComparison : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
